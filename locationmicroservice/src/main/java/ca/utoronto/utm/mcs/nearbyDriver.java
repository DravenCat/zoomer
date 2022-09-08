package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import org.json.*;
import org.neo4j.driver.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.Record;

import static org.neo4j.driver.Values.parameters;

public class nearbyDriver implements HttpHandler{
    @Override
    public void handle(HttpExchange r) throws IOException {
        try {
            if (r.getRequestMethod().equals("GET")) {
                getNearbyDriver(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Write the needed data into output stream as described in api document
     * @param r the http exchange
     * @throws IOException any IOException
     * @throws JSONException any JSONException
     */
    private void getNearbyDriver(HttpExchange r) throws IOException, JSONException {
        int statusCode = 400;
        JSONObject res = new JSONObject();
        String requestURI = r.getRequestURI().toString();
        String[] uriSplitter = requestURI.split("/");
        // if there are extra url params send 400 and return
        if (uriSplitter.length != 4) {
            JSONObject data = new JSONObject();
            data.put("status", "BAD REQUEST");
            String response = data.toString();
            r.sendResponseHeaders(statusCode, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }
        if (uriSplitter[3].indexOf("?")<0) {
            JSONObject data = new JSONObject();
            data.put("status", "BAD REQUEST");
            String response = data.toString();
            r.sendResponseHeaders(statusCode, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }
        String uid = uriSplitter[3].substring(0, uriSplitter[3].indexOf("?"));
        String mock_url_string = "http://localhost:8000".concat(r.getRequestURI().toString());
        URL mock_url = new URL(mock_url_string);
        String url_query = mock_url.getQuery();
        if (url_query == null || url_query.contains("&") || !url_query.contains("radius=")) {
            res.put("status", "BAD REQUEST");
            String response = res.toString();
            r.sendResponseHeaders(statusCode, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }
        String radius_string = url_query.substring(url_query.indexOf("=") + 1 ,url_query.length());
        if (!this.isNumeric(radius_string)) { ;
            res.put("status", "BAD REQUEST");
            String response = res.toString();
            r.sendResponseHeaders(statusCode, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }

        Double radius = Double.parseDouble(radius_string);
        if (uid.isEmpty() ||(radius + 1e-6) < 0) {
            // no uid provided or badly-provided radius
            res.put("status", "BAD REQUEST");
            String response = res.toString();
            r.sendResponseHeaders(statusCode, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            String nearbyDriverQuery = "MATCH (u: user {uid: $x}), (n: user {is_driver: true}) " +
                    "WHERE distance(point({longitude: u.longitude, latitude: u.latitude}), " +
                    "point({longitude: n.longitude, latitude: n.latitude})) <= $y " +
                    "RETURN n.uid, n.longitude, n.latitude, n.street_at";
            try (Session session = Utils.driver.session()) {
                if (checkUserExist(uid)) {
                    Result result = session.run(nearbyDriverQuery, parameters("x", uid, "y", 1000 * radius));
                    JSONObject data = new JSONObject();
                    while (result.hasNext()) {
                        Record driver = result.next();
                        String driverUid = driver.get("n.uid").asString();
                        double longitude = driver.get("n.longitude").asDouble();
                        double latitude = driver.get("n.latitude").asDouble();
                        String street = driver.get("n.street_at").asString();
                        JSONObject driverObject = new JSONObject();
                        driverObject.put("longitude", longitude);
                        driverObject.put("latitude", latitude);
                        driverObject.put("street", street);
                        data.put(driverUid, driverObject);
                    }
                    if(data.length() == 0) {
                        statusCode = 404;
                        res.put("status", "NOT FOUND");
                        String response = res.toString();
                        r.sendResponseHeaders(statusCode, response.length());
                        OutputStream os = r.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                        return;
                    }
                    statusCode = 200;
                    res.put("status", "OK");
                    res.put("data", data);
                    String response = res.toString();
                    r.sendResponseHeaders(statusCode, response.length());
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else { //if the user is not found
                    statusCode = 404;
                    JSONObject data = new JSONObject();
                    data.put("status", "NOT FOUND");
                    String response = data.toString();
                    r.sendResponseHeaders(statusCode, response.length());
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            } catch (Exception e) {
                // error happened
                res.put("status", "INTERNAL SERVER ERROR");
                statusCode = 500;
                String response = res.toString();
                r.sendResponseHeaders(statusCode, response.length());
                OutputStream os = r.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    /**
     * Check if the user exists
     * @param uid the user's uid
     * @return true if user exists. false otherwise
     * @throws Exception any exception occurs
     */
    private boolean checkUserExist(String uid) throws Exception {
        try (Session session = Utils.driver.session()) {
            String userCheck = "MATCH (n) where n.uid=" + "'" + uid + "' RETURN n";
            Result userCheckResult = session.run(userCheck);
            return userCheckResult.hasNext();
        }
    }

    /**
     * Checking string contains only digit
     * @param str String
     * @return True/False
     */
    private boolean isNumeric(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                if (Character.compare(str.charAt(i),'.') != 0 ) {
                    return false;
                }
            }
        }
        return true;
    }
}
