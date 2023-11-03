package com.niruSoft.niruSoft.utils;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;

public class AddressEventHandler implements IEventHandler {
    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfDocument pdfDocument = docEvent.getDocument();
        PdfPage page = docEvent.getPage();

        PageSize pageSize = PageSize.A5;
        PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDocument);

        float y = pageSize.getBottom() + 10;
        float x = (pageSize.getLeft() + pageSize.getWidth() / 2)+200;
        float fontSize = 13;

        new Canvas(canvas, page.getPageSize())
                .showTextAligned(new Paragraph().add(new Text("No 75/75A 'C' Block New Kalasipalyam Market Bangalore 560002.")
                                .setFontSize(fontSize))
                        .setFontColor(DeviceRgb.BLUE)
                        .setItalic()
                        .setMargin(0)
                        .setTextAlignment(TextAlignment.RIGHT), x, y, TextAlignment.RIGHT);
    }
}
