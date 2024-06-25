package com.bliss_stock.aiServerAPI.gcp;

import com.bliss_stock.aiServerAPI.S3Util;
import com.bliss_stock.aiServerAPI.audioControl.AudioInfo;
import com.bliss_stock.aiServerAPI.audioControl.AudioManipulator;
import com.bliss_stock.aiServerAPI.common.AudioLogger;
import com.bliss_stock.aiServerAPI.model.Speech2text;
import com.svix.exceptions.ApiException;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

import static com.bliss_stock.aiServerAPI.audioControl.AudioManipulator.numericalSort;
import static com.bliss_stock.aiServerAPI.audioControl.AudioStorage.*;
import static com.bliss_stock.aiServerAPI.gcp.UploadObject.uploadObject;

public class UploadAudioToBucket {

    public static String upload(Speech2text parameters, String convertedAudio, AudioLogger audioLogger) throws IOException, UnsupportedAudioFileException, InterruptedException, ParseException, URISyntaxException, ApiException {
////  ====================UPLOAD FILES FROM FORM TO AUDIO_FILES/ AND CONVERTED FILES TO /WAV_FILES========================

        File theDir = new File("/usr/local/src/static/audio_files");
        if (!theDir.exists()) {
            theDir.mkdirs();
        }
        File theDirWav = new File("/usr/local/src/static/wav_files");
        if (!theDirWav.exists()) {
            theDirWav.mkdirs();
        }
        File theDirLog = new File("/usr/local/src/static/logs");
        if (!theDirLog.exists()) {
            theDirLog.mkdirs();
        }
        String[] uploadedFile = parameters.getFile().getOriginalFilename().split("\\.");
        String fileFormat = uploadedFile[uploadedFile.length - 1];
        String SOURCE_PATH = "/usr/local/src/static";
//        String SOURCE_PATH = System.getProperty("user.dir") + "/src/static";

//        System.out.println("New file: " + SOURCE_PATH + "/audio_files/" + "audio_" + parameters.getAudio_id() + "." + fileFormat);
//        File destination = new File(SOURCE_PATH + "/audio_files/" + "audio_" + parameters.getAudio_id() + "." + fileFormat);

//        try {
//            parameters.getFile().transferTo(destination);
//        } catch (IOException e) {
//            System.out.println("Error during copying file to audio_files/: " + e.getMessage());
//            throw new RuntimeException(e);
//        }

        HashMap<String, String> bucketInfo = new HashMap<>();

        bucketInfo.put("projectId", "voitra");
        bucketInfo.put("bucketName", "voitra-stt");
        String[] list = new File(SOURCE_PATH + "/audio_files").list();
        System.out.println("Total files in directory audio_files/ : " + list.length);
//            int audioFilesLengthNow = list.length;
//            System.out.println("file Length NOW|BEFORE :" + audioFilesLengthNow + "|" + audioFilesLengthBefore);
//            boolean needToProcess = audioFilesLengthNow != audioFilesLengthBefore;
        String[] bucketObj = new String[0];
        String wf = null;
        HashMap<String, String> wfHash = new HashMap<>();
        HashMap<String, String> segmentedAudio = new HashMap<>();
        if(new File(convertedAudio).exists()){
            wf = AudioManipulator.noiseCancellingAFFTDN(convertedAudio);
            audioLogger.logAudioEvent("ノイズキャンセリング完了");
        }
        else System.out.println("convertedAudio does not exist");
//=========================PERFORMANCE_IMPROVEMENT_FEATURES LOGICS(comment out to test core features)============================================
        String fileToManipulate = wf;
        double fileDuration = AudioInfo.getDuration(fileToManipulate);

        if (!(fileDuration >= 300)) {
            if (Objects.equals(parameters.getDiarization(), "0")) {
                fileToManipulate = AudioManipulator.removeSilence(convertedAudio);
                audioLogger.logAudioEvent("無言音声削減完了");
            }
            wfHash.put("type", "file");
            wfHash.put("path", fileToManipulate);
        }
        else{
            segmentedAudio = AudioManipulator.segmentAudio(fileToManipulate);
            audioLogger.logAudioEvent("ファイル分割完了");
            if (!Objects.equals(parameters.getDiarization(), "1")) {
//                loop segment folder, remove silence of each audio, store in new directory.
                System.out.println("Request is not diarization, removing silence in all of segments...");
                File newFolder = new File("/usr/local/src/static/wav_files/" + segmentedAudio.get("fileName") + "_rs_SEGMENTS");
                newFolder.mkdirs();
                File[] segmentsList = new File(segmentedAudio.get("dirName")).listFiles();
//                numericalSort(segmentsList);
                for (int i = 0; i < segmentsList.length; i++) {
                    String currentFile = segmentsList[i].getAbsolutePath();
                    String sourceFile = AudioManipulator.removeSilence(currentFile);//will be in wav_files/
                    String targetFile = newFolder.getAbsolutePath() + "/" + sourceFile.split("/")[6];
                    Files.move(Paths.get(sourceFile), Paths.get(targetFile));
                    System.out.println("source file: " + sourceFile);
                    System.out.println("target file: " + targetFile);
                }
                segmentedAudio.put("dirName", newFolder.getAbsolutePath() + "/");
                String newFileName = segmentedAudio.get("fileName") + "_rs";
                segmentedAudio.put("fileName", newFileName);
            }
            wfHash.put("type", "directory");
            wfHash.put("path", segmentedAudio.get("dirName"));
        }
//        ----------------------------------------------------------------------------
//            }
//================================ END OF FILE PROCESSING ================================================================
//================================ UPLOADING WAV FILES TO BUCKET "voitra-stt/GCP_AUDIO"===============================
//        listing all files under voitra-stt/GCP_AUDIO
        bucketObj = ListObjectsWithPrefix.listObjectsWithPrefix(bucketInfo.get("projectId"), bucketInfo.get("bucketName"), "GCP_AUDIO/");
        List bucketObjList = Arrays.asList(bucketObj);
//        if the file is a single file(not a directory)
        if (Objects.equals(wfHash.get("type"), "file")) {
            String[] wfPath = wfHash.get("path").split("/");
            String fName = wfPath[wfHash.get("path").split("/").length - 1];
            if (!bucketObjList.contains("GCP_AUDIO/" + fName)) {
                uploadObject(bucketInfo.get("projectId"),
                        bucketInfo.get("bucketName"),
                        "GCP_AUDIO/" + fName,
                        wfHash.get("path"));
//                      SOURCE_PATH + "/wav_files/" + wfHash.get("path"));
            }
            System.out.println("fName: " + fName);
            String zippedFile = zipper(true, wfHash.get("path"));
            S3Util.uploadObject(zippedFile);
            delete(zippedFile);
        } else {
            File[] segments = new File(segmentedAudio.get("dirName")).listFiles();
            assert segments != null;
            System.out.println("Number of segments to upload: " + segments.length);
            for (int i = 0; i < segments.length; i++) {
                String fName = segments[i].getName();
                System.out.println("file to upload: " + fName);
                uploadObject(bucketInfo.get("projectId"),
                        bucketInfo.get("bucketName"),
                        "GCP_AUDIO/" + fName,
                        wfHash.get("path") + fName);
            }
            String zippedFile = zipper(false, segmentedAudio.get("dirName"));
            S3Util.uploadObject(zippedFile);
            delete(zippedFile);
            System.out.println("Segments successfully uploaded");
            wfHash.put("path", wfHash.get("path"));
        }
//================================  END OF BUCKET UPLOAD ===============================================================
        System.gc();
        Runtime.getRuntime().gc();
        return wfHash.get("path");
    }

}