package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.*;
import org.neo4j.driver.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

import static org.neo4j.driver.Values.parameters;

public class navigation implements HttpHandler{


    @Override
    public void handle(HttpExchange r) throws IOException {
        try {
            if (r.getRequestMethod().equals("GET")) {
                navigate(r);
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
    private void navigate(HttpExchange r) throws IOException, JSONException {
        int statusCode = 400;
        String body = Utils.convert(r.getRequestBody());
        JSONObject res = new JSONObject();
        String reqURIString = r.getRequestURI().toString();
        String[] uriSplitter = reqURIString.split("/");
        if (uriSplitter.length != 4) {
            res.put("status", "BAD REQUEST");
            String response = res.toString();
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

        String uri = "http://localhost:5000".concat(reqURIString);
        URL url = new URL(uri);
        String urlQuery = url.getQuery();
        String rest = uriSplitter[3];
        String driverUid = rest.substring(0, rest.indexOf("?"));

        if (urlQuery == null || !rest.contains("?passengerUid=") || rest.contains("&") || driverUid.equals("")) {
            res.put("status", "BAD REQUEST");
            String response = res.toString();
            r.sendResponseHeaders(statusCode, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }
        String passengerUid = urlQuery.substring(urlQuery.indexOf("=") + 1, urlQuery.length());

        if (!passengerUid.equals("")) {
            try (Session session = Utils.driver.session()) {
                //Check if driver and passenger exist and get their street
                String driverRoad = getUserStreet(driverUid);
                String passengerRoad  = getUserStreet(passengerUid);

                if (driverRoad != null && passengerRoad != null) {
                    String navigationQuery = "MATCH (from:road {name:$x}), (to:road {name:$y}), " +
                            "paths = allShortestPaths((from)-[:ROUTE_TO*]->(to)) " +
                            "WITH REDUCE(time = 0, route in relationships(paths) | time + route.travel_time) " +
                            "AS total_time,paths "+
                            "RETURN paths, total_time " +
                            "ORDER BY total_time ASC " +
                            "LIMIT 1";
                    Result result = session.run(
                            navigationQuery, parameters("x", driverRoad, "y", passengerRoad));
                    //chech if the route exists
                    if (!result.hasNext()) {
                        statusCode = 404;
                        JSONObject data = new JSONObject();
                        data.put("status", "NOT FOUND Road");
                        String response = data.toString();
                        r.sendResponseHeaders(statusCode, response.length());
                        OutputStream os = r.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                        return;
                    }

                    Record record = result.next();
                    Path path = record.get("paths").asPath();
                    int totalTime = record.get("total_time").asInt();
                    Iterator<Relationship> relationships = path.relationships().iterator();
                    Iterator<Node> nodes = path.nodes().iterator();
                    Node from = nodes.next();
                    JSONObject fromObject = new JSONObject();
                    fromObject.put("street", from.get("name").asString());
                    fromObject.put("time", 0);
                    fromObject.put("has_traffic", from.get("is_traffic").asBoolean());

                    List<JSONObject> route = new ArrayList<>();
                    route.add(fromObject);
                    while (relationships.hasNext()) {
                        Relationship routeTo = relationships.next();
                        Node road = nodes.next();
                        JSONObject roadObject = new JSONObject();
                        roadObject.put("street", road.get("name").asString());
                        roadObject.put("time", routeTo.get("travel_time").asInt());
                        roadObject.put("has_traffic", road.get("is_traffic").asBoolean());
                        route.add(roadObject);
                    }

                    JSONObject data = new JSONObject();
                    data.put("total_time", totalTime);
                    data.put("route", route);
                    statusCode = 200;
                    res.put("status", "OK");
                    res.put("data", data);
                    String response = res.toString();
                    r.sendResponseHeaders(statusCode, response.length());
                    OutputStream os = r.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else {
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

        } else {
            res.put("status", "BAD REQUEST");
            String response = res.toString();
            r.sendResponseHeaders(statusCode, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    /**
     * Returen the street that user at. Null if the user does not exist
     * @param userUid the uid of user
     * @return a string contains the name of the street
     * @throws Exception any exception occurs
     */
    private String getUserStreet (String userUid) throws Exception {
        try (Session session = Utils.driver.session()) {
            String userCheck = "MATCH (n) where n.uid=" + "'" + userUid + "' RETURN n.street_at";
            Result userCheckResult = session.run(userCheck);
            if (userCheckResult.hasNext()){
                return userCheckResult.next().get("n.street_at").asString();
            }
            return null;
        }
    }
}
