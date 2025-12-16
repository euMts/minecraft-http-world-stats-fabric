package dev.matheuspass.http_world_stats;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public final class LocalHttpServer {
    private static final Gson GSON = new Gson();

    // We'll keep a snapshot here to avoid touching MC internals from the HTTP thread.
    private final AtomicReference<JsonObject> worldSnapshot = new AtomicReference<>(new JsonObject());

    private HttpServer server;

    public void start(int port) {
        if (server != null) return;

        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

            // Register specific endpoints first
            server.createContext("/world", ex -> handleJson(ex, worldSnapshot.get()));
            server.createContext("/health", ex -> handleText(ex, 200, "ok"));
            
            // Root endpoint and catch-all for unknown routes (registered last)
            server.createContext("/", ex -> {
                String path = ex.getRequestURI().getPath();
                if ("/".equals(path)) {
                    // Root path - show API info
                    String info = "HTTP World Stats API\n\n" +
                            "Available endpoints:\n" +
                            "  GET /health - Health check\n" +
                            "  GET /world  - World statistics (JSON)\n";
                    handleText(ex, 200, info);
                } else {
                    // Unknown route - return 404 with helpful message
                    handleText(ex, 404, "404 Not Found\n\n" +
                            "The requested path '" + path + "' was not found.\n\n" +
                            "Available endpoints:\n" +
                            "  GET /health - Health check\n" +
                            "  GET /world  - World statistics (JSON)\n");
                }
            });

            // Dedicated thread pool for HTTP requests
            server.setExecutor(Executors.newFixedThreadPool(2));
            server.start();

            System.out.println("[http_world_stats] Local HTTP server started on http://127.0.0.1:" + port);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start HTTP server", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public void setWorldSnapshot(JsonObject snapshot) {
        worldSnapshot.set(snapshot);
    }

    private static void handleJson(HttpExchange ex, JsonObject obj) {
        try {
            byte[] bytes = GSON.toJson(obj).getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // optional (browser)
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            safeFail(ex, e);
        }
    }

    private static void handleText(HttpExchange ex, int code, String text) {
        try {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            ex.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            safeFail(ex, e);
        }
    }

    private static void safeFail(HttpExchange ex, Exception e) {
        try {
            byte[] bytes = ("error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(500, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception ignored) {}
    }
}

