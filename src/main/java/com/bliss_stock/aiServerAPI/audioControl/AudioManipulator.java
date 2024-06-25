package com.bliss_stock.aiServerAPI.audioControl;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

import com.bliss_stock.aiServerAPI.audioControl.AudioInfo;
import com.bliss_stock.aiServerAPI.common.Log;
import com.bliss_stock.aiServerAPI.controller.Speech2textController;

import static com.bliss_stock.aiServerAPI.audioControl.AudioInfo.getDuration;
import static com.bliss_stock.aiServerAPI.audioControl.AudioInfo.getSilence;

public class AudioManipulator {
    static String SOURCE_PATH = "/usr/local/src/static";
    static Log myLog = Speech2textController.myLog;
    static double duration;
    static final double segmentationTime = 300.0;
    static final double range = 30.0;
    public static String removeSilence(String filePath) {
        File wav_file = new File(filePath);
        String fileName = wav_file.getName().substring(0, wav_file.getName().indexOf("."));
        String wavFilePath = SOURCE_PATH + "/wav_files/" + fileName + "_rs.wav";

        String cmd = "ffmpeg -i " + filePath + " -af silenceremove=" + "detection=peak:" + "stop_mode=all:" + "start_mode=all:" + "stop_threshold=-60dB:" + "start_threshold=-60dB:" + "start_periods=0:" + "stop_periods=-1 " + wavFilePath;
        System.out.println("removeSilence command: " + cmd);
        try{
            Process removeSiln = Runtime.getRuntime().exec(cmd);
            if (removeSiln.waitFor() == 0) {
                System.out.println("Silence removal successful");
            } else {
                System.out.println("Silence removal failed");
                wavFilePath = filePath;
            }
            System.gc();
            Runtime.getRuntime().gc();
            return wavFilePath;
        }
        catch(Exception e){
            myLog.logger.info("AudioManipulator.java, removeSilence() Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static HashMap<String, String> segmentAudio(String filePath) {
        try{
            DecimalFormat df3 = new DecimalFormat("#.###");
            df3.setRoundingMode(RoundingMode.DOWN);
            File wav_file = new File(filePath);
            String fileName = wav_file.getName().substring(0, wav_file.getName().indexOf("."));
            duration = getDuration(filePath);
            String segmentDir = SOURCE_PATH + "/wav_files/" + fileName + "_SEGMENTS/";
            Runtime.getRuntime().exec("mkdir " + segmentDir);
            Process segmentation = null;
            ArrayList<Double> rawSilence = getSilence(filePath);
            ArrayList<Double> silenceUnique = new ArrayList<>(){{
                add(0.0);
            }};
            double searchStartTime = 0.0;
            double lastChoice = 0.0;
            while(duration-lastChoice >=300){
                System.out.println("============================");
                double searchTime = searchStartTime + 300;
                System.out.println("lastChoice: " + lastChoice);
                System.out.println("searchTime: " + searchTime);
                var la = findAdjacentSilence(rawSilence, searchTime);
                System.out.println("Lower and Above: " + la);
                var choice = chooseSilence(la.get(0), la.get(1), searchTime, lastChoice);
                lastChoice = choice;
                System.out.println("Choice: " + choice);
                silenceUnique.add(choice);
                System.out.println("============================");
                searchStartTime = searchTime;
            }
            System.out.println("rawSilence: " + rawSilence);
            if(silenceUnique.size()>1){
                if(silenceUnique.contains(duration)){
                    silenceUnique.remove(duration);
                }
                if(isInRange(duration - silenceUnique.get(silenceUnique.size()-2))){
                    silenceUnique.remove(silenceUnique.get(silenceUnique.size()-1));
                }
            }
            System.out.println("silenceUnique: " + silenceUnique);
            double durationToClip = 0;
            System.out.println("audio duration: " + duration);
            for(int i=0; i<=silenceUnique.size()-1; i++){
                if(i == silenceUnique.size()-1){
                    durationToClip = Double.parseDouble(df3.format(duration - silenceUnique.get(i)));
                }
                else{
                    durationToClip = silenceUnique.get(i+1) - silenceUnique.get(i);
                }

                System.out.println("==============================Start loop-" + (i + 1) + "===============================");
                String cmd = "ffmpeg -ss " + silenceUnique.get(i) +
                        " -i " + filePath + " -t " + durationToClip + " " + segmentDir + (i + 1) + "_" + fileName + ".wav";
                System.out.println("Segmentaion cmd for loop [" + (i + 1) + "]: " + cmd);
                System.out.println("Duration of segment " + (i+1) + ": " + df3.format(durationToClip/60) + " min");
				segmentation = Runtime.getRuntime().exec(cmd);
                System.out.println("==============================End loop-" + (i + 1) + "===============================");
            }

            if (segmentation.waitFor() == 0) {
                System.out.println("Segmentation successful");
            } else {
                System.out.println("Segmentation failed");
            }
            int NoOfFiles = new File(segmentDir).list().length;
            myLog.logger.info("====================SEGMENTATION INFO====================");
            myLog.logger.info(fileName + " rawSilence: " + rawSilence);
            myLog.logger.info(fileName + " silenceUnique: " + silenceUnique);
            myLog.logger.info("Number of segments: " + NoOfFiles);
            myLog.logger.info("==========================================================");
            HashMap<String, String> returnVal = new HashMap<>(){
                {
                    put("dirName", segmentDir);
                    put("noOfFiles", String.valueOf(NoOfFiles));
                    put("fileName", fileName);
                }
            };
            System.gc();
			Runtime.getRuntime().gc();
            return returnVal;
        }
        catch(Exception e){
			myLog.logger.info("AudioManipulator.java, segmentAudio() Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public static ArrayList<Double> findAdjacentSilence(List<Double> rawSilence, double searchTime){
        double lower = -1;
        double above = -99;
        for(var x: rawSilence){
            if (x<= searchTime){
                lower = x;
            }
        }
        if(rawSilence.indexOf(lower) != rawSilence.size()-1){
            above = rawSilence.get(rawSilence.indexOf(lower)+1);
        }
        ArrayList<Double> returnArr = new ArrayList<>();
        returnArr.add(lower);
        returnArr.add(above);
        return returnArr;
    }
    public static double chooseSilence(double lower, double above, double ref, double lastChoice){
        double choice=ref;
//		both values exist
        if(lower != -1 && above != -99){
            double aboveDiff = above - ref;
            double lowerDiff = ref - lower;
            if (aboveDiff < lowerDiff){
                System.out.println("Above is closer to searchTime");
                choice = above;
            }
            if (lowerDiff < aboveDiff){
                System.out.println("Lower is closer to searchTime");
                choice = lower;
            }
            if (lowerDiff == aboveDiff){
                System.out.println("both lower and above values are equally far from searchTime");
                choice = lower;
            }
        }
//		both values do not exist
        if(lower == -1 && above == -99){
            System.out.println("Both values do not exist, taking searchTime as choice");
            choice = ref;
        }
//		only lower exists
        if (lower != -1 && above == -99){
            System.out.println("Only lower value exist, taking lower value as choice");
            choice = lower;
        }
//		only above exists
        if (above != -99 && lower == -1){
            System.out.println("Only above value exist, taking above value as choice");
            choice = above;
        }

        if(!(isInRange(choice-lastChoice))){
            choice = ref;
        }
        if(duration - choice <=359 && duration - choice >= 121){
            System.out.println("in last element condition");
            if(isInRange(choice - lastChoice - 300)){
                choice = duration;
            }
        }
        return choice;
    }
    public static boolean isInRange(double length){
        if(240<length && length<480){
            return true;
        }
        return false;
    }
    public static String noiseCancellingAFFTDN(String input_file) {
        String ncAfftdnPath = "";
        File inputFile = new File(input_file);
        String outputFile = SOURCE_PATH + "/wav_files/" + inputFile.getName().substring(0, inputFile.getName().indexOf(".")) + "_nc.wav";
        System.out.println("NC AFFTDN Command: " + "ffmpeg -i " + input_file + " -af afftdn=nr=97:nf=-40:nt=c:bn=66|165 " + outputFile);
        try{
            Process ncAfftdn = Runtime.getRuntime().exec("ffmpeg -i " + input_file + " -af afftdn=nr=97:nf=-40:nt=c:bn=66|165 " + outputFile);
            ncAfftdnPath = outputFile;
            if(ncAfftdn.waitFor()== 0){
                System.out.println("Noise cancellation successful.");
            }
            else{
                System.out.println("Noise cancellation failed.");
                ncAfftdnPath = input_file;
            }

            System.gc();
            Runtime.getRuntime().gc();
            return ncAfftdnPath;
        }
        catch(Exception e){
            myLog.logger.info("AudioManipulator.java, noiseCancellingAFFTDN() Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public static void numericalSort(File[] fileArray){
        Arrays.sort(fileArray, new Comparator<File>() {
            public int compare(File f1, File f2) {
                try {
                    int i1 = Integer.parseInt(f1.getName().split("_")[0]);
                    int i2 = Integer.parseInt(f2.getName().split("_")[0]);
                    return i1 - i2;
                } catch(NumberFormatException e) {
                    throw new AssertionError(e);
                }
            }
        });
    }
}
