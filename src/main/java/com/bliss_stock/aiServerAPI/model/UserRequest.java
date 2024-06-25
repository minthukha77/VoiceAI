package com.bliss_stock.aiServerAPI.model;

import com.bliss_stock.aiServerAPI.common.Log;
import com.svix.exceptions.ApiException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Objects;

public class UserRequest {
    static Log myLog;
    public static InitialResult validate(Speech2text parameters){
        String diarization = parameters.getDiarization() == null? "0": parameters.getDiarization();
        String numberOfSpeaker = parameters.getNumber() == null? "0": parameters.getNumber();
        String[] parameterValues = {
                parameters.getFile().getOriginalFilename(),
                parameters.getAudio_id(),
                parameters.getCallback(),
                parameters.getToken(),
                parameters.getLang(),
                parameters.getWait(),
                diarization,
                numberOfSpeaker
        };
        try{
            if (parameters.getFile().getBytes().length > 0 && !Arrays.asList(parameterValues).contains(null)){
                return new InitialResult(true);
            }
            else {
                System.gc();
                Runtime.getRuntime().gc();
                return new InitialResult(true);
            }
        }
        catch(IOException e){
            myLog.logger.info("UserRequest.java, validate() Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
