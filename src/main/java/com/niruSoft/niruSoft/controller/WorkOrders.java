package com.niruSoft.niruSoft.controller;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.niruSoft.niruSoft.model.PDFData;
import com.niruSoft.niruSoft.service.GenerateBillService;
import com.niruSoft.niruSoft.service.PDFGenerationService;
import com.niruSoft.niruSoft.utils.ExcelValidator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.niruSoft.niruSoft.utils.CommonUtils.formatDate;


@RestController
public class WorkOrders {

    private final GenerateBillService generateBillService;

    private final PDFGenerationService pdfGenerationService;

    @Autowired
    public WorkOrders(ExcelValidator excelValidator, GenerateBillService generateBillService, PDFGenerationService pdfGenerationService) {
        this.generateBillService = generateBillService;
        this.pdfGenerationService = pdfGenerationService;
    }

    @RequestMapping(value = "/zip", headers = "content-type=multipart/form-data", method = RequestMethod.POST, produces = "application/zip")
    public ResponseEntity<byte[]> generatePDFZip(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded".getBytes());
        }
        boolean isValid = ExcelValidator.validateExcel(file.getInputStream());
        if (!isValid) {
            return ResponseEntity.badRequest().body("Invalid Excel file".getBytes());
        }
        JSONObject excelData = generateBillService.processExcelData(file.getInputStream());

//        System.out.print(excelData);
        List<CompletableFuture<PDFData>> pdfFutures = new ArrayList<>();
        for (String farmerName : excelData.keySet()) {
            JSONObject farmerData = excelData.getJSONObject(farmerName);
            String jsonData = farmerData.toString();
            JSONArray dateArray = farmerData.getJSONArray("DATE");
            if (!dateArray.isEmpty()) {
                String date = formatDate(dateArray.getString(0));
                CompletableFuture<PDFData> pdfFuture = pdfGenerationService.generatePDFFromJSONAsync(jsonData, farmerName, date);
                pdfFutures.add(pdfFuture);
            }
        }

        try {
            CompletableFuture<Void> allOf = CompletableFuture.allOf(pdfFutures.toArray(new CompletableFuture[0]));
            allOf.get();
            List<PDFData> pdfDataList = pdfFutures.stream().map(future -> {
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Failed to retrieve PDF data", e);
                }
            }).collect(Collectors.toList());
            byte[] zipBytes = createZipFile(pdfDataList);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "Farmer_Data.zip");
            headers.setContentLength(zipBytes.length);
            return ResponseEntity.ok().headers(headers).body(zipBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating ZIP file".getBytes());
        }
    }

    public static byte[] createZipFile(List<PDFData> pdfDataList) throws IOException {
        ByteArrayOutputStream zipOutputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(zipOutputStream)) {
            for (PDFData pdfData : pdfDataList) {
                String pdfFileName = pdfData.fileName();
                byte[] pdfBytes = pdfData.pdfBytes();

                ZipEntry zipEntry = new ZipEntry(pdfFileName);
                zip.putNextEntry(zipEntry);
                zip.write(pdfBytes);
                zip.closeEntry();
            }
        }

        return zipOutputStream.toByteArray();
    }


    @RequestMapping(value = "/upload", headers = "content-type=multipart/form-data", method = RequestMethod.POST,produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> uploadExcelFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded".getBytes());
        }
        boolean isValid = ExcelValidator.isHeaderValid(file.getInputStream());
        if (!isValid) {
            return ResponseEntity.badRequest().body("Invalid Excel file".getBytes());
        }
        JSONObject excelData = generateBillService.processExcelFile(file.getInputStream());
//        System.out.print(excelData);

        List<CompletableFuture<PDFData>> pdfFutures = new ArrayList<>();
        for (String farmerName : excelData.keySet()) {
            JSONObject farmerDataObject = excelData.getJSONObject(farmerName);
            if (farmerDataObject.has("DATE")) {
                JSONArray dateArray = farmerDataObject.getJSONArray("DATE");
                if (!dateArray.isEmpty()) {
                    String date = dateArray.getString(0);
                    String jsonData = farmerDataObject.toString();
                    CompletableFuture<PDFData> pdfFuture = pdfGenerationService.generatePDFFromJSONAsyncSecond(jsonData, farmerName, date);
                    pdfFutures.add(pdfFuture);
                }
            }
        }
        try {
            CompletableFuture<Void> allOf = CompletableFuture.allOf(pdfFutures.toArray(new CompletableFuture[0]));
            allOf.get();
            List<PDFData> pdfDataList = pdfFutures.stream().map(future -> {
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Failed to retrieve PDF data", e);
                }
            }).collect(Collectors.toList());
            byte[] combinedPdfBytes = createCombinedPdf(pdfDataList);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Combined_Farmer_Data.pdf");
            headers.setContentLength(combinedPdfBytes.length);
            return ResponseEntity.ok().headers(headers).body(combinedPdfBytes);
        } catch (IOException | InterruptedException | ExecutionException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    public static byte[] createCombinedPdf(List<PDFData> pdfDataList) throws IOException {
        ByteArrayOutputStream mergedPdfOutputStream = new ByteArrayOutputStream();

        try (PdfDocument mergedPdfDocument = new PdfDocument(new PdfWriter(mergedPdfOutputStream))) {
            for (PDFData pdfData : pdfDataList) {
                byte[] pdfBytes = pdfData.pdfBytes();
                PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)));

                for (int pageNum = 1; pageNum <= pdfDoc.getNumberOfPages(); pageNum++) {
                    PdfPage page = pdfDoc.getPage(pageNum);
                    mergedPdfDocument.addPage(page.copyTo(mergedPdfDocument));
                }
            }
        }

        return mergedPdfOutputStream.toByteArray();
    }



}

