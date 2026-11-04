package io.daytrader.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AddressBookRewriteTest {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rewrittenPathsReturnRestResponsesWithoutErrorDispatch() throws Exception {
        HttpResponse<String> list = get("/rest/addresses");
        assertEquals(200, list.statusCode());
        assertEquals("application/json", list.headers().firstValue("Content-Type").orElse(null));
        JsonNode addresses = objectMapper.readTree(list.body()).path("addresses");
        assertEquals(5, addresses.size());

        HttpResponse<String> existing = get("/rest/addresses/Entry2");
        assertEquals(415, existing.statusCode());
        assertEquals("", existing.body());
        assertNull(existing.headers().firstValue("Content-Type").orElse(null));

        HttpResponse<String> unknown = get("/rest/addresses/Nope");
        assertEquals(404, unknown.statusCode());
        assertEquals("", unknown.body());
        assertNull(unknown.headers().firstValue("Content-Type").orElse(null));
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
