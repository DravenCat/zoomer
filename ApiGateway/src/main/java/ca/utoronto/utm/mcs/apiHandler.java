package ca.utoronto.utm.mcs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class apiHandler implements HttpHandler {
    private static final String locationUri = "http://locationmicroservice:8000";
    private static final String tripUri = "http://tripinfomicroservice:8000";
    private static final String userUri = "http://usermicroservice:8000";

    @Override
    public void handle(HttpExchange r) {
        try {
            forwardToMicroService(r);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Forward the http request to the microservice
     * @param r the request
     * @throws IOException any IOException
     */
    private void forwardToMicroService(HttpExchange r) throws IOException {
        HttpClient client = HttpClient.newHttpClient();
        try {
            String reqMethod = r.getRequestMethod();
            String newUri = handleUri(r.getRequestURI().toString());
            String reqBody = Utils.convert(r.getRequestBody());
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(newUri))
                    .method(reqMethod, HttpRequest.BodyPublishers.ofString(reqBody)).build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String res = response.body();
            r.sendResponseHeaders(statusCode, res.length());
            OutputStream os = r.getResponseBody();
            os.write(res.getBytes());
            os.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }catch (IllegalArgumentException e) {
            System.err.print(e.getMessage());
        }
    }

    /**
     * Handle the original uri into correct uri for calling the microservice
     * @param uri the original uri
     * @return the new uri for calling the microservice
     */
    public static String handleUri(String uri) {
        if (uri.contains("/location/")) {
            return locationUri + uri.substring(uri.indexOf("/location/"));
        } else if (uri.contains("/user/")) {
            return userUri + uri.substring(uri.indexOf("/user/"));
        } else if (uri.contains("/trip/")) {
            return tripUri + uri.substring(uri.indexOf("/trip/"));
        }
        return "";
    }
}