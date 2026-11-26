package com.example.registration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * Minimal HTTP server serving the Student Registration form and handling submissions in memory.
 */
public class StudentRegistrationServer {

    public record StudentRecord(String studentName, String rollNumber, String email, String course) {}

    private final HttpServer server;
    private final List<StudentRecord> registrations = new CopyOnWriteArrayList<>();

    public StudentRegistrationServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.setExecutor(Executors.newFixedThreadPool(4));
        registerRoutes();
    }

    private void registerRoutes() {
        server.createContext("/", this::handleFormPage);
        server.createContext("/index.html", this::handleFormPage);
        server.createContext("/register", this::handleRegistration);
        server.createContext("/health", this::handleHealth);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public List<StudentRecord> getRegistrations() {
        return List.copyOf(registrations);
    }

    private void handleFormPage(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "text/plain; charset=UTF-8", "Method Not Allowed");
            return;
        }
        String html = loadFormHtml();
        sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
    }

    private void handleRegistration(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Location", "/");
            sendResponse(exchange, 302, "text/plain; charset=UTF-8", "");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseFormParams(body);

        String studentName = params.getOrDefault("studentName", "").trim();
        String rollNumber = params.getOrDefault("rollNumber", "").trim();
        String email = params.getOrDefault("email", "").trim();
        String course = params.getOrDefault("course", "").trim();

        if (studentName.isEmpty() || rollNumber.isEmpty() || email.isEmpty() || course.isEmpty()) {
            sendResponse(exchange, 400, "text/html; charset=UTF-8",
                    buildErrorPage("All fields (Student Name, Roll Number, Email, Course) are required."));
            return;
        }

        StudentRecord record = new StudentRecord(studentName, rollNumber, email, course);
        registrations.add(record);
        sendResponse(exchange, 200, "text/html; charset=UTF-8", buildConfirmationPage(record, registrations));
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"status\":\"UP\"}");
    }

    public static String loadFormHtml() throws IOException {
        try (InputStream in = StudentRegistrationServer.class.getResourceAsStream("/index.html")) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        Path localFile = Path.of("index.html");
        if (Files.exists(localFile)) {
            return Files.readString(localFile, StandardCharsets.UTF_8);
        }
        throw new IOException("index.html not found on classpath or working directory.");
    }

    public static Map<String, String> parseFormParams(String body) {
        Map<String, String> params = new LinkedHashMap<>();
        if (body == null || body.isBlank()) {
            return params;
        }
        for (String pair : body.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    public static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String buildConfirmationPage(StudentRecord latest, List<StudentRecord> allRecords) {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < allRecords.size(); i++) {
            StudentRecord r = allRecords.get(i);
            rows.append("<tr>")
                    .append("<td>").append(i + 1).append("</td>")
                    .append("<td>").append(escapeHtml(r.studentName())).append("</td>")
                    .append("<td>").append(escapeHtml(r.rollNumber())).append("</td>")
                    .append("<td>").append(escapeHtml(r.email())).append("</td>")
                    .append("<td>").append(escapeHtml(r.course())).append("</td>")
                    .append("</tr>");
        }

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Registration Successful</title>
                    <style>
                        body { font-family: Arial, Helvetica, sans-serif; background: #f4f6f9; padding: 40px 16px; color: #1f2937; }
                        .card { max-width: 720px; margin: 0 auto; background: #fff; padding: 28px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); }
                        h1 { margin-top: 0; color: #15803d; font-size: 22px; }
                        .summary { background: #f0fdf4; border: 1px solid #bbf7d0; padding: 14px 18px; border-radius: 6px; margin-bottom: 20px; }
                        table { width: 100%%; border-collapse: collapse; margin-top: 12px; font-size: 14px; }
                        th, td { border: 1px solid #e5e7eb; padding: 8px 10px; text-align: left; }
                        th { background: #f9fafb; }
                        .back-link { display: inline-block; margin-top: 18px; color: #2563eb; text-decoration: none; font-weight: 600; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>Student Registered Successfully</h1>
                        <div class="summary">
                            <p><strong>Student Name:</strong> %s</p>
                            <p><strong>Roll Number:</strong> %s</p>
                            <p><strong>Email:</strong> %s</p>
                            <p><strong>Course:</strong> %s</p>
                        </div>
                        <h3>Registered Students (%d)</h3>
                        <table>
                            <thead>
                                <tr><th>#</th><th>Student Name</th><th>Roll Number</th><th>Email</th><th>Course</th></tr>
                            </thead>
                            <tbody>%s</tbody>
                        </table>
                        <a class="back-link" href="/">&larr; Register Another Student</a>
                    </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(latest.studentName()),
                escapeHtml(latest.rollNumber()),
                escapeHtml(latest.email()),
                escapeHtml(latest.course()),
                allRecords.size(),
                rows.toString()
        );
    }

    private static String buildErrorPage(String message) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><title>Validation Error</title></head>
                <body style="font-family: Arial, sans-serif; padding: 40px;">
                    <h2 style="color: #b91c1c;">Registration Error</h2>
                    <p>%s</p>
                    <a href="/">Go back to registration form</a>
                </body>
                </html>
                """.formatted(escapeHtml(message));
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void main(String[] args) throws IOException {
        int port = 8080;
        String envPort = System.getenv("PORT");
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else if (envPort != null && !envPort.isBlank()) {
            port = Integer.parseInt(envPort.trim());
        }

        StudentRegistrationServer app = new StudentRegistrationServer(port);
        app.start();
        System.out.println("Student Registration Server started on http://localhost:" + app.getPort());
    }
}
