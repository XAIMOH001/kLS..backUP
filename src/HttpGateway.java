import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.nio.charset.StandardCharsets;

public class HttpGateway {
    private HttpServer server;

    public HttpGateway(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));
    }

    public void start() {
        server.createContext("/api/branches", ex -> route(ex, Router::branches));
        server.createContext("/api/drinks", ex -> route(ex, Router::drinks));
        server.createContext("/api/orders", ex -> route(ex, Router::placeOrder));
        server.createContext("/api/restock", ex -> route(ex, Router::restock));
        server.createContext("/api/stock", ex -> route(ex, Router::stock));
        server.createContext("/api/reports/customers", ex -> route(ex, Router::reportCustomers));
        server.createContext("/api/reports/branches", ex -> route(ex, Router::reportBranches));
        server.createContext("/api/reports/total", ex -> route(ex, Router::reportTotal));
        server.createContext("/api/reports/lowstock", ex -> route(ex, Router::reportLowStock));
        
        server.createContext("/", ex -> {
            try {
                String path = ex.getRequestURI().getPath();
                if (path.equals("/")) path = "/order.html";
                if (path.equals("/admin")) path = "/dashboard.html";
                
                File file = new File("web" + path);
                if (file.exists() && !file.isDirectory()) {
                    String contentType = "text/plain";
                    if (path.endsWith(".html")) contentType = "text/html";
                    else if (path.endsWith(".css")) contentType = "text/css";
                    else if (path.endsWith(".js")) contentType = "application/javascript";
                    else if (path.endsWith(".png")) contentType = "image/png";
                    else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) contentType = "image/jpeg";
                    else if (path.endsWith(".svg")) contentType = "image/svg+xml";

                    ex.getResponseHeaders().set("Content-Type", contentType);
                    ex.sendResponseHeaders(200, file.length());
                    try (OutputStream os = ex.getResponseBody()) {
                        Files.copy(file.toPath(), os);
                    }
                } else {
                    String msg = "404 Not Found";
                    ex.sendResponseHeaders(404, msg.length());
                    try (OutputStream os = ex.getResponseBody()) {
                        os.write(msg.getBytes());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ex.close();
            }
        });

        server.start();
        printConnectionInfo(server.getAddress().getPort());
    }

    private void printConnectionInfo(int port) {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("DRINKS POS SERVER STARTED");
        System.out.println("=".repeat(40));
        System.out.println("Local:     http://localhost:" + port);
        
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        System.out.println("Network:   http://" + addr.getHostAddress() + ":" + port);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Network connection info unavailable.");
        }
        System.out.println("=".repeat(40) + "\n");
    }

    public interface ApiHandler {
        Object handle(HttpExchange ex) throws Exception;
    }

    private void route(HttpExchange ex, ApiHandler h) {
        try {
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                ex.close();
                return;
            }
            
            Object result = h.handle(ex);
            String json = Json.toJson(result);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                String errorMsg = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "Error";
                String error = "{\"error\":\"" + errorMsg + "\"}";
                byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "application/json");
                ex.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } finally {
            ex.close();
        }
    }
}
