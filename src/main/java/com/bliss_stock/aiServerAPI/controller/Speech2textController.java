package com.bliss_stock.aiServerAPI.controller;
import com.bliss_stock.aiServerAPI.audioControl.AudioConverter;
import com.bliss_stock.aiServerAPI.audioControl.AudioStorage;
import com.bliss_stock.aiServerAPI.common.AudioLogger;
import com.bliss_stock.aiServerAPI.common.CustomTimeFormat;
import com.bliss_stock.aiServerAPI.common.Log;
import com.bliss_stock.aiServerAPI.gcp.UploadAudioToBucket;
import com.bliss_stock.aiServerAPI.model.FileToTranscribe;
import com.bliss_stock.aiServerAPI.queueOperator.QueueOperator;
import com.bliss_stock.aiServerAPI.model.InitialResult;
import com.bliss_stock.aiServerAPI.model.Speech2text;
import com.bliss_stock.aiServerAPI.model.UserRequest;
import com.svix.exceptions.ApiException;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.*;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

@CrossOrigin(origins = "*")

@RestController
@RequestMapping(value = "", consumes = "multipart/form-data")
public class Speech2textController {

    private Environment env;
    private AsyncService asyncService;

    public static Log myLog;
    static String logFileName;
    static QueueOperator diarizer;
    public static CustomTimeFormat currentTime = new CustomTimeFormat(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS"));
    //    @GetMapping("/speech2text")
//    public Result speech2text(@RequestParam(value = "file", defaultValue = "path_to_file") String file, @RequestParam(value = "number", defaultValue = "1") String number, @RequestParam(value = "lang", defaultValue = "ja") String lang, @RequestParam(value = "token", defaultValue = "token_1") String token, @RequestParam(value = "dirization", defaultValue = "yes") String dirization, @RequestParam(value = "audio_id", defaultValue = "audio_1") String audio_id, @RequestParam(value = "callback", defaultValue = "function()") String callback, @RequestParam(value = "wait", defaultValue = "yes") String wait) {
    //    use @RequestBody instead of @ModelAttribute to send JSON data

    @Autowired
    public Speech2textController(Environment env, AsyncService asyncService){

        this.env = env;
        this.asyncService = asyncService;

        logFileName = env.getProperty("log.filename");

        try {

            myLog = new Log("/usr/local/src/static/logs/" + logFileName + ".txt", "Custom");
            diarizer = new QueueOperator(myLog, 4); // This has to be static for multithreading to function properly.

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/speech2text") public InitialResult s2t(@ModelAttribute Speech2text parameters){
        String SOURCE_PATH = "/usr/local/src/static";
        String[] uploadedFile = parameters.getFile().getOriginalFilename().split("\\.");
        String fileFormat = uploadedFile[uploadedFile.length - 1];
        AudioLogger audioLogger = new AudioLogger(myLog, parameters);
        audioLogger.logNewAudio(); // To log parameters
        InitialResult success = UserRequest.validate(parameters);
        File destination = new File(SOURCE_PATH + "/audio_files/" + "audio_" + parameters.getAudio_id() + "." + fileFormat);
        String convertedAudio;
        try{
            parameters.getFile().transferTo(destination);
            String filePath = SOURCE_PATH + "/audio_files/" + "audio_" + parameters.getAudio_id() + "." + fileFormat;
            System.out.println("filePath: " + filePath);
            String file_to_convert_from = filePath;
            String file_to_convert_to = SOURCE_PATH + "/wav_files/" + "audio_" + parameters.getAudio_id() + ".wav";

//                 System.out.println(file_to_convert_from + "-->" + file_to_convert_to);
            AudioConverter audioConverter = new AudioConverter(file_to_convert_from, file_to_convert_to, 16000);
//          noise reduction not included
            convertedAudio = audioConverter.convertAudio();
//            delete file in audio_files/ after conversion
            AudioStorage.delete(filePath);
        }
        catch (IOException e) {
            System.out.println("Error during copying file to audio_files/: " + e.getMessage());
            throw new RuntimeException(e);
        }

        asyncService.process(new Processes(parameters, convertedAudio, audioLogger, success));
        
        myLog.logger.info("tmp:::" + "Returning success.");
        System.out.println("tmp:::" + "Returning success.");
        return success;
    }

    public static void logAndRespond(int code, String message) {
        myLog.logger.info(message);
        HttpServletResponse res = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        res.setStatus(code);
        try {
            if (code != 200) res.sendError(code, message);
        } catch (IOException ex) {
            myLog.logger.info("Error producing HTTP response: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    class Processes implements Runnable {

        private Speech2text parameters;
        private String convertedAudio;
        private AudioLogger audioLogger;
        private InitialResult success;

        public Processes(

            Speech2text parameters,
            String convertedAudio,
            AudioLogger audioLogger,
            InitialResult success){
            this.parameters = parameters;
            this.convertedAudio = convertedAudio;
            this.audioLogger = audioLogger;
            this.success = success;
        }

        @Override
        public void run() {
            myLog.logger.info("tmp:::" + "Processing.");
            System.out.println("tmp:::" + "Processing.");
            try{
                String wf = UploadAudioToBucket.upload(parameters, convertedAudio, audioLogger);
                System.out.println("wf: " + wf);
                boolean isDirectory = false;
    
                if(Pattern.matches("^.*_SEGMENTS/$", wf)){
    //                wf = wf.substring(3);
                    System.out.println("It is a directory.");
                    isDirectory = true;
                }
    
                audioLogger.logReturnCode(success.getSuccess() ? "200" : "400");
    
                FileToTranscribe newRequest = new FileToTranscribe(parameters, success, wf);
    
                if(isDirectory){
                    diarizer.addToLongQueue(newRequest);
                } else {
                    diarizer.addToShortQueue(newRequest);
                }
            }
            catch (UnsupportedAudioFileException e) {
                logAndRespond(400,
                        "Speech2textController.java, InitialResult: " + success.getSuccess() + " AUDIO_ID: " + parameters.getAudio_id() + " 音声ファイルサポート外例外が発生しました。(Unsupported Audio Exception) " + e.getMessage());
                throw new RuntimeException(e);
            } catch (IOException e) {
                logAndRespond(500, "IOException: " + e.getMessage());
                throw new RuntimeException(e);
            } catch (ParseException e) {
                logAndRespond(400, "ParseException: " + e.getMessage());
                throw new RuntimeException(e);
            } catch (URISyntaxException e) {
                logAndRespond(400, "Endpoint URL incorrect: " + parameters.getCallback() + ", message: " + e.getMessage());
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                logAndRespond(400, "Audio upload interrupted at " + currentTime.nowRec());
                throw new RuntimeException(e);
            } catch (ApiException e) {
                logAndRespond(400, "Svix API exception: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
        
    }
}