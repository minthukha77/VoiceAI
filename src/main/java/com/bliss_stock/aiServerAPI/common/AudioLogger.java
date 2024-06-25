package com.bliss_stock.aiServerAPI.common;

import com.bliss_stock.aiServerAPI.model.Speech2text;
import com.bliss_stock.aiServerAPI.common.CustomTimeFormat;

import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public class AudioLogger {
   
   private Log logObj;
   private Speech2text params;
   public static CustomTimeFormat lineStart = new CustomTimeFormat(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS"));
   public static Function<String, String> filler = (type) -> " :: " + type + " :: ";
   
   public AudioLogger(Log logObj, Speech2text params){
      this.logObj = logObj;
      this.params = params;
      
   }

   public void logNewAudio(){
      this.logObj.logger.info(
         dateTimeDEBUG()
            + "NEW AUDIO RECEIVED: AUDIO_ID: " + params.getAudio_id()
      );
      logAudioInfo("Language", true, params.getLang());
      logAudioInfo("Diarization", true, params.getDiarization());
      logAudioInfo("Speaker Number", true, params.getNumber());
      logAudioInfo("Wait", true, params.getWait());
      logAudioInfo("Callback", true, params.getCallback());
   }
   
   public void logReturnCode(String code){
      logAudioInfo("INITIAL_Return Code: ", true, code);
   }
   
   public void logAudioInfo(String key, boolean withValue, String value){
      this.logObj.logger.info(
         dateTimeDEBUG()
            + "AUDIO_ID: " + params.getAudio_id() + "_"
            + key + (withValue ? ": " + value : "") // Either "key:value" or "key"
      );
   }
   
   public void logAudioEvent(String event){
      logAudioInfo(event, false, "");
   }
   
   public String dateTimeDEBUG() {
      return lineStart.nowRec() + filler.apply("DEBUG");
   }
}