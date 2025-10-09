package com.example.examplefeature.ui;

import com.example.examplefeature.Task;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

public class PdfService {
    public byte[] taskToPdf(Task t, Locale locale) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String html = buildHtmlForTask(t, locale);
            new PdfRendererBuilder().useFastMode().withHtmlContent(html, null).toStream(out).run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar PDF", e);
        }
    }

    private String buildHtmlForTask(Task t, Locale locale) {
    }
}
