package ca.utoronto.utm.mcs;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class App {
   static int PORT = 8000;

   public static void main(String[] args) throws Exception {
      HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
      Mongodb mongodb = new Mongodb();
      server.createContext("/trip", new trip(mongodb));
      server.createContext("/trip/passenger", new passengerTrips(mongodb));
      server.createContext("/trip/driver", new driverTrips(mongodb));
      server.createContext("/trip/driverTime", new DriverTime(mongodb));
      server.start();
      System.out.printf("Server started on port %d...\n", PORT);
   }
}
