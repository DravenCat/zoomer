package ca.utoronto.utm.mcs;

import static org.junit.jupiter.api.Assertions.*;

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

/*
Please Write Your Tests For CI/CD In This Class. 
You will see these tests pass/fail on github under github actions.
*/
public class AppTest {

   public static String uriComponent = "http://localhost:8004";

   public static String uriDb = "bolt://localhost:7687";
   public static Driver driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j", "123456"));


   @BeforeAll
   public static void setDatabase() {
      String uriDb = "bolt://localhost:7687";
      Driver driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j", "123456"));
      try (Session session = driver.session()){
         String resetQuery = "MATCH (n) DETACH DELETE n";
         session.run(resetQuery);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   @BeforeEach
   public void initDatabase() {
      try (Session session = this.driver.session()){
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
    * Test for the case that nearbyDriver method return status 200
    */
   @Test
   public void nearbyDriver200Test() {
      try {
         HttpClient client = HttpClient.newHttpClient();

         String uri = uriComponent + "/location/nearbyDriver/1001?radius=150";
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
    * Test for the case that nearbyDriver method return status 400
    */
   @Test
   public void nearbyDriver400Test() {
      try {
         HttpClient client = HttpClient.newHttpClient();

         String uri = uriComponent + "/location/nearbyDriver/?radius=150";
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                 .method("GET", HttpRequest.BodyPublishers.noBody()).build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(400, status);
      } catch (IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   /**
    * Test for the case that navigation method return status 200
    */
   @Test
   public void navigation200Test() {
      try {
         HttpClient client = HttpClient.newHttpClient();

         String uri = uriComponent + "/location/navigation/2001?passengerUid=1001";

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
    * Test for the case that navigation method return status 400
    */
   @Test
   public void navigation400Test() {
      try {
         HttpClient client = HttpClient.newHttpClient();
         String uri = uriComponent + "/location/navigation/2001";
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                 .method("GET", HttpRequest.BodyPublishers.noBody()).build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(400, status);
      } catch (IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   /**
    * Test for the case that navigation method return status 404
    */
   @Test
   public void navigation404Test() {
      try {
         HttpClient client = HttpClient.newHttpClient();

         String uri = uriComponent + "/location/navigation/2003?passengerUid=1002";
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
      try (Session session = this.driver.session()){
         String resetQuery = "MATCH (n) DETACH DELETE n";
         session.run(resetQuery);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
