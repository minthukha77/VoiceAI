package com.bliss_stock.aiServerAPI.common;

// The default Logger class doesn't seem to produce correct UTF-8 characters.

import java.io.*;
import java.nio.charset.StandardCharsets;

public class CustomLogger {
   private File file;
   
   public CustomLogger(File file){
      this.file = file;
      System.out.println("Writing in " + file);
      info("log file: " + file.getAbsolutePath());
   }
   public void info(String text){
      try (FileOutputStream fos = new FileOutputStream(file, true);
           OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
           BufferedWriter writer = new BufferedWriter(osw);) {

         writer.append(text).append("\n");

      } catch (IOException e) {
         e.printStackTrace();
      }

   }
}
