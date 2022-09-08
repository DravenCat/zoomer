package ca.utoronto.utm.mcs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.JSONObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
   private String url = "jdbc:postgresql://localhost:5432/root";

   @BeforeEach
   public void setup() {
      try {
         Class.forName("org.postgresql.Driver");
         Connection connection = DriverManager.getConnection(url, "root", "123456");
         Statement stmt = connection.createStatement();
         String sql = "DELETE FROM users";
         stmt.executeUpdate(sql);

         String email = "2002@mail";
         String password = "4BA29B9F9E5732ED33761840F4BA6C53";
         String name = "2002";
         String sql_insert = "INSERT INTO users(prefer_name,email,password,rides,availableCoupons,redeemedCoupons)"
                 + "VALUES('" + name + "','" + email + "','" + password + "','0','{}','{}')";
         stmt.executeUpdate(sql_insert);
         connection.close();

         connection.close();
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   /**
    * Test for /user/register 200
    */
   @Test
   public void userRegister_200() {
      try {
         String uri = uriComponent.concat("/user/register");
         JSONObject req_body = new JSONObject();
         req_body.put("name","1001");
         req_body.put("email","1001@mail");
         req_body.put("password","1001");
         String req = req_body.toString();
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
                 .uri(URI.create(uri))
                 .POST(HttpRequest.BodyPublishers.ofString(req))
                 .build();

         HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(200,status);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Test for /user/register 400
    */
   @Test
   public void userRegister_400() {
      try {
         String uri = uriComponent.concat("/user/register");
         JSONObject req_body = new JSONObject();
         req_body.put("password","user1");
         String req = req_body.toString();
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
                 .uri(URI.create(uri))
                 .POST(HttpRequest.BodyPublishers.ofString(req))
                 .build();

         HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(400,status);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Test for /user/login 200
    */
   @Test
   public void userLogin_200() {
      try {
         String uri = uriComponent.concat("/user/login");
         JSONObject req_body = new JSONObject();
         req_body.put("email","2002@mail");
         req_body.put("password","2002");
         String req = req_body.toString();
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
                 .uri(URI.create(uri))
                 .POST(HttpRequest.BodyPublishers.ofString(req))
                 .build();

         HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(200,status);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Test for /user/login 400
    */
   @Test
   public void userLogin_400() {
      try {
         String uri = uriComponent.concat("/user/login");
         JSONObject req_body = new JSONObject();
         req_body.put("email","user1");
         String req = req_body.toString();
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
                 .uri(URI.create(uri))
                 .POST(HttpRequest.BodyPublishers.ofString(req))
                 .build();

         HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
         int status = response.statusCode();
         assertEquals(400,status);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   @AfterEach
   public void cleanUP() {
      try {
         Class.forName("org.postgresql.Driver");
         Connection connection = DriverManager.getConnection(url, "root", "123456");
         Statement stmt = connection.createStatement();
         String sql = "DELETE FROM users";
         stmt.executeUpdate(sql);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}