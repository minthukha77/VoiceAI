package com.bliss_stock.aiServerAPI.audioControl;

import com.bliss_stock.aiServerAPI.common.Log;
import com.bliss_stock.aiServerAPI.controller.Speech2textController;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class AudioInfo {
    //    Only works with "wav" format
    static Log myLog = Speech2textController.myLog;
    public static double getDuration(String fileToCheck) {
        try{
            File file = new File(fileToCheck);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = audioInputStream.getFormat();
            long frames = audioInputStream.getFrameLength();
            double durationInSeconds = (frames + 0.0) / format.getFrameRate();
            System.gc();
            Runtime.getRuntime().gc();
            return durationInSeconds;
        }
        catch(Exception e){
            myLog.logger.info("AudioInfo.java, getDuration() Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static ArrayList<Double> getSilence(String filePath){
//                        detect silence
        ArrayList<Double> output = new ArrayList<>();
        try {
            String ffmpegCmd = "ffmpeg -i " + filePath
                    + " -af silencedetect=n=-60dB:d=1 -f null - 2>&1 | grep \"silencedetect\" | awk '{print $4 \" \" $5}'";
            String[] cmd = {"/bin/sh", "-c", ffmpegCmd};
            Process silenceDetect = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(silenceDetect.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if(line.contains("silence_end: ")){
                    double time =Double.parseDouble(line.substring(line.indexOf(":")+1));
                    output.add(time);
                }
            }
            int exitVal = silenceDetect.waitFor();
            if (exitVal == 0) {
                System.out.println("getSilence successful");
            } else {
                System.out.println("getSilence failed");
            }
        }
         catch (Exception e) {
             myLog.logger.info("AudioInfo.java, getSilence() Error: " + e.getMessage());
             throw new RuntimeException(e);
        }
        System.gc();
        Runtime.getRuntime().gc();
        return output;
    }

    public static String frequencyFromFile(String file) {
        try {

            // Executing the command using runtime object.
            Process process = Runtime.getRuntime().exec("ffmpeg -i " + file + " -hide_banner");

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(process.getErrorStream())); // stdError.readLine()

            // The following reads any errors from the attempted command.
            // The cli output includes an error message (saying an output file is missing),
            // so that output can be read as follows.
            String s = null;
            while ((s = stdError.readLine()) != null) {
                if (s.contains(" Hz,")) {
                    return (s.split("[,]"))[1];
                }
            }

        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        System.gc();
        Runtime.getRuntime().gc();
        return "Not found.";
    }
}
