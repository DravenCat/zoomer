package ca.utoronto.utm.mcs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.sql.*;
import java.util.Iterator;

public class User implements HttpHandler {
   public Connection connection;

   public User() throws ClassNotFoundException, SQLException {
      String url = "jdbc:postgresql://postgres:5432/root";
      Class.forName("org.postgresql.Driver");
      this.connection = DriverManager.getConnection(url, "root", "123456");
   }


   public static boolean isNumeric(String str) {
      try {
         Double.parseDouble(str);
         return true;
      } catch (NumberFormatException e) {
         return false;
      }
   }

   /**
    * Hashing password using MD5
    *
    * @param s initial password
    * @return String hashed password
    */
   private String MD5(String s) {
      char hexDigits[]={'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
      try {
         byte[] btInput = s.getBytes();
         MessageDigest mdInst = MessageDigest.getInstance("MD5");
         mdInst.update(btInput);
         byte[] md = mdInst.digest();
         int j = md.length;
         char str[] = new char[j * 2];
         int k = 0;
         for (int i = 0; i < j; i++) {
            byte byte0 = md[i];
            str[k++] = hexDigits[byte0 >>> 4 & 0xf];
            str[k++] = hexDigits[byte0 & 0xf];
         }
         return new String(str);
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }


   @Override
   public void handle(HttpExchange r) throws IOException {
      try {
         if (r.getRequestMethod().equals("GET")) {
            handleGET(r);
         }
         if (r.getRequestMethod().equals("PATCH")) {
            handlePATCH(r);
         }
         if (r.getRequestMethod().equals("POST")) {
            this.handlePOST(r);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Handling POST request (/user/register and /user/login)
    * @param r HttpExchange r
    */
   private void handlePOST(HttpExchange r) throws JSONException, IOException {
      String path = r.getRequestURI().getPath();
      if(path.equals("/user/register")) {
         this.userRegister(r);
      }
      if (path.equals("/user/login")) {
         this.userLogin(r);
      }
   }

   /**
    * POST /user/register route
    *
    * @param r HttpExchange
    * @throws IOException IOException
    * @throws JSONException JSONException
    */
   private void userRegister(HttpExchange r) throws IOException, JSONException {
      String body = Utils.convert(r.getRequestBody());
      JSONObject req = new JSONObject(body);
      JSONObject res = new JSONObject();
      if(req.has("name") && req.has("email") && req.has("password")) {
         try {
            String email = req.getString("email");
            String password = req.getString("password");
            String name = req.getString("name");
            String prepare_1 = "SELECT * FROM users WHERE email='" + email
                    + "' AND password='" + this.MD5(password) + "'";
            PreparedStatement ps_1 = this.connection.prepareStatement(prepare_1);
            ResultSet rs_1 = ps_1.executeQuery();
            if(rs_1.next()) {
               // 403
               res.put("status","FORBIDDEN");
               String response = res.toString();
               r.sendResponseHeaders(403, response.length());
               OutputStream os = r.getResponseBody();
               os.write(response.getBytes());
               os.close();
            }
            else {
               String prepare_2 = "INSERT INTO users(prefer_name,email,password,rides,availableCoupons,redeemedCoupons)"
                       + "VALUES('" + name + "','" + email + "','" + this.MD5(password) + "','0','{}','{}')";
               PreparedStatement ps_2 = this.connection.prepareStatement(prepare_2);
               int count = ps_2.executeUpdate();
               if(count>0){
                  // 200
                  res.put("status", "OK");
                  String response = res.toString();
                  r.sendResponseHeaders(200, response.length());
                  OutputStream os = r.getResponseBody();
                  os.write(response.getBytes());
                  os.close();
               } else {
                  // 500
                  res.put("status", "INTERNAL SERVER ERROR");
                  String response = res.toString();
                  r.sendResponseHeaders(500, response.length());
                  OutputStream os = r.getResponseBody();
                  os.write(response.getBytes());
                  os.close();
               }
            }
         } catch (Exception e) {
            // 500
            e.printStackTrace();
            res.put("status", "INTERNAL SERVER ERROR");
            String response = res.toString();
            r.sendResponseHeaders(500, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
         }
      }
      else {
         // 400
         res.put("status", "BAD REQUEST");
         String response = res.toString();
         r.sendResponseHeaders(400, response.length());
         OutputStream os = r.getResponseBody();
         os.write(response.getBytes());
         os.close();
      }
   }

   /**
    * POST /user/login route
    * @param r HttpExchange
    * @throws IOException IOException
    * @throws JSONException JSONException
    */
   private void userLogin(HttpExchange r) throws IOException, JSONException {
      String body = Utils.convert(r.getRequestBody());
      JSONObject req = new JSONObject(body);
      JSONObject res = new JSONObject();
      if(req.has("email") && req.has("password")) {
         try {
            String email = req.getString("email");
            String password = req.getString("password");
            String prepare = "SELECT * FROM users WHERE email='" + email
                    + "' AND password='" + this.MD5(password) + "'";
            PreparedStatement ps = this.connection.prepareStatement(prepare);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
               // 200
               res.put("status", "OK");
               String response = res.toString();
               r.sendResponseHeaders(200, response.length());
               OutputStream os = r.getResponseBody();
               os.write(response.getBytes());
               os.close();
            }
            else {
               // 404
               res.put("status", "NOT FOUND");
               String response = res.toString();
               r.sendResponseHeaders(404, response.length());
               OutputStream os = r.getResponseBody();
               os.write(response.getBytes());
               os.close();
            }
         } catch (Exception e) {
            // 500
            res.put("status", "INTERNAL SERVER ERROR");
            String response = res.toString();
            r.sendResponseHeaders(500, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
         }
      }
      else {
         // 400
         res.put("status", "BAD REQUEST");
         String response = res.toString();
         r.sendResponseHeaders(400, response.length());
         OutputStream os = r.getResponseBody();
         os.write(response.getBytes());
         os.close();
      }
   }


   public void handleGET(HttpExchange r) throws IOException, JSONException, SQLException {
      String[] url = r.getRequestURI().getPath().split("/");
      System.out.println(url[url.length - 1]);
      if (isNumeric(url[url.length - 1])) {
         try {
            ResultSet rs;
            int uid = Integer.parseInt(url[url.length - 1]);
            String prepare = "SELECT prefer_name as name, email, rides, isdriver,availableCoupons, redeemedCoupons FROM users WHERE uid = ?";
            PreparedStatement ps = this.connection.prepareStatement(prepare);
            ps.setInt(1, uid);
            rs = ps.executeQuery();
            if (rs.next()) {
               JSONObject var = new JSONObject();
               String name = rs.getString("name");
               String email = rs.getString("email");
               String rides = rs.getString("rides");
               Boolean isDriver = rs.getBoolean("isdriver");
               Array availableCoupons = rs.getArray("availableCoupons");
               Array redeemedCoupons = rs.getArray("redeemedCoupons");
               var.put("name", name);
               var.put("email", email);
               var.put("rides", rides);
               var.put("is_driver", isDriver);
               var.put("availableCoupons", availableCoupons.toString());
               var.put("redeemedCoupons", redeemedCoupons.toString());
               String response = var.toString();
               r.sendResponseHeaders(200, response.length());
               // Writing response body
               OutputStream os = r.getResponseBody();
               os.write(response.getBytes());
               os.close();
            } else {
               JSONObject res = new JSONObject();
               res.put("status", "NOT FOUND");
               String response = res.toString();
               r.sendResponseHeaders(404, response.length());
               // Writing response body
               OutputStream os = r.getResponseBody();
               os.write(response.getBytes());
               os.close();
            }

         } catch (SQLException se) {
            se.printStackTrace();
            JSONObject res = new JSONObject();
            res.put("status", "INTERNAL SERVER ERROR");
            String response = res.toString();
            r.sendResponseHeaders(500, response.length());
            // Writing response body
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
         }
      }

   }

   public void handlePATCH(HttpExchange r) throws IOException, JSONException, SQLException {
      String[] url = r.getRequestURI().getPath().split("/");
      if (isNumeric(url[url.length - 1])) {
         ResultSet rs;
         int uid = Integer.parseInt(url[url.length - 1]);
         String preCheck = "SELECT count(*) as c FROM users WHERE uid = ?";
         PreparedStatement ps = this.connection.prepareStatement(preCheck);
         ps.setInt(1, uid);
         try {
            rs = ps.executeQuery();
            if (rs.next()) {
               int numOfUser = rs.getInt("c");
               String alters = "";
               if (numOfUser == 1) {
                  // Packing the alter info
                  String body = Utils.convert(r.getRequestBody());
                  JSONObject deserialized = new JSONObject(body);
                  Iterator<?> it = deserialized.keys();
                  String[] alternates = new String[deserialized.length()];
                  int order = 0;
                  System.out.println(deserialized.length());
                  while (it.hasNext()) {
                     System.out.println(order);
                     String key = it.next().toString();
                     if (order != 0 && order < deserialized.length()) {
                        alters += ", ";
                     }
                     if (key.equals("is_driver")) {
                        alternates[order] = "is_driver";
                        alters += "isdriver = ? ";
                     } else if (key.equals("name")) {
                        alternates[order] = "name";
                        alters += "prefer_name = ? ";
                     } else {
                        alternates[order] = key;
                        alters += key + " = ? ";
                     }
                     order++;
                  }
                  String alternation = "UPDATE users SET " + alters + " WHERE uid = ?";
                  PreparedStatement ps1 = this.connection.prepareStatement(alternation);
                  for (int j = 0; j < deserialized.length(); j++) {
                     System.out.println(alternates[j]);
                     System.out.print(j);
                     if (alternates[j].equals("is_driver")) {
                        System.out.println(deserialized.getBoolean(alternates[j]));
                        ps1.setBoolean(j + 1, deserialized.getBoolean(alternates[j]));
                     } else if (alternates[j].equals("rides")) {
                        ps1.setInt(j + 1, deserialized.getInt(alternates[j]));
                     } else {
                        ps1.setString(j + 1, deserialized.getString(alternates[j]));
                     }
                  }
                  ps1.setInt(deserialized.length() + 1, uid);
                  try {
                     ps1.executeUpdate();
                     // success
                     JSONObject res = new JSONObject();
                     res.put("status", "OK");
                     String response = res.toString();
                     r.sendResponseHeaders(200, response.length());
                     // Writing response body
                     OutputStream os = r.getResponseBody();
                     os.write(response.getBytes());
                     os.close();
                  } catch (SQLException e) {
                     e.printStackTrace();
                     JSONObject res = new JSONObject();
                     res.put("status", "INTERNAL SERVER ERROR");
                     String response = res.toString();
                     r.sendResponseHeaders(500, response.length());
                     // Writing response body
                     OutputStream os = r.getResponseBody();
                     os.write(response.getBytes());
                     os.close();
                  }
               }
            } else {
               JSONObject res = new JSONObject();
               res.put("status", "NOT FOUND");
               String response = res.toString();
               r.sendResponseHeaders(404, response.length());
               // Writing response body
               OutputStream os = r.getResponseBody();
               os.write(response.getBytes());
               os.close();
            }
         } catch (SQLException e) {
            e.printStackTrace();
            JSONObject res = new JSONObject();
            res.put("status", "INTERNAL SERVER ERROR");
            String response = res.toString();
            r.sendResponseHeaders(500, response.length());
            // Writing response body
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
         }
      } else {
         JSONObject res = new JSONObject();
         res.put("status", "BAD REQUEST");
         String response = res.toString();
         r.sendResponseHeaders(400, response.length());
         // Writing response body
         OutputStream os = r.getResponseBody();
         os.write(response.getBytes());
         os.close();
      }

   }
}
