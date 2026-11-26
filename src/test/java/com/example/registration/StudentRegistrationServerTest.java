package com.example.registration;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentRegistrationServerTest {

    @Test
    void indexHtmlContainsAllRequiredFormFields() throws Exception {
        String html = StudentRegistrationServer.loadFormHtml();
        assertTrue(html.contains("<form"), "HTML must include a form element");
        assertTrue(html.contains("name=\"studentName\""), "HTML must include Student Name field");
        assertTrue(html.contains("name=\"rollNumber\""), "HTML must include Roll Number field");
        assertTrue(html.contains("name=\"email\""), "HTML must include Email field");
        assertTrue(html.contains("name=\"course\""), "HTML must include Course field");
        assertTrue(html.contains("type=\"submit\""), "HTML must include a Submit button");
    }

    @Test
    void parseFormParamsDecodesUrlEncodedValues() {
        Map<String, String> parsed = StudentRegistrationServer.parseFormParams(
                "studentName=Abhinav+Tripathi&rollNumber=0241CSDS232&email=test%40example.com&course=B.Tech+CSE");
        assertEquals("Abhinav Tripathi", parsed.get("studentName"));
        assertEquals("0241CSDS232", parsed.get("rollNumber"));
        assertEquals("test@example.com", parsed.get("email"));
        assertEquals("B.Tech CSE", parsed.get("course"));
    }

    @Test
    void escapeHtmlEscapesUnsafeCharacters() {
        assertEquals("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;",
                StudentRegistrationServer.escapeHtml("<script>alert('x')</script>"));
    }

    @Test
    void serverServesFormAndHandlesRegistrationPost() throws Exception {
        StudentRegistrationServer server = new StudentRegistrationServer(0);
        server.start();
        try {
            HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
            String baseUrl = "http://localhost:" + server.getPort();

            HttpResponse<String> getResp = client.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, getResp.statusCode());
            assertTrue(getResp.body().contains("Student Registration Form"));

            String formBody = "studentName=Aarav+Sharma&rollNumber=24CS101&email=aarav%40example.com&course=B.Tech+CSE";
            HttpResponse<String> postResp = client.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/register"))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(formBody))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, postResp.statusCode());
            assertTrue(postResp.body().contains("Student Registered Successfully"));
            assertTrue(postResp.body().contains("Aarav Sharma"));
            assertTrue(postResp.body().contains("24CS101"));
            assertEquals(1, server.getRegistrations().size());
        } finally {
            server.stop();
        }
    }
}
