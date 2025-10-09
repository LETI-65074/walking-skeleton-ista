package com.example.examplefeature.ui;

import com.example.examplefeature.Task;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;


@Service
public class PdfService {
    public byte[] taskToPdf(Task t, Locale locale) {    //recebe uma tarefa e a converte num ficheiro PDF (em bytes)
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String html = buildHtmlForTask(t, locale);
            new PdfRendererBuilder().useFastMode().withHtmlContent(html, null).toStream(out).run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar PDF", e);
        }
    }

    private String buildHtmlForTask(Task t, Locale locale) {

        var dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .withLocale(locale).withZone(ZoneId.systemDefault());
        var df  = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale);

        String desc = esc(t.getDescription());
        String due  = t.getDueDate() == null ? "Never" : esc(df.format(t.getDueDate()));
        String created = esc(dtf.format(t.getCreationDate()));

        return """
           <html><head><meta charset="utf-8"/>
           <style>
             @page { size:A4; margin:20mm; }
             body{font-family:Arial,Helvetica,sans-serif;font-size:12px;color:#222}
             h1{font-size:20px;margin:0 0 12px}
             .row{margin:6px 0}
             .label{font-weight:bold}
             hr{margin:12px 0;border:none;border-top:1px solid #ccc}
           </style></head><body>
           <h1>Tarefa</h1>
           <div class="row"><span class="label">Descrição:</span> %s</div>
           <div class="row"><span class="label">Due Date:</span> %s</div>
           <div class="row"><span class="label">Creation Date:</span> %s</div>
           <hr/>
           <div>Gerado em: %s</div>
           </body></html>
           """.formatted(desc, due, created, esc(java.time.LocalDateTime.now().toString()));
        }

    //gera um PDF com TODAS as tarefas como tabela.
    public byte[] tasksToPdf(List<Task> tasks, Locale locale) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String html = buildHtmlForTasks(tasks, locale);
            new PdfRendererBuilder().useFastMode().withHtmlContent(html, null).toStream(out).run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar PDF (todas as tarefas)", e);
        }
    }

    private String buildHtmlForTasks(List<Task> tasks, Locale locale) {
        var dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale).withZone(ZoneId.systemDefault());
        var df  = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale);

        StringBuilder rows = new StringBuilder();
        for (Task t : tasks) {
            String desc = esc(t.getDescription());
            String due  = t.getDueDate() == null ? "Never" : esc(df.format(t.getDueDate()));
            String created = esc(dtf.format(t.getCreationDate()));
            rows.append("<tr><td>").append(desc)
                    .append("</td><td>").append(due)
                    .append("</td><td>").append(created)
                    .append("</td></tr>");
        }

        // evitamos .formatted aqui para não conflitar com possíveis % em CSS
        return """
           <html><head><meta charset="utf-8"/>
           <style>
             @page { size:A4; margin:20mm; }
             body{font-family:Arial,Helvetica,sans-serif;font-size:12px;color:#222}
             h1{font-size:18px;margin:0 0 12px}
             table{width:100%;border-collapse:collapse}
             th,td{padding:6px 8px;border-bottom:1px solid #ccc;text-align:left;vertical-align:top}
           </style></head><body>
           <h1>Lista de Tarefas</h1>
           <table>
             <thead><tr><th>Descrição</th><th>Due Date</th><th>Creation Date</th></tr></thead>
             <tbody>""" + rows + """
             </tbody>
           </table>
           </body></html>
           """;
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

}
