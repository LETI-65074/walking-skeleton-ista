package com.example.examplefeature.qr.codes.ui;

import com.example.examplefeature.Task;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class QrCodeServiceEdu {

    /** Gera PNG de QR para uma URL. */
    public byte[] urlToQrPng(String url) {
        return qrPng(url, 360); // o método ajusta dinamicamente o tamanho
    }

    /** Gera um ZIP com um PNG por URL (usa os nomes das tasks para nomear os ficheiros). */
    public byte[] urlsToZip(List<String> urls, List<Task> tasks) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {

            for (int i = 0; i < urls.size(); i++) {
                String base = sanitize(tasks.get(i).getDescription() == null ? "tarefa" : tasks.get(i).getDescription());
                byte[] png = urlToQrPng(urls.get(i));

                zip.putNextEntry(new ZipEntry(base + ".png"));
                zip.write(png);
                zip.closeEntry();
            }
            zip.finish();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar ZIP de QRs", e);
        }
    }

    // ---- geração do QR com tamanho dinâmico, quiet zone 4 e correção de erro Q ----
    private byte[] qrPng(String text, Integer requestedSize) {
        try (ByteArrayOutputStream pngOut = new ByteArrayOutputStream()) {
            int len = text == null ? 0 : text.getBytes(StandardCharsets.UTF_8).length;
            int base = (requestedSize != null ? requestedSize : 0);
            int size = Math.max(base, Math.min(1024, Math.max(360, len * 5)));

            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 4);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text == null ? "" : text, BarcodeFormat.QR_CODE, size, size, hints);

            MatrixToImageWriter.writeToStream(matrix, "PNG", pngOut, new MatrixToImageConfig());
            return pngOut.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar QR", e);
        }
    }

    private String sanitize(String s) { return s.strip().replaceAll("[^\\p{L}\\p{N}_-]+", "_"); }
}

