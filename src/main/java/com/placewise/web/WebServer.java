package com.placewise.web;

import com.placewise.AppContext;
import com.placewise.exception.InvalidCompanyException;
import com.placewise.exception.InvalidStudentException;
import com.placewise.exception.PlaceWiseException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The browser version of PlaceWise: a small web server, built into the JDK ({@code com.sun.net.httpserver}),
 * that sends the page (HTML, CSS, JavaScript) and answers the page's API calls with JSON.
 *
 * <ul>
 *   <li>It listens on this computer only (localhost), never on the network.</li>
 *   <li>No executor is set, so requests are handled one at a time on the server's own thread: two clicks can
 *       never change data at the same moment.</li>
 *   <li>Errors become JSON such as {@code {"error":"No student with roll number CU9999"}}, just as the terminal
 *       menu prints {@code Error: ...} and carries on.</li>
 * </ul>
 */
public final class WebServer {

    private final HttpServer server;
    private final ApiRoutes api;
    private final PrintStream log;

    /**
     * Creates the server (it does not answer requests until {@link #start()}).
     *
     * @param app  the application services, the same ones the terminal menu uses
     * @param port the port to listen on; 0 picks any free port (used by the tests)
     * @param log  where unexpected problems are reported
     * @throws PlaceWiseException if the port cannot be used, for example because another program has it
     */
    public WebServer(AppContext app, int port, PrintStream log) {
        try {
            this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        } catch (IOException e) {
            throw new PlaceWiseException("Cannot start the web server on port " + port + " (" + e.getMessage()
                    + "). Another program may be using it: try --port 8081", e);
        }
        this.api = new ApiRoutes(app);
        this.log = log;
        server.createContext("/api/", this::handleApi);   // everything under /api/ is data
        server.createContext("/", StaticFiles::handle);   // everything else is a file of the page
        server.setExecutor(null);                          // null = one request at a time
    }

    /**
     * Starts the server, tells the user where to look, and stops it when Enter is pressed in the terminal.
     *
     * @param app  the application services
     * @param port the port to listen on
     * @param in   the terminal's input (Enter stops the server)
     * @param out  the terminal's output
     */
    public static void runUntilEnter(AppContext app, int port, InputStream in, PrintStream out) {
        WebServer web = new WebServer(app, port, out);
        web.start();
        out.println("PlaceWise web version: open http://localhost:" + web.port() + "/ in a browser.");
        out.println("Press Enter here to stop it (the terminal version still works without --web).");
        try {
            waitForEnter(in);
        } finally {
            web.stop();
        }
        out.println("Web server stopped.");
    }

    /** Begins answering requests. */
    public void start() {
        server.start();
    }

    /** Stops answering requests and frees the port. */
    public void stop() {
        server.stop(0);
    }

    /**
     * The port actually in use (useful when 0 was asked for).
     *
     * @return the port number
     */
    public int port() {
        return server.getAddress().getPort();
    }

    /** Reads until a line break or the end of the input. */
    private static void waitForEnter(InputStream in) {
        try {
            int c = in.read();
            while (c != -1 && c != '\n') {
                c = in.read();
            }
        } catch (IOException e) {
            // the input broke: treat it like Enter and stop the server
        }
    }

    private void handleApi(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String route = method + " " + exchange.getRequestURI().getPath();
        // A web page from another site cannot add this header, so it cannot change data behind the user's back.
        if (method.equals("POST") && !"1".equals(exchange.getRequestHeaders().getFirst("X-PlaceWise"))) {
            sendJson(exchange, 403, error("This request did not come from the PlaceWise page", List.of()));
            return;
        }
        try {
            String input = method.equals("POST") ? readBody(exchange) : exchange.getRequestURI().getRawQuery();
            Object answer = api.handle(route, Form.parse(input));
            if (answer == null) {
                sendJson(exchange, 404, error("No such API call: " + route, List.of()));
            } else {
                sendJson(exchange, 200, answer);
            }
        } catch (PlaceWiseException | IllegalArgumentException e) {
            // a broken rule or a bad value: the page shows the message and the user can try again
            sendJson(exchange, 400, error(e.getMessage(), problemsOf(e)));
        } catch (RuntimeException e) {
            log.println("Unexpected problem in " + route + ": " + e);
            sendJson(exchange, 500, error("Unexpected problem (" + e.getClass().getSimpleName() + "): "
                    + e.getMessage(), List.of()));
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    /** The validators list every broken rule; the page shows them as a list. */
    private static List<String> problemsOf(RuntimeException e) {
        if (e instanceof InvalidStudentException invalid) {
            return invalid.problems();
        }
        if (e instanceof InvalidCompanyException invalid) {
            return invalid.problems();
        }
        return List.of();
    }

    private static Map<String, Object> error(String message, List<String> problems) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("error", message);
        json.put("problems", problems);
        return json;
    }

    private static void sendJson(HttpExchange exchange, int status, Object answer) throws IOException {
        send(exchange, status, "application/json; charset=utf-8", Json.write(answer).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sends a complete answer and closes the exchange.
     *
     * @param exchange    the request being answered
     * @param status      the HTTP status, for example 200 or 404
     * @param contentType the type of the body
     * @param body        the bytes to send
     * @throws IOException if the browser went away
     */
    static void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store"); // always show the latest data and files
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length); // -1 means "no body"
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
