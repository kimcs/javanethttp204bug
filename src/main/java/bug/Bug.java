package bug;

import io.undertow.Undertow;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Bug {

    public static void main(String[] args) throws IOException, InterruptedException {
        Undertow server = undertow();
        server.start();
        try {
            if (!runTestForStatusCode(1, 200)) {
                return; // TEST FAILED
            }
            if (!runTestForStatusCode(2, 204)) {
                return; // TEST FAILED
            }
            System.out.format("%nTEST PASSED%n");
        } finally {
            server.stop();
        }
    }

    private static boolean runTestForStatusCode(int r, int expectedHttpStatus) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        System.out.format("%n%nREQUEST %d%n%n", r);
        get(expectedHttpStatus);
        long durationMs = System.currentTimeMillis() - startTime;
        long durationSec = TimeUnit.MILLISECONDS.toSeconds(durationMs);
        long retryStartTime = System.currentTimeMillis();
        retryUsingUrlClient(expectedHttpStatus);
        long retryDurationMs = System.currentTimeMillis() - retryStartTime;
        long retryDurationSec = TimeUnit.MILLISECONDS.toSeconds(retryDurationMs);
        if (durationSec > 5) {
            System.out.format("TEST FAILED. Http request spent %d seconds to complete (URL retry spent %d seconds), GET with status %d%n", durationSec, retryDurationSec, expectedHttpStatus);
            return false;
        }
        return true;
    }

    private static void retryUsingUrlClient(int expectedStatusCode) throws IOException {
        System.out.format("%nRetrying request using URL client.%n");
        URL url = new URL("http://localhost:14632/" + expectedStatusCode);
        URLConnection urlConnection = url.openConnection();
        // RESPONSE HEADERS
        Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
        System.out.format("%n%s%n", headerFields.get(null).get(0));
        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            if (entry.getKey() != null) {
                System.out.format("%s: %s%n", entry.getKey(), entry.getValue().get(0));
            }
        }
        System.out.format("%n");
        // RESPONSE BODY
        try (InputStream is = urlConnection.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            System.out.format("%s%n%n", new String(bytes, StandardCharsets.UTF_8));
        }
    }

    private static void get(final int expectedStatus) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:14632/" + expectedStatus)).GET().build();
        System.out.format("Client sending GET request expecting status: %d%n", expectedStatus);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int actualStatus = response.statusCode();
        String body = response.body();
        System.out.format("Client received response with Status: %d,  Body: \"%s\"%n", actualStatus, body);
    }

    private static Undertow undertow() {
        return Undertow.builder()
                .addHttpListener(14632, "localhost")
                .setHandler(httpServerExchange -> {
                    int expectedStatus = Integer.parseInt(httpServerExchange.getRequestPath().substring(1));
                    System.out.format("Undertow sending response with statuscode: %d%n", expectedStatus);
                    httpServerExchange.setStatusCode(expectedStatus);
                    httpServerExchange.getResponseSender().send("Hello from Undertow, echoing statuscode " + expectedStatus);
                })
                .build();
    }
}
