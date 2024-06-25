package com.bliss_stock.aiServerAPI.webhook;

import com.bliss_stock.aiServerAPI.common.Log;
import com.bliss_stock.aiServerAPI.controller.Speech2textController;
import com.svix.Svix;
import com.svix.exceptions.ApiException;
import com.svix.models.EndpointIn;
import com.svix.models.EndpointOut;
import com.svix.models.MessageIn;
import com.svix.models.MessageOut;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class Webhook {
    static Log myLog = Speech2textController.myLog;
    private static Svix svix = new Svix("testsk_ILZZRaKTH75-QE73j3VWXbZACLZhvyWe.eu");
    private static String appId = "app_2Hq8YhkyHUo2hUFdCG4YgpWswZq";
    public static void createMessage(int audio_id, double totalTime, String callbackURL, String result, String eventType) {
        //String[] urlArray = callbackURL.split("/");
        boolean status = result != "";
        byte[] bytes = result.getBytes(StandardCharsets.UTF_8);
        String httpsCallbackURL = callbackURL;
        String utf8Result = new String(bytes, StandardCharsets.UTF_8);
        try{
            MessageOut messageOut = svix.getMessage().create(appId, new MessageIn()
                    .eventType(eventType)
                    .eventId(UUID.randomUUID().toString())
                    .channels(null)
                    .payload("{\"callbackURL\":\""+ httpsCallbackURL +"\"," +
                            "\"audio_id\":" + audio_id + "," +
                            "\"status\":" + status + "," +
                            "\"total_time\":" + totalTime + "," +
                            "\"results\":\"" + utf8Result + "\"}"
                    )
                    .payloadRetentionPeriod(90));
            System.gc();
            Runtime.getRuntime().gc();
        }
        catch(ApiException e){
            myLog.logger.info("Webhook.java createMessage() Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public static void createMessageDiarization(int audio_id, double totalTime, String callbackURL, ArrayList result, String eventType){
        boolean status = result != null;
        String httpsCallbackURL = callbackURL;
        try{
            MessageOut messageOut = svix.getMessage().create(appId, new MessageIn()
                    .eventType(eventType)
                    .eventId(UUID.randomUUID().toString())
                    .channels(null)
                    .payload("{\"callbackURL\":\""+ httpsCallbackURL +"\"," +
                            "\"audio_id\":" + audio_id + "," +
                            "\"results\":" + result + "," +
                            "\"status\":" + status + "," +
                            "\"total_time\":" + totalTime +
                            "}"
                    )
                    .payloadRetentionPeriod(90));
            System.gc();
            Runtime.getRuntime().gc();
        }
        catch(Exception e){
            myLog.logger.info("Webhook.java createMessageDiarization() Error: " + e.getMessage());
            throw new RuntimeException(e);
        }

    }
    public static void endpointDelete(String endpointId){
        try{
            svix.getEndpoint().delete(appId, endpointId);
            System.gc();
            Runtime.getRuntime().gc();

        }
        catch(ApiException e){
            myLog.logger.info("Webhook.java endpointDelete() Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public static void endpointCreate(String endpointURL){
        try{
            svix.getEndpoint().create(appId, new EndpointIn()
                    .uid(UUID.randomUUID().toString())
                    .url(new URI(endpointURL))
                    .version(1)
                    .description("Voitra web server's webhook consumer")
                    .filterTypes(null)
                    .channels(null)
                    .disabled(false)
                    .rateLimit(null)
                    .secret(null) //set to null for automatic verification secret generations
            );
            System.gc();
            Runtime.getRuntime().gc();
        }
        catch(Exception e){
            myLog.logger.info("Webhook.java endpointCreate() Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}