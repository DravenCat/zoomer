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
import java.util.Iterator;

public class trip implements HttpHandler {
    private final Mongodb mongodb;

    public trip(Mongodb mongodbDAO) { this.mongodb = mongodbDAO; }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if(exchange.getRequestMethod().equals("POST")) {
                this.handlePost(exchange);
            }
            if(exchange.getRequestMethod().equals("PATCH")) {
                this.handlePatch(exchange);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handling post request
     * @param r HttpExchange
     * @throws IOException IOException
     * @throws JSONException JSONException
     */
    private void handlePost(HttpExchange r) throws IOException, JSONException {
        String path = r.getRequestURI().getPath();
        if(path.equals("/trip/request")) {
            this.tripRequest(r);
        }
        if(path.equals("/trip/confirm")) {
            this.tripConfirm(r);
        }
    }

    /**
     * Handle POST /trip/request route
     * @param r HttpExchange
     * @throws IOException IOException
     * @throws JSONException JSONException
     */
    private void tripRequest(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject req = new JSONObject(body);
        JSONObject res = new JSONObject();
        if(req.has("uid") && req.has("radius")) {
            try {
                String passenger_uid = req.getString("uid");
                String radius = req.getString("radius");

                HttpClient client = HttpClient.newHttpClient();
                String uri = "http://locationmicroservice:8000/location/nearbyDriver/" + passenger_uid + "?radius=" + radius;
                HttpRequest request_location = HttpRequest.newBuilder().uri(URI.create(uri))
                        .method("GET", HttpRequest.BodyPublishers.noBody()).build();
                HttpResponse<String> response_location =
                        client.send(request_location, HttpResponse.BodyHandlers.ofString());
                int status = response_location.statusCode();

                if(status == 400) {
                    res.put("status", "BAD REQUEST");
                    String response = res.toString();
                    r.sendResponseHeaders(status, response.length());
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
                else if (status == 404) {
                    res.put("status", "NOT FOUND");
                    String response = res.toString();
                    r.sendResponseHeaders(status, response.length());
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
                else if (status == 200) {
                    String response_body = response_location.body();
                    JSONObject response_JSON = new JSONObject(response_body);
                    String data = response_JSON.getString("data");
                    JSONObject finalize = new JSONObject(data);
                    String driverArray[] = new String[finalize.length()];
                    int index = 0;

                    Iterator iterator = finalize.keys();
                    while(iterator.hasNext()) {
                        String key = (String) iterator.next();
                        driverArray[index] = key;
                        index++;
                    }

                    res.put("data",driverArray);
                    res.put("status", "OK");
                    String response = res.toString();
                    r.sendResponseHeaders(status, response.length());
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
                else {
                    res.put("status", "INTERNAL SERVER ERROR");
                    String response = res.toString();
                    r.sendResponseHeaders(500, response.length());
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }

            } catch (Exception e) {
                res.put("status", "INTERNAL SERVER ERROR");
                String response = res.toString();
                r.sendResponseHeaders(500, response.length());
                OutputStream os = r.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        } else {
            res.put("status","BAD REQUEST");
            String response  = res.toString();
            r.sendResponseHeaders(400,response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    /**
     * Handle POST /trip/confirm
     * @param r HttpExchange
     * @throws IOException IOException
     */
    private void tripConfirm(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject req = new JSONObject(body);
        JSONObject res = new JSONObject();
        if(req.has("driver") && req.has("passenger") && req.has("startTime")) {
            try {
                String driver = req.getString("driver");
                String passenger = req.getString("passenger");
                String startTime = req.getString("startTime");
                if (Utils.isNumeric(startTime) && !driver.equals("")
                        && !passenger.equals("") && startTime.length() == 10) {
                    // check the driver and the passenger exist and driver is actually a driver
                    if (checkUser(passenger) == 404) {
                        res.put("status","NOT FOUND");
                        String response  = res.toString();
                        r.sendResponseHeaders(404,response.length());
                        OutputStream os = r.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                        return;
                    }
                    if (checkUser(driver) == 404) {
                        res.put("status","NOT FOUND");
                        String response  = res.toString();
                        r.sendResponseHeaders(404,response.length());
                        OutputStream os = r.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                        return;
                    }
                    // check whether exist in mongodb
                    // this.mongodb.deleteDocument(driver,passenger,startTime);
                    if(this.mongodb.findByDriverPassenger(driver,passenger,startTime)) {
                        System.out.println("here");
                        res.put("status","FORBIDDEN Exist");
                        String response  = res.toString();
                        r.sendResponseHeaders(403,response.length());
                        OutputStream os = r.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    } else {
                        Document object = this.mongodb.confirmCreateDocument(driver,passenger,startTime);
                        String id = object.getObjectId("_id").toString();
                        res.put("status","OK");
                        res.put("data",id);
                        String response  = res.toString();
                        r.sendResponseHeaders(200,response.length());
                        OutputStream os = r.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }
                } else {
                    res.put("status","BAD REQUEST");
                    String response  = res.toString();
                    r.sendResponseHeaders(400,response.length());
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                res.put("status","INTERNAL SERVER ERROR");
                String response = res.toString();
                r.sendResponseHeaders(500,response.length());
                OutputStream os = r.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        } else {
            res.put("status","BAD REQUEST");
            String response  = res.toString();
            r.sendResponseHeaders(400,response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }


    /**
     * Check whether a user exist in location database
     * @param uid user uid
     * @return int status
     * @throws IOException IOException
     * @throws InterruptedException InterruptedException
     */
    private int checkUser(String uid) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String uri = "http://locationmicroservice:8000/location/" + uid;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                .method("GET", HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> response_location =
                client.send(request, HttpResponse.BodyHandlers.ofString());
        return response_location.statusCode();
    }


    /**
     * Handle PATCH /trip/:_id route
     * @param r HttpExchange
     * @throws IOException IOException
     * @throws JSONException JSONException
     */
    private void handlePatch(HttpExchange r) throws IOException, JSONException {
        String[] url = r.getRequestURI().getPath().split("/");
        String body = Utils.convert(r.getRequestBody());
        JSONObject req = new JSONObject(body);
        JSONObject res = new JSONObject();
        try {
            if(url.length == 3) {
                if (req.has("distance") && req.has("endTime") && req.has("timeElapsed") &&
                        req.has("discount") && req.has("totalCost") && req.has("driverPayout")) {
                    String distance = req.getString("distance");
                    String endTime = req.getString("endTime");
                    String timeElapsed = req.getString("timeElapsed");
                    String discount = req.getString("discount");
                    String totalCost = req.getString("totalCost");
                    String driverPayout = req.getString("driverPayout");
                    String id = url[2];

                    // check req body param type
                    if (Utils.isNumeric(distance) && Utils.isNumeric(totalCost) && Utils.isNumeric(discount)
                            && Utils.isNumeric(driverPayout) && Utils.isNumeric(endTime)
                            && Utils.isCorrectFormat(timeElapsed) && endTime.length() == 10) {
                        double total_cost = Double.parseDouble(totalCost);
                        double driver_payout = Double.parseDouble(driverPayout);
                        if(total_cost * 0.65 == driver_payout) {
                            if(!this.mongodb.findById_boolean(id)) {
                                res.put("status","NOT FOUND");
                                String response  = res.toString();
                                r.sendResponseHeaders(404,response.length());
                                OutputStream os = r.getResponseBody();
                                os.write(response.getBytes());
                                os.close();
                                return;
                            }
                            Document object = this.mongodb.findById(id);
                            String startTime = object.get("startTime").toString();
                            double start_time = Double.parseDouble(startTime);
                            double end_time = Double.parseDouble(endTime);
                            double time_elapsed = Utils.convertTime(timeElapsed);
                            if(time_elapsed != end_time - start_time || end_time < start_time) {
                                res.put("status","BAD REQUEST");
                                String response  = res.toString();
                                r.sendResponseHeaders(400,response.length());
                                OutputStream os = r.getResponseBody();
                                os.write(response.getBytes());
                                os.close();
                                return;
                            }
                            String passenger = object.get("passenger").toString();
                            String driver = object.get("driver").toString();
                            this.mongodb.updateDocument(id,distance,endTime,timeElapsed,discount,
                                    totalCost,driverPayout,startTime,passenger,driver);
                            res.put("status","OK");
                            String response  = res.toString();
                            r.sendResponseHeaders(200,response.length());
                            OutputStream os = r.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                        } else {
                            res.put("status","BAD REQUEST");
                            String response  = res.toString();
                            r.sendResponseHeaders(400,response.length());
                            OutputStream os = r.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                        }
                    } else {
                        res.put("status","BAD REQUEST");
                        String response  = res.toString();
                        r.sendResponseHeaders(400,response.length());
                        OutputStream os = r.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }
                } else {
                    res.put("status","BAD REQUEST");
                    String response  = res.toString();
                    r.sendResponseHeaders(400,response.length());
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            } else {
                res.put("status","BAD REQUEST");
                String response  = res.toString();
                r.sendResponseHeaders(400,response.length());
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
