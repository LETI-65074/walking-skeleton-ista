package com.example.examplefeature.qr.codes.ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class HtmlShareServerEdu {

    /** Inicia um servidor só em localhost com porta aleatória e serve o HTML fornecido. */
    static String start(String html, long autoShutdownMinutes) {
        Objects.requireNonNull(html, "html");
        try {
            // escuta em todas as interfaces, porta aleatória
            InetSocketAddress addr = new InetSocketAddress(0);
            HttpServer server = HttpServer.create(addr, 0);
            server.createContext("/", new StaticHtmlHandler(html));
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            // auto-desliga (default 5 min)
            long minutes = autoShutdownMinutes <= 0 ? 5 : autoShutdownMinutes;
            @SuppressWarnings("resource")
            ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
            ses.schedule(() -> {
                try {
                    server.stop(0);
                } finally {
                    ses.shutdownNow();
                }
            }, minutes, TimeUnit.MINUTES);

            int port = server.getAddress().getPort();
            String host = firstSiteLocalIPv4(); // 192.168.x.y (ou null)
            if (host == null) host = "127.0.0.1"; // fallback
            return "http://" + host + ":" + port + "/";
        } catch (Exception e) {
            throw new RuntimeException("Falha ao iniciar servidor local para partilhar HTML", e);
        }
    }

    private static String firstSiteLocalIPv4() {
        try {
            var ifaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                var ni = ifaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                var addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    var a = addrs.nextElement();
                    if (a instanceof java.net.Inet4Address && a.isSiteLocalAddress()) {
                        return a.getHostAddress(); // ex.: 192.168.1.23
                    }
                }
            }
            var local = java.net.InetAddress.getLocalHost();
            if (local instanceof java.net.Inet4Address) return local.getHostAddress();
        } catch (Exception ignore) {}
        return null;
    }
    private static class StaticHtmlHandler implements HttpHandler {
        private final byte[] payload;

        StaticHtmlHandler(String html) {
            String wrapped = """
                <!doctype html>
                <html lang="pt">
                  <head>
                    <meta charset="utf-8"/>
                    <meta name="viewport" content="width=device-width, initial-scale=1"/>
                    <title>Tarefas</title>
                    <style>
                      body{font-family:system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;
                           margin:24px; line-height:1.5; color:#222;}
                      h1,h2{margin:0 0 12px}
                      hr{margin:16px 0; border:0; border-top:1px solid #ddd}
                      .card{max-width:850px}
                      pre{white-space:pre-wrap}
                    </style>
                  </head>
                  <body>
                    <div class="card">
                      %s
                      <hr/>
                      <small>Servido localmente em 127.0.0.1</small>
                    </div>
                  </body>
                </html>
                """.formatted(html);
            this.payload = wrapped.getBytes(StandardCharsets.UTF_8);
        }

        @Override public void handle(HttpExchange ex) {
            try (OutputStream os = ex.getResponseBody()) {
                ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                ex.sendResponseHeaders(200, payload.length);
                os.write(payload);
            } catch (Exception ignored) {}
        }
    }
}

