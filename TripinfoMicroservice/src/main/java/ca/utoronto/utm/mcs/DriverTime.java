package ca.utoronto.utm.mcs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DriverTime implements HttpHandler {
    private final Mongodb mongodb;

    public DriverTime(Mongodb mongodb) { this.mongodb = mongodb; }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (exchange.getRequestMethod().equals("GET")) {
                this.handleGet(exchange);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handling GET /trip/driverTime/:_id
     *
     * @param r HttpExchange
     * @throws JSONException JSONException
     * @throws IOException IOException
     */
    private void handleGet(HttpExchange r) throws JSONException, IOException {
        String[] url = r.getRequestURI().getPath().split("/");
        JSONObject res = new JSONObject();
        if (url.length != 4) {
            res.put("status","BAD REQUEST");
            String response  = res.toString();
            r.sendResponseHeaders(400,response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            try {
                String id = url[3];
                if (!this.mongodb.findById_boolean(id)) {
                    res.put("status","NOT FOUND");
                    String response  = res.toString();
                    r.sendResponseHeaders(404,response.length());
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                    return;
                }
                Document object = this.mongodb.findById(id);

                String passenger_uid = object.get("passenger").toString();
                String driver_uid = object.get("driver").toString();
                HttpClient client = HttpClient.newHttpClient();
                String uri =
                        "http://locationmicroservice:8000/location/navigation/" + driver_uid
                                + "?passengerUid=" + passenger_uid;
                HttpRequest request_location = HttpRequest.newBuilder().uri(URI.create(uri))
                        .method("GET", HttpRequest.BodyPublishers.noBody()).build();
                HttpResponse<String> response_location =
                        client.send(request_location, HttpResponse.BodyHandlers.ofString());
                int status = response_location.statusCode();

                if (status == 404) {
                    res.put("status","NOT FOUND");
                    String response  = res.toString();
                    r.sendResponseHeaders(404,response.length());
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else if (status == 500) {
                    res.put("status","INTERNAL SERVER ERROR");
                    String response  = res.toString();
                    r.sendResponseHeaders(500,response.length());
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else if (status == 400) {
                    res.put("status","BAD REQUEST");
                    String response  = res.toString();
                    r.sendResponseHeaders(400,response.length());
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else {
                    String response_body = response_location.body();
                    JSONObject response_JSON = new JSONObject(response_body);
                    String data_location = response_JSON.getString("data");
                    JSONObject finalize = new JSONObject(data_location);
                    String arrival_time = finalize.getString("total_time");

                    JSONObject arrivalTime = new JSONObject();
                    arrivalTime.put("arrival_time",arrival_time);

                    res.put("status","OK");
                    res.put("data",arrivalTime);
                    String response  = res.toString();
                    r.sendResponseHeaders(200,response.length());
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                res.put("status","INTERNAL SERVER ERROR");
                String response  = res.toString();
                r.sendResponseHeaders(500,response.length());
                OutputStream os = r.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }
}
