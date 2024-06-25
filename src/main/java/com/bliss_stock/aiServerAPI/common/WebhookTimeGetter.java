package com.bliss_stock.aiServerAPI.common;

import com.bliss_stock.aiServerAPI.controller.Speech2textController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class WebhookTimeGetter {
    static Log myLog= Speech2textController.myLog;
    public static Date getTimeNow() {
        LocalDateTime time1Obj = LocalDateTime.now();
        DateTimeFormatter timeFormatObj = DateTimeFormatter.ofPattern("HH:mm:ss");
        String requestStartTime = time1Obj.format(timeFormatObj);
        SimpleDateFormat simpleDateFormat
                = new SimpleDateFormat("HH:mm:ss");
        try{
            Date time = simpleDateFormat.parse(requestStartTime);
            System.gc();
            Runtime.getRuntime().gc();
            return time;
        }
        catch(ParseException e){
            myLog.logger.info("WebhookTimeGetter.java, getTimeNow() Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
