package com.bliss_stock.aiServerAPI.common;

import org.springframework.boot.convert.DurationFormat;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.*;

/*
 * ON LOGGING
 *
 * Log.java is the class on which myLog instances will be built, each of which will be used for one log file.
 * AutoLogger.java is the class for autoLogger instances, which will write logs during multithreading (when a thread starts, when GCP is completed, when the thread closes, etc.). It will use a myLog object.
 * AudioLogger.java also uses a myLog instance and it logs info regarding the audio (parameters, manipulation, etc.)
 *
 * */

public class Log {

//    public Logger logger;
    public com.bliss_stock.aiServerAPI.common.CustomLogger logger;
    
    FileHandler fh;
    
    public Log(String fileName,
               String requiredFormat)
            throws IOException {

        File f = new File(fileName);
        
        if(!f.exists()){
            f.createNewFile();
            System.out.println("created a new file: " + fileName);
        }
        
        this.logger = new com.bliss_stock.aiServerAPI.common.CustomLogger(f);

//        fh = new FileHandler(fileName, true);
//        logger = Logger.getLogger("");
//        logger.addHandler(fh);

//        switch (requiredFormat){
//            case "Custom":
//                CustomFormatter customFormatter = new CustomFormatter();
//                fh.setFormatter(customFormatter);
//                break;
//            case "Simple":
//                SimpleFormatter simpleFormatter = new SimpleFormatter();
//                fh.setFormatter(simpleFormatter);
//                break;
//            case "XML":
//                XMLFormatter xmlFormatter = new XMLFormatter();
//                fh.setFormatter(xmlFormatter);
//                break;
//        }
    }
    static class CustomFormatter extends SimpleFormatter {
        
        @Override
        public String format(LogRecord message) {
            System.gc();
            Runtime.getRuntime().gc();
            return message.getMessage() + "\n";
        }
    }
}

