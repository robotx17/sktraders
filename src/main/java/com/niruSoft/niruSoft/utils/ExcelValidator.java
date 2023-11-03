package com.niruSoft.niruSoft.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

@Service
public class ExcelValidator {

    private static final String[] EXPECTED_HEADERS = {
            "DATE", "SALESMAN", "FARMERNAME", "ITEMQTY","ITEM","COOLIE","LUGGAGE","ADV./CASH","C%","S.C","CUSTOMERNAME","QTY","RATE","UNIT","AMOUNT","NETAMT"
    };


    public static boolean validateExcel(InputStream excelInputStream) {
        try (Workbook workbook = WorkbookFactory.create(excelInputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() <= 1) {
                return false;
            }
            Row headerRow = sheet.getRow(0);

            if (headerRow == null || headerRow.getPhysicalNumberOfCells() != EXPECTED_HEADERS.length) {
                return false;
            }
//            System.out.println(EXPECTED_HEADERS.length);
            for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
                Cell headerCell = headerRow.getCell(i);
                String cellValue = headerCell != null ? headerCell.getStringCellValue().replace(" ", "").toUpperCase() : "null";
//                System.out.println(cellValue);

                if (!isCellValid(cellValue, EXPECTED_HEADERS[i])) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isCellValid(String cellValue, String expectedValue) {
        return cellValue.equals(expectedValue);
    }

    public static boolean isHeaderValid(InputStream inputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = sheet.iterator();

            if (iterator.hasNext()) {
                Row headerRow = iterator.next();
                return isHeaderValid(headerRow);
            } else {
                return false; // The sheet is empty
            }
        }
    }

    private static boolean isHeaderValid(Row headerRow) {
        // Check if the header row contains the expected column names in the correct order
        Cell dateCell = headerRow.getCell(0);
        Cell itemCell = headerRow.getCell(1);
        Cell nameCell = headerRow.getCell(2);
        Cell qtyCell = headerRow.getCell(3);
        Cell rateCell = headerRow.getCell(4);
        Cell amtCell = headerRow.getCell(5);
        Cell countCell = headerRow.getCell(6);

        return dateCell.getStringCellValue().equalsIgnoreCase("DATE")
                && itemCell.getStringCellValue().equalsIgnoreCase("ITEM")
                && nameCell.getStringCellValue().equalsIgnoreCase("NAME")
                && qtyCell.getStringCellValue().equalsIgnoreCase("QTY")
                && rateCell.getStringCellValue().equalsIgnoreCase("Rate")
                && amtCell.getStringCellValue().equalsIgnoreCase("AMT")
                && countCell.getStringCellValue().equalsIgnoreCase("COUNT");
    }
}
