package ca.utoronto.utm.mcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class Utils {
   public static String convert(InputStream inputStream) throws IOException {

      try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
         return br.lines().collect(Collectors.joining(System.lineSeparator()));
      }
   }

   /**
    * Check contains only digit
    * @param str String
    * @return boolean
    */
   public static boolean isNumeric(String str) {
      for (int i = 0; i < str.length(); i++) {
         if (!Character.isDigit(str.charAt(i))) {
            if (str.charAt(i) != '.') {
               return false;
            }
         }
      }
      return true;
   }

   /**
    * Check is the format of HH:MM:SS
    * @param time String
    * @return boolean
    */
   public static boolean isCorrectFormat(String time) {
      if (time.length() == 8) {
         for (int i = 0; i < time.length(); i++) {
            if (i == 2 || i == 5){
               if (time.charAt(i) != ':') {
                  return false;
               }
            } else {
               if (!Character.isDigit(time.charAt(i))) {
                  return false;
               }
            }

         }
         return true;
      } else {
         return false;
      }
   }

   /**
    * Converting HH:MM:SS to double
    * @param time String
    * @return double
    */
   public static double convertTime(String time) {
      String hour = time.substring(0,2);
      String min = time.substring(3,5);
      String sec = time.substring(6);

      double h = Double.parseDouble(hour) * 60 * 60;
      double m = Double.parseDouble(min) * 60;
      double s = Double.parseDouble(sec);

      return h + m + s;
   }
}
