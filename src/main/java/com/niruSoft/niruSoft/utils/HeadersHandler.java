package com.niruSoft.niruSoft.utils;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.TabStop;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TabAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.SneakyThrows;

public class HeadersHandler implements IEventHandler {
    private final String customString;

    public HeadersHandler(String customString) {
        this.customString = customString;
    }
    @SneakyThrows
    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfDocument pdfDoc = docEvent.getDocument();
        PdfPage page = docEvent.getPage();


        PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);

        float x = page.getPageSize().getWidth() - 400;
        float y = page.getPageSize().getTop() - 95;


        Paragraph headerParagraph = new Paragraph();
        headerParagraph.addTabStops(new TabStop(500f, TabAlignment.LEFT));
        headerParagraph.setFontSize(19);
        Text nameLabel = new Text("NAME :   ");
        Text nameValue = new Text("  "+customString);
        headerParagraph.add(nameLabel);
        headerParagraph.add(nameValue);
        headerParagraph.setFont(PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN));
        headerParagraph.setBold();

        new Canvas(canvas, page.getPageSize())
                .showTextAligned(headerParagraph, x, y, TextAlignment.LEFT);
    }
}


