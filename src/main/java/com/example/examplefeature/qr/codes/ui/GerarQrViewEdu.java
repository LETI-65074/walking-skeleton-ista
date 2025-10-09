package com.example.examplefeature.qr.codes.ui;



import com.example.examplefeature.Task;
import com.example.examplefeature.TaskService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.util.Locale;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.PageRequest;

import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.List;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

@Route("gerar-qr")
@PageTitle("Gerar QR")
@Menu(order = 2, icon = "vaadin:qrcode", title = "Gerar QR")
class GerarQrViewEdu extends Main {

    private final TaskService taskService;
    private final QrCodeServiceEdu qrService;

    private final Grid<Task> grid;
    private final Checkbox gerarTodas;
    private final TextField nomeBase;
    private final Button gerarBtn;

    GerarQrViewEdu(TaskService taskService, QrCodeServiceEdu qrService) {
        this.taskService = taskService;
        this.qrService   = qrService;

        gerarTodas = new Checkbox("Gerar QRs para todas");
        nomeBase = new TextField("Nome base do ficheiro");
        nomeBase.setPlaceholder("ex.: tarefa/tarefas");
        nomeBase.setClearButtonVisible(true);
        nomeBase.setMinWidth("16em");

        gerarBtn = new Button("Gerar QR", e -> gerarAcao());
        gerarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        gerarBtn.setEnabled(false);

        grid = new Grid<>();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setItems(query -> taskService.list(toSpringPageRequest(query)).stream());
        grid.addColumn(Task::getDescription).setHeader("Descrição").setAutoWidth(true).setFlexGrow(2);
        grid.addColumn(t -> t.getDueDate() == null ? "Never" : t.getDueDate().toString())
                .setHeader("Due Date").setAutoWidth(true);
        grid.addColumn(t -> t.getCreationDate().toString())
                .setHeader("Creation Date").setAutoWidth(true);
        grid.setSizeFull();

        gerarTodas.addValueChangeListener(ev ->
                gerarBtn.setEnabled(ev.getValue() || grid.asSingleSelect().getValue() != null)
        );
        grid.asSingleSelect().addValueChangeListener(ev ->
                gerarBtn.setEnabled(gerarTodas.getValue() || ev.getValue() != null)
        );

        add(gerarTodas, nomeBase, gerarBtn, grid);
        setSizeFull();
    }



    /** Base URL com IP/porta LAN (evita localhost/::1). Considera X-Forwarded-* se existir. */
    private String resolveBaseUrl() {
        var req = VaadinService.getCurrentRequest();
        if (req instanceof VaadinServletRequest vsr) {
            HttpServletRequest http = vsr.getHttpServletRequest();

            String scheme = headerOrDefault(http.getHeader("X-Forwarded-Proto"), http.getScheme());
            String host   = headerOrDefault(http.getHeader("X-Forwarded-Host"), http.getServerName());
            int port      = headerPortOrDefault(http.getHeader("X-Forwarded-Port"), http.getServerPort());

            // se for loopback/localhost, tenta descobrir IPv4 da LAN
            if (isLoopback(host)) {
                String lan = findFirstSiteLocalIPv4();
                if (lan != null) host = lan;
            }

            String portPart = (("http".equalsIgnoreCase(scheme) && port == 80)
                    || ("https".equalsIgnoreCase(scheme) && port == 443))
                    ? "" : ":" + port;

            return scheme + "://" + host + portPart;
        }
        return "http://localhost:8080";
    }

    private boolean isLoopback(String host) {
        if (host == null) return true;
        String h = host.toLowerCase();
        return "localhost".equals(h) || "127.0.0.1".equals(h) || "::1".equals(h) || "0:0:0:0:0:0:0:1".equals(h) || "[::1]".equals(h);
    }

    private String findFirstSiteLocalIPv4() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                var addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    var addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress(); // ex.: 192.168.1.23
                    }
                }
            }
            InetAddress local = InetAddress.getLocalHost();
            if (local instanceof Inet4Address) return local.getHostAddress();
        } catch (Exception ignore) {}
        return null;
    }

    private String headerOrDefault(String header, String def) {
        return (header == null || header.isBlank()) ? def : header;
    }
    private int headerPortOrDefault(String header, int def) {
        try { return (header == null || header.isBlank()) ? def : Integer.parseInt(header); }
        catch (NumberFormatException e) { return def; }
    }

    /** Download/abertura via DownloadHandler (Anchor) — compatível Vaadin 24.8+ */
    private void baixar(String contentType, byte[] data, String fileName) {
        Anchor a = new Anchor(ev -> {
            ev.setContentType(contentType);
            ev.setFileName(fileName);
            try (OutputStream os = ev.getOutputStream()) { os.write(data); }
        }, "");
        a.getElement().setAttribute("hidden", true);
        add(a);

        UI.getCurrent().getPage().open(a.getHref());
        // opcional: remover depois
        // getUI().ifPresent(ui -> ui.accessLater(a::remove));
    }

    private void gerarAcao() {
        try {
            String base = nomeBase.isEmpty() || nomeBase.getValue().isBlank()
                    ? (gerarTodas.getValue() ? "tarefas" : "tarefa")
                    : sanitize(nomeBase.getValue());
            String ts = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());

            if (gerarTodas.getValue()) {
                List<Task> tasks = taskService.list(PageRequest.of(0, 10_000));

                // 1) Markdown -> HTML
                String md = markdownParaTarefas(tasks, getLocale());
                String html = renderMarkdown(md);

                // 2) Sobe servidor local em porta aleatória
                String localUrl = HtmlShareServerEdu.start(html, 5); // encerra em 5 min

                // 3) QR para o localhost com a página pronta
                byte[] png = qrService.urlToQrPng(localUrl);
                baixar("image/png", png, base + "_" + ts + ".png");

                Notification.show("QR gerado para página local em " + localUrl,
                                3500, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } else {
                Task t = grid.asSingleSelect().getValue();
                if (t == null) {
                    Notification.show("Seleciona uma tarefa ou marca 'Gerar todas'.",
                                    2500, Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_WARNING);
                    return;
                }
                if (nomeBase.isEmpty() || nomeBase.getValue().isBlank()) {
                    base = sanitize(nvl(t.getDescription()).isBlank() ? "tarefa" : t.getDescription());
                }

                // 1) Markdown -> HTML
                String md = markdownParaUmaTarefa(t, getLocale());
                String html = renderMarkdown(md);

                // 2) Sobe servidor local em porta aleatória
                String localUrl = HtmlShareServerEdu.start(html, 5);

                // 3) QR para a página local
                byte[] png = qrService.urlToQrPng(localUrl);
                baixar("image/png", png, base + "_" + ts + ".png");

                Notification.show("QR gerado para página local em " + localUrl,
                                3500, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }

            nomeBase.clear();
        } catch (Exception ex) {
            Notification.show("Erro ao gerar QR/página local: " + ex.getMessage(),
                            4000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private String markdownParaUmaTarefa(Task t, Locale locale) {
        var dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale).withZone(ZoneId.systemDefault());
        var df  = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale);

        String desc = nvl(t.getDescription());
        String due  = t.getDueDate() == null ? "Never" : df.format(t.getDueDate());
        String created = dtf.format(t.getCreationDate());

        return """
           # Tarefa

           **Descrição:** %s

           **Due Date:** %s

           **Creation Date:** %s
           """.formatted(desc, due, created);
    }

    private String markdownParaTarefas(List<Task> tasks, Locale locale) {
        var dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale).withZone(ZoneId.systemDefault());
        var df  = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale);

        StringBuilder md = new StringBuilder("# Tarefas\n\n");
        for (Task t : tasks) {
            String desc = nvl(t.getDescription());
            String due  = t.getDueDate() == null ? "Never" : df.format(t.getDueDate());
            String created = dtf.format(t.getCreationDate());
            md.append("- **Descrição:** ").append(desc).append("\n")
                    .append("  - **Due Date:** ").append(due).append("\n")
                    .append("  - **Creation Date:** ").append(created).append("\n\n");
        }
        return md.toString();
    }

    private String renderMarkdown(String markdown) {
        MutableDataSet opts = new MutableDataSet();
        Parser parser = Parser.builder(opts).build();
        HtmlRenderer renderer = HtmlRenderer.builder(opts).build();
        Node doc = parser.parse(markdown == null ? "" : markdown);
        return renderer.render(doc);
    }


    private String urlEncode(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private String sanitize(String s) { return s.strip().replaceAll("[^\\p{L}\\p{N}_-]+", "_"); }
    private String nvl(String s) { return s == null ? "" : s; }
}

