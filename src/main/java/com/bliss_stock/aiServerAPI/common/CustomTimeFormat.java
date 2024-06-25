package com.bliss_stock.aiServerAPI.common;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class CustomTimeFormat {
   private DateTimeFormatter formatObj;
   
   public CustomTimeFormat(DateTimeFormatter obj) {
      this.formatObj = obj;
   }
   
   public String nowRec() {
      Instant nowUtc = Instant.now();
      return ZonedDateTime.ofInstant(nowUtc, ZoneId.of("Asia/Yangon")).format(this.formatObj);
   }
   
   public static String customStaticTime(DateTimeFormatter customFormat) {
      Instant nowUtc = Instant.now();
      System.gc();
      Runtime.getRuntime().gc();
      return ZonedDateTime.ofInstant(nowUtc, ZoneId.of("Asia/Yangon")).format(customFormat) + "MMT";
   }
   
   public String biZonedTime() {
      Instant nowUtc = Instant.now();
      String nowMmt = ZonedDateTime.ofInstant(nowUtc, ZoneId.of("Asia/Yangon")).format(this.formatObj);
      String nowJst = ZonedDateTime.ofInstant(nowUtc, ZoneId.of("Asia/Tokyo")).format(this.formatObj);
      return nowMmt + " MMT" + "(" + nowJst + " JST)";
   }
   
}
