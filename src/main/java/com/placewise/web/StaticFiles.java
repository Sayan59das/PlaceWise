package com.placewise.web;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Sends the page's own files (HTML, CSS, JavaScript, fonts), which live in {@code src/main/resources/web}
 * and end up inside the jar.
 */
final class StaticFiles {

    /**
     * Only names such as {@code /app.js} or {@code /js/students.js} are allowed. No dots except before the
     * extension, so a path like {@code /../secret} can never reach a file outside the web folder.
     */
    private static final Pattern SAFE_PATH = Pattern.compile("/([a-z0-9-]+/)?[a-z0-9-]+\\.(html|css|js|woff2)");

    private StaticFiles() {
    }

    static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        if (!exchange.getRequestMethod().equals("GET") || !SAFE_PATH.matcher(path).matches()) {
            notFound(exchange);
            return;
        }
        try (InputStream file = StaticFiles.class.getResourceAsStream("/web" + path)) {
            if (file == null) {
                notFound(exchange);
                return;
            }
            WebServer.send(exchange, 200, contentType(path), file.readAllBytes());
        }
    }

    private static String contentType(String path) {
        String extension = path.substring(path.lastIndexOf('.') + 1);
        return switch (extension) {
            case "html" -> "text/html; charset=utf-8";
            case "css" -> "text/css; charset=utf-8";
            case "js" -> "text/javascript; charset=utf-8";
            case "woff2" -> "font/woff2";
            default -> "application/octet-stream";
        };
    }

    private static void notFound(HttpExchange exchange) throws IOException {
        WebServer.send(exchange, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
    }
}
