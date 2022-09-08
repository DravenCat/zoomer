package ca.utoronto.utm.mcs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/*
Please Write Your Tests For CI/CD In This Class.
You will see these tests pass/fail on github under github actions.
*/
public class AppTest {

   public static String uriComponent = "http://localhost:8004";

   private String sqlUrl = "jdbc:postgresql://localhost:5432/root";
   public static String uriDb = "bolt://localhost:7687";
   public static Driver driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j", "123456"));

   private MongoClient mongoClient;
   private MongoDatabase db;
   private MongoCollection<Document> col;

   @BeforeAll
   public static void setDatabase() {
      try {
         // Postgres
         String sql_url = "jdbc:postgresql://localhost:5432/root";
         Class.forName("org.postgresql.Driver");
         Connection connection = DriverManager.getConnection(sql_url, "root", "123456");
         Statement stmt = connection.createStatement();
         String sql_delete = "DELETE FROM users";
         stmt.executeUpdate(sql_delete);
         connection.close();

         // MongoDB
         MongoClient mongoClient =
                 new MongoClient(new MongoClientURI("mongodb://root:123456@localhost:27017/trip?authSource=admin"));
         DB db = mongoClient.getDB("trip");
         DBCollection col = db.getCollection("trips");
         DBCursor cursor = col.find();
         while(cursor.hasNext()){
            DBObject object = cursor.next();
            col.remove(object);
         }

         //Neo4j
         String uri_Db = "bolt://localhost:7687";
         Driver driver = GraphDatabase.driver(uri_Db, AuthTokens.basic("neo4j", "123456"));
         Session session = driver.session();
         String resetQuery = "MATCH (n) DETACH DELETE n";
         session.run(resetQuery);
         session.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   @BeforeEach
   public void initDatabase() {
      try {
         // mongoDB
         this.mongoClient =
                 new MongoClient(new MongoClientURI("mongodb://root:123456@localhost:27017/trip?authSource=admin"));
         this.db = this.mongoClient.getDatabase("trip");
         this.col = this.db.getCollection("trips");


         //Neo4j
         Session session = driver.session();
         String setupQuery =
                 "CREATE (:user {uid:'1001', is_driver:false, longitude: 75, latitude: 45, street_at: 'Wuyi Road'}) " +
                         "CREATE (:user {uid:'1002', is_driver:false, longitude: 76, latitude: 46, street_at: 'Zunyi Road'}) " +
                         "CREATE (:user {uid:'2001', is_driver:true, longitude: 76, latitude: 46, street_at: 'Zunyi Road'}) " +
                         "CREATE (:user {uid:'2002', is_driver:true, longitude: 74, latitude: 44, street_at: 'Panyu Road'}) " +
                         "CREATE (:user {uid:'2003', is_driver:true, longitude: 1, latitude: 1, street_at: 'Dingxi Road'}) " +
                         "CREATE (:user {uid:'2004', is_driver:true, longitude: 2, latitude: 3, street_at: 'Kaixuan Road'}) " +
                         "CREATE (wy:road {name: 'Wuyi Road', is_traffic: false}) " +
                         "CREATE (zy:road {name: 'Zunyi Road', is_traffic: false}) " +
                         "CREATE (kx:road {name: 'Kaixuan Road', is_traffic: true}) " +
                         "CREATE (dx:road {name: 'Dingxi Road', is_traffic: false}) " +
                         "CREATE (py:road {name: 'Panyu Road', is_traffic: true}) " +
                         "CREATE (zy)-[:ROUTE_TO {travel_time: 3, is_traffic: false}]->(wy) " +
                         "CREATE (zy)-[:ROUTE_TO {travel_time: 6, is_traffic: true}]->(py) " +
                         "CREATE (py)-[:ROUTE_TO {travel_time: 6, is_traffic: true}]->(kx) " +
                         "CREATE (py)-[:ROUTE_TO {travel_time: 1, is_traffic: true}]->(dx) " +
                         "CREATE (kx)-[:ROUTE_TO {travel_time: 6, is_traffic: true}]->(wy) " +
                         "CREATE (dx)-[:ROUTE_TO {travel_time: 8, is_traffic: false}]->(wy) ";
         session.run(setupQuery);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Test for /trip/request 200
    */
   @Test
   public void request200Test() {
      try {
         HttpClient client = HttpClient.newHttpClient();

         String uri = uriComponent + "/trip/request";
         JSONObject reqBody = new JSONObject();
         reqBody.put("uid", "1001");
         reqBody.put("radius", 150);
         String req = reqBody.toString();
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                 .method("POST", HttpRequest.BodyPublishers.ofString(req)).build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         System.out.println(response.body().toString());
         assertEquals(200, status);
      } catch (JSONException | IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   /**
    * Test for /trip/request 400
    */
   @Test
   public void request400Test() {
      try {
         HttpClient client = HttpClient.newHttpClient();

         String uri = uriComponent + "/trip/request";
         JSONObject reqBody = new JSONObject();
         reqBody.put("uid", "1001");
         reqBody.put("radius", -10);
         String req = reqBody.toString();
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                 .method("POST", HttpRequest.BodyPublishers.ofString(req)).build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(400, status);
      } catch (JSONException | IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   /**
    * Test for /trip/confirm 200
    */
   @Test
   public void confirm200Test() {
      try {
         HttpClient client = HttpClient.newHttpClient();

         String uri = uriComponent + "/trip/confirm";
         JSONObject reqBody = new JSONObject();
         reqBody.put("driver", "2002");
         reqBody.put("passenger", "1001");
         reqBody.put("startTime", "1622584800");
         String req = reqBody.toString();

         HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                 .method("POST", HttpRequest.BodyPublishers.ofString(req)).build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(200, status);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Test for /trip/confirm 400
    */
   @Test
   public void confirm400Test() {
      try {
         HttpClient client = HttpClient.newHttpClient();

         String uri = uriComponent + "/trip/confirm";
         JSONObject reqBody = new JSONObject();
         reqBody.put("driver", "2002");
         reqBody.put("startTime", 1622584800);
         String req = reqBody.toString();
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                 .method("POST", HttpRequest.BodyPublishers.ofString(req)).build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(400, status);
      } catch (JSONException | IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   /**
    * Test for /trip/:_id 200
    */
   @Test
   public void patchId200Test() {
      try {
         Document doc_1 = new Document("driver","2001")
                 .append("passenger","1001")
                 .append("startTime","1622584800")
                 .append("distance","")
                 .append("totalCost","")
                 .append("endTime","")
                 .append("timeElapsed","")
                 .append("driverPayout","")
                 .append("discount","");
         this.col.insertOne(doc_1);
         String id = doc_1.getObjectId("_id").toString();

         HttpClient client = HttpClient.newHttpClient();

         String uri = uriComponent + "/trip/" + id;
         JSONObject reqBody = new JSONObject();
         reqBody.put("distance", "20");
         reqBody.put("endTime", 1622586600);
         reqBody.put("timeElapsed", "00:30:00");
         reqBody.put("discount", 0.8);
         reqBody.put("totalCost", 30);
         reqBody.put("driverPayout", 19.5);
         String req = reqBody.toString();
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                 .method("PATCH", HttpRequest.BodyPublishers.ofString(req)).build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(200, status);
      } catch (JSONException | IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   /**
    * Test for /trip/:_id 400
    */
   @Test
   public void patchId400Test(){
      try {
         HttpClient client = HttpClient.newHttpClient();

         String uri = uriComponent + "/trip/5349b4ddd2781d08c09890f4";
         JSONObject reqBody = new JSONObject();
         reqBody.put("endTime", 1622586600);
         reqBody.put("timeElapsed", 0);
         reqBody.put("discount", 0.8);
         reqBody.put("totalCost", 30);
         reqBody.put("driverPayout", 19.5);
         String req = reqBody.toString();
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                 .method("PATCH", HttpRequest.BodyPublishers.ofString(req)).build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(400, status);
      } catch (JSONException | IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   /**
    *  Test for /trip/passenger/:uid 200
    */
   @Test
   public void getPassengerTrips200Test(){
      try {
         Document doc_1 = new Document("driver","2001")
                 .append("passenger","1001")
                 .append("startTime","1622584800")
                 .append("distance","")
                 .append("totalCost","")
                 .append("endTime","")
                 .append("timeElapsed","")
                 .append("driverPayout","")
                 .append("discount","");
         this.col.insertOne(doc_1);

         HttpClient client = HttpClient.newHttpClient();

         String uri = uriComponent + "/trip/passenger/1001";
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                 .method("GET", HttpRequest.BodyPublishers.noBody()).build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(200, status);
      } catch (IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   /**
    *  Test for /trip/passenger/:uid 404
    */
   @Test
   public void getPassengerTrips404Test(){
      try {
         HttpClient client = HttpClient.newHttpClient();

         String uri = uriComponent + "/trip/passenger/1009";
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                 .method("GET", HttpRequest.BodyPublishers.noBody()).build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(404, status);
      } catch (IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   /**
    *  Test for /trip/driver/:uid 200
    */
   @Test
   public void getDriverTrips200Test(){
      try {
         Document doc_1 = new Document("driver","2001")
                 .append("passenger","1001")
                 .append("startTime","1622584800")
                 .append("distance","")
                 .append("totalCost","")
                 .append("endTime","")
                 .append("timeElapsed","")
                 .append("driverPayout","")
                 .append("discount","");
         this.col.insertOne(doc_1);

         HttpClient client = HttpClient.newHttpClient();

         String uri = uriComponent + "/trip/driver/2001";
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                 .method("GET", HttpRequest.BodyPublishers.noBody()).build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(200, status);
      } catch (IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   /**
    *  Test for /trip/driver/:uid 404
    */
   @Test
   public void getDriverTrips404Test(){
      try {
         HttpClient client = HttpClient.newHttpClient();

         String uri = uriComponent + "/trip/driver/2009";
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                 .method("GET", HttpRequest.BodyPublishers.noBody()).build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(404, status);
      } catch (IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   /**
    *  Test for /trip/driverTime/:_id 200
    */
   @Test
   public void getDriverTime200Test(){
      try {
         Document doc_1 = new Document("driver","2001")
                 .append("passenger","1001")
                 .append("startTime","1622584800")
                 .append("distance","")
                 .append("totalCost","")
                 .append("endTime","")
                 .append("timeElapsed","")
                 .append("driverPayout","")
                 .append("discount","");
         this.col.insertOne(doc_1);
         String id = doc_1.getObjectId("_id").toString();

         HttpClient client = HttpClient.newHttpClient();

         String uri = uriComponent + "/trip/driverTime/" + id;
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                 .method("GET", HttpRequest.BodyPublishers.noBody()).build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(200, status);
      } catch (IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   /**
    *  Test for /trip/driverTime/:_id 404
    */
   @Test
   public void getDriverTime404Test(){
      try {
         HttpClient client = HttpClient.newHttpClient();

         String uri = uriComponent + "/trip/driverTime/5349b4ddd2781d08c09890f4";
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                 .method("GET", HttpRequest.BodyPublishers.noBody()).build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(404, status);
      } catch (IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   @AfterEach
   public void resetDatabase() {
      //clear the collection
      try {
         // Postgres
         String sql_url = "jdbc:postgresql://localhost:5432/root";
         Class.forName("org.postgresql.Driver");
         Connection connection = DriverManager.getConnection(sql_url, "root", "123456");
         Statement stmt = connection.createStatement();
         String sql_delete = "DELETE FROM users";
         stmt.executeUpdate(sql_delete);
         connection.close();

         //Neo4j
         String uri_Db = "bolt://localhost:7687";
         Driver driver = GraphDatabase.driver(uri_Db, AuthTokens.basic("neo4j", "123456"));
         Session session = driver.session();
         String resetQuery = "MATCH (n) DETACH DELETE n";
         session.run(resetQuery);
         session.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
