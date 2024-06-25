package com.bliss_stock.aiServerAPI.common;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.function.Function;
public class AutoLogger {
   // Variables according to the Voitra Projects' needs.
   public static String bars = "============";
   public static Function<String, String> filler = (type) -> " :: " + type + " :: ";
   public static CustomTimeFormat lineStart = new com.bliss_stock.aiServerAPI.common.CustomTimeFormat(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS"));
   public static CustomTimeFormat readableJiKan = new com.bliss_stock.aiServerAPI.common.CustomTimeFormat(DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy"));
   
   private int threadNum;
   private String audioId;
   private Log logObj;
   private String fileSize;
   private Instant startTime;
   private Instant endTime;
   
   public AutoLogger(int threadNum, String audioId, Log logObj, String fileSize) {
      this.threadNum = threadNum;
      this.audioId = audioId;
      this.logObj = logObj;
      this.fileSize = fileSize;
   }

   public void logMsg(String msg){

      System.out.println("tmp:::" + msg);
      this.logObj.logger.info(
         "tmp:::" + msg
      );
   }
   
   
   public String dateTimeDEBUG(boolean withThreadNum) {
      String pre = withThreadNum ? "T" + this.threadNum + "_ " : "";
      return lineStart.nowRec() + filler.apply("DEBUG") + pre;
   }
   
   public void logStart() {
      logHorLines(" START ");
      startTime = Instant.now();
      this.logObj.logger.info(
         dateTimeDEBUG(true)
            + "開始時間: " + readableJiKan.nowRec()
      );
      this.logObj.logger.info(
         dateTimeDEBUG(true)
            + "ファイルサイズ: " + this.fileSize
      );
   }
   
   public void logHorLines(String startOrEnd) {
      this.logObj.logger.info(
         dateTimeDEBUG(true)
            + bars
            + startOrEnd + this.threadNum
            + " audio_id: " + this.audioId + " "
            + bars
      );
   }
   
   public void gcpCompleted() {
      System.out.println("audio" + this.audioId + " GCP completed.");
      endTime = Instant.now();
      this.logObj.logger.info(
         dateTimeDEBUG(true)
            + "GCP Completed at "
            + readableJiKan.nowRec()
      );
      
   }
   
   public void transcriptionFailed() {
      endTime = Instant.now();
      this.logObj.logger.info(
         dateTimeDEBUG(true)
            + "Transcription Failed at "
            + readableJiKan.nowRec()
      );
      this.logObj.logger.info(
         dateTimeDEBUG(true)
            + "終了時間: " + readableJiKan.nowRec()
      );
      this.logObj.logger.info(
         dateTimeDEBUG(true)
            + "処理時間: "
            + formatMillis(ChronoUnit.MILLIS.between(startTime, endTime))
      
      );
   }
   
   public void logEnd(String result, String returnStatus) {
      this.logObj.logger.info(
         dateTimeDEBUG(true)
            + "終了時間: " + readableJiKan.nowRec()
      );
      this.logObj.logger.info(
         dateTimeDEBUG(true)
            + "処理時間: "
            + formatMillis(ChronoUnit.MILLIS.between(startTime,endTime))
      );
      this.logObj.logger.info(
         dateTimeDEBUG(true)
            + "AUDIO_ID: " + audioId + "_" + result
//            + "認識結果(UTF-8で)"
      );
      this.logObj.logger.info(
         dateTimeDEBUG(true)
            + "AUDIO_ID: " + audioId + "_"
            + "WEBHOOK_RETURN_STATUS: " + returnStatus
      );
      logHorLines(" END ");
   }
   
   public void logMemoryDetails () {
      this.logObj.logger.info(
         dateTimeDEBUG(true)
            + "Memory Status:"
      );
      this.logObj.logger.info(
         (new Memory().memInfo(dateTimeDEBUG(true)))
      );
   }
   
   public void logMemory () {
      
      long allocatedBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      long allocatedKiloBytes = allocatedBytes / 1024;
      
      this.logObj.logger.info(
         dateTimeDEBUG(true)
            + "Allocated Memory: "
            + NumberFormat.getInstance().format(allocatedKiloBytes)
            + " kB"
      );
      
   }
   
   public void logMemoryUsingMemBeans(String type){
         MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
         double usage;

         switch (type){
            case "initial": usage = (double)memoryMXBean.getHeapMemoryUsage().getInit(); break;
            case "used": usage = (double)memoryMXBean.getHeapMemoryUsage().getUsed(); break;
            case "maximum": usage = (double)memoryMXBean.getHeapMemoryUsage().getMax(); break;
            case "committed": usage = (double)memoryMXBean.getHeapMemoryUsage().getCommitted(); break;
            default:
               throw new IllegalArgumentException();
         }

         this.logObj.logger.info(
                 dateTimeDEBUG(true)
                         + "Heap Memory (" + type + ")"
                         + usage
         );

      }
      public void logCpuUsage() {
         OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
         this.logObj.logger.info(
                 dateTimeDEBUG(true)
                         + "CPU Percent Usage by JVM: "
                         + new DecimalFormat("0.00").format(osBean.getProcessCpuLoad())
         );
         this.logObj.logger.info(
                 dateTimeDEBUG(true)
                         + "CPU Percent Usage Overall: "
                         + new DecimalFormat("0.00").format(osBean.getSystemCpuLoad())
         );
      }
   
   public void logMemoryBefore () {
      
      long allocatedBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      long allocatedKiloBytes = allocatedBytes / 1024;
      
      this.logObj.logger.info(
         dateTimeDEBUG(true)
            + "MEM###Before GCP Call: "
            + NumberFormat.getInstance().format(allocatedKiloBytes)
            + " kB"
      );
      
   }
   
   public void logMemoryAfter () {
      
      long allocatedBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      long allocatedKiloBytes = allocatedBytes / 1024;
      
      this.logObj.logger.info(
         dateTimeDEBUG(true)
            + "MEM###After GCP Call: "
            + NumberFormat.getInstance().format(allocatedKiloBytes)
            + " kB"
      );
      
   }
   
      public void exceptionThrown(String name){
      this.logObj.logger.info(
         dateTimeDEBUG(true)
            + "EXCEPTION: "
            + name
      );
   }
   
   
   public void queStatus (HashMap<Integer, Boolean> queueStatus, int vacantQueueIndex, int threadNum ){
      this.logObj.logger.info("---------- QUEUE INFORMATION ----------");
      queueStatus.forEach((i, b) -> {
         this.logObj.logger.info(i + ": " + b);
      });
      this.logObj.logger.info(String.valueOf(vacantQueueIndex));
      this.logObj.logger.info(String.valueOf(threadNum));
      this.logObj.logger.info("---------- QUEUE INFORMATION ----------");
   
   }
   
   
   public static String formatMillis(long millis){
      long totalSec = millis/1000;
      long totalMin = totalSec/60;
      
      long sec = Math.abs(totalSec % 60);
      long min = totalMin % 60;
      long hrs = totalMin/60;
      String seconds = sec < 10 ? "0" + sec : String.valueOf(sec);
      System.gc();
      Runtime.getRuntime().gc();
      return String.format("%02d:%02d:%s", hrs, min, seconds);
   }

   public static void main(String[] args) {

      System.gc();
      Runtime.getRuntime().gc();
      System.out.println(formatMillis(3452323));
   }
   
   
   class Memory {
      
      private Runtime runtime = Runtime.getRuntime();
      private Log logObj;

      public long totalMem() {
         return Runtime.getRuntime().totalMemory();
      }
      
      public long usedMem() {
         return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      }
      
      public String memInfo(String pre) {
         NumberFormat format = NumberFormat.getInstance();
         StringBuilder sb = new StringBuilder();
         long maxMemory = runtime.maxMemory();
         long allocatedMemory = runtime.totalMemory();
         long freeMemory = runtime.freeMemory();
         sb.append(pre).append(" Free memory: ");
         sb.append(format.format(freeMemory / 1024));
         sb.append("\n");
         sb.append(pre).append(" Allocated memory: ");
         sb.append(format.format(allocatedMemory / 1024));
         sb.append("\n");
         sb.append(pre).append(" Max memory: ");
         sb.append(format.format(maxMemory / 1024));
         sb.append("\n");
         sb.append(pre).append(" Total free memory: ");
         sb.append(format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024));
         return sb.toString();
         
      }
}
}
