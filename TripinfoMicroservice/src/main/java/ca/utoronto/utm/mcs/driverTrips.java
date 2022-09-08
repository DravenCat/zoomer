package ca.utoronto.utm.mcs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class driverTrips implements HttpHandler {
    private final Mongodb mongodb;

    public driverTrips(Mongodb mongodb) { this.mongodb = mongodb; }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if(exchange.getRequestMethod().equals("GET")) {
                this.handleGet(exchange);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handling GET /trip/driver/:uid
     *
     * @param r HttpExchange
     * @throws JSONException JSONException
     * @throws IOException IOException
     */
    private void handleGet(HttpExchange r) throws JSONException, IOException {

        String[] url = r.getRequestURI().getPath().split("/");
        JSONObject res = new JSONObject();
        if(url.length != 4) {
            res.put("status","BAD REQUEST");
            String response  = res.toString();
            r.sendResponseHeaders(400,response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            try {
                String driver_uid = url[3];
                if(!this.mongodb.findDriver(driver_uid)) {
                    res.put("status","NOT FOUND");
                    String response  = res.toString();
                    r.sendResponseHeaders(404,response.length());
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                    return;
                }
                ArrayList<JSONObject> result = this.mongodb.findTripDriver(driver_uid);
                JSONObject trips_json = new JSONObject();
                trips_json.put("trips",result);
                res.put("data",trips_json);
                res.put("status","OK");
                String response  = res.toString();
                r.sendResponseHeaders(200,response.length());
                OutputStream os = r.getResponseBody();
                os.write(response.getBytes());
                os.close();
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
