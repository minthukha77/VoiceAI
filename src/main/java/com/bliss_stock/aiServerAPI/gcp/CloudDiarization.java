package com.bliss_stock.aiServerAPI.gcp;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.speech.v1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeakerDiarizationConfig;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.WordInfo;
import com.google.protobuf.Duration;
import com.bliss_stock.aiServerAPI.audioControl.AudioConverter;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.HashMap;
import java.util.ArrayList;

import static com.bliss_stock.aiServerAPI.controller.Speech2textController.myLog;
import static com.bliss_stock.aiServerAPI.gcp.GcpUtil.replaceSpecialChars;


public class CloudDiarization {
    public static ArrayList<HashMap<String, String>> transcribeDiarizationGcs(
            String gcsUri,
            String customLang,
            String customEncoding,
            int customHertz,
            int SpeakerCountMin,
            int SpeakerCountMax
    )
            throws IOException, ExecutionException, InterruptedException, CancellationException {

        ArrayList<HashMap<String, String>> resultArray = new ArrayList<HashMap<String, String>>();

        try {
            SpeechClient speechClient = SpeechClient.create();
            SpeakerDiarizationConfig speakerDiarizationConfig =
                    SpeakerDiarizationConfig.newBuilder()
                            .setEnableSpeakerDiarization(true)
                            .setMinSpeakerCount(SpeakerCountMin)
                            .setMaxSpeakerCount(SpeakerCountMax)
                            .build();
            RecognitionConfig config =
                    RecognitionConfig.newBuilder()
                            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                            .setLanguageCode(customLang)
                            .setSampleRateHertz(customHertz)
                            .setDiarizationConfig(speakerDiarizationConfig)
                            .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();

            OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> future =
                    speechClient.longRunningRecognizeAsync(config, audio);
            while (!future.isDone()) {
                Thread.sleep(10000);
            }
            LongRunningRecognizeResponse response = future.get();


            SpeechRecognitionAlternative alternative =
                    response.getResults(response.getResultsCount() - 1).getAlternatives(0);

            WordInfo wordInfo = alternative.getWords(0);
            HashMap<String, String> resultInfo = new HashMap<String, String>();
            int currentSpeakerTag = wordInfo.getSpeakerTag();
            String text = "";
            String speaker = String.valueOf(alternative.getWords(0).getSpeakerTag());
            String start = String.valueOf(TimeInSeconds(alternative.getWords(0).getStartTime()));
            String stop = String.valueOf(TimeInSeconds(alternative.getWords(0).getEndTime()));


            for (int i = 0; i < alternative.getWordsCount(); i++) {
                wordInfo = alternative.getWords(i);
                if (currentSpeakerTag == wordInfo.getSpeakerTag()) {
                    speaker = String.valueOf(wordInfo.getSpeakerTag());
                    stop = String.valueOf(TimeInSeconds(alternative.getWords(i).getEndTime()));
                    text += wordInfo.getWord().split("[|]")[0];

                } else {
                    resultInfo.put("speaker", speaker);
                    resultInfo.put("start", start);
                    resultInfo.put("stop", stop);
                    resultInfo.put("text", text);
                    resultArray.add(resultInfo);

                    resultInfo = new HashMap<String, String>();

                    text = "";
                    speaker = String.valueOf(wordInfo.getSpeakerTag());
                    start = String.valueOf(TimeInSeconds(alternative.getWords(i).getStartTime()));
                    stop = String.valueOf(TimeInSeconds(alternative.getWords(i).getEndTime()));
                    text += wordInfo.getWord().split("[|]")[0];

                    currentSpeakerTag = wordInfo.getSpeakerTag();

                }
            }
            String cleanText = replaceSpecialChars(text);
            resultInfo.put("speaker", speaker);
            resultInfo.put("start", start);
            resultInfo.put("stop", stop);
            resultInfo.put("text", cleanText);
            resultArray.add(resultInfo);
            System.gc();
            Runtime.getRuntime().gc();
            return resultArray;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            String segmentName = gcsUri.substring(gcsUri.lastIndexOf("/")+1);
            myLog.logger.info("Diarization error at" + segmentName);
            myLog.logger.info("error message: " + String.valueOf(e.getCause()));
            resultArray.add(new HashMap<String, String>(){{
                put("speaker", "1");
                put("start", "0.0");
                put("stop", "0.0");
                put("text", "");
            }});
            return resultArray;
        }
        catch (CancellationException e){
            String segmentName = gcsUri.substring(gcsUri.lastIndexOf("/")+1);
            myLog.logger.info("Diarization error at " + segmentName);
            myLog.logger.info("error message: " + e.getMessage());
            resultArray.add(new HashMap<String, String>(){{
                put("speaker", "1");
                put("start", "0.0");
                put("stop", "0.0");
                put("text", "");
            }});
            return resultArray;
        }
        catch (ExecutionException e) {
            String error = String.valueOf(e.getCause());
            if (error.contains("InvalidArgumentException")) {
                myLog.logger.info("Handling InvalidArgumentException");
                String[] array = gcsUri.split("GCP_AUDIO/");
                String obj_name = array[1];
                String SOURCE_PATH = System.getProperty("user.dir") + "/src/static";
                DownloadObject d_obj = new DownloadObject();
                d_obj.downloadObject("voitra", "voitra-stt", "GCP_AUDIO/" + obj_name, SOURCE_PATH + "/audio_files/" + obj_name);

                String file_to_convert_from = SOURCE_PATH + "/audio_files/" + obj_name;
                String file_to_convert_to = SOURCE_PATH + "/wav_files/" + obj_name.substring(0, obj_name.lastIndexOf(".")) + ".wav";
                AudioConverter audioConverter = new AudioConverter(file_to_convert_from, file_to_convert_to, 16000);
                audioConverter.convertAudio();

                UploadObject.uploadObject("voitra", "voitra-stt", "GCP_AUDIO/" + obj_name, SOURCE_PATH + "/wav_files/" + obj_name);

                resultArray = CloudDiarization.transcribeDiarizationGcs(
                        "gs://voitra-stt/GCP_AUDIO/" + obj_name,
                        customLang,
                        "LINEAR16",
                        16000,
                        2,
                        SpeakerCountMax);
            }
            System.gc();
            Runtime.getRuntime().gc();
            return resultArray;
        }
    }

    public static ArrayList<HashMap<String, String>> transcribeDiarization(
            String gcsUri,
            String customLang,
            String customEncoding,
            int customHertz
    )
            throws IOException, ExecutionException, InterruptedException, CancellationException {
        ArrayList<HashMap<String, String>> resultArray = new ArrayList<HashMap<String, String>>();
        try {
            SpeechClient speechClient = SpeechClient.create();
            SpeakerDiarizationConfig speakerDiarizationConfig =
                    SpeakerDiarizationConfig.newBuilder()
                            .setEnableSpeakerDiarization(true)
                            .build();
            RecognitionConfig config =
                    RecognitionConfig.newBuilder()
                            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                            .setLanguageCode(customLang)
                            .setSampleRateHertz(customHertz)
                            .setDiarizationConfig(speakerDiarizationConfig)
                            .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();

            OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> future =
                    speechClient.longRunningRecognizeAsync(config, audio);
            LongRunningRecognizeResponse response = future.get();


            SpeechRecognitionAlternative alternative =
                    response.getResults(response.getResultsCount() - 1).getAlternatives(0);

            WordInfo wordInfo = alternative.getWords(0);
            HashMap<String, String> resultInfo = new HashMap<String, String>();
            int currentSpeakerTag = wordInfo.getSpeakerTag();
            String text = "";
            String speaker = String.valueOf(alternative.getWords(0).getSpeakerTag());
            String start = String.valueOf(TimeInSeconds(alternative.getWords(0).getStartTime()));
            String stop = String.valueOf(TimeInSeconds(alternative.getWords(0).getEndTime()));


            for (int i = 0; i < alternative.getWordsCount(); i++) {

                wordInfo = alternative.getWords(i);
                if (currentSpeakerTag == wordInfo.getSpeakerTag()) {
                    speaker = String.valueOf(wordInfo.getSpeakerTag());
                    stop = String.valueOf(TimeInSeconds(alternative.getWords(i).getEndTime()));
                    text += wordInfo.getWord().split("[|]")[0];

                } else {
                    resultInfo.put("speaker", speaker);
                    resultInfo.put("start", start);
                    resultInfo.put("stop", stop);
                    resultInfo.put("text", text);
                    resultArray.add(resultInfo);
                    
                    resultInfo = new HashMap<String, String>();
                    text = "";
                    speaker = String.valueOf(wordInfo.getSpeakerTag());
                    start = String.valueOf(TimeInSeconds(alternative.getWords(i).getStartTime()));
                    stop = String.valueOf(TimeInSeconds(alternative.getWords(i).getEndTime()));
                    text += wordInfo.getWord().split("[|]")[0];

                    currentSpeakerTag = wordInfo.getSpeakerTag();

                }
            }
            String cleanText = replaceSpecialChars(text);
            resultInfo.put("speaker", speaker);
            resultInfo.put("start", start);
            resultInfo.put("stop", stop);
            resultInfo.put("text", cleanText);
            resultArray.add(resultInfo);

            System.gc();
            Runtime.getRuntime().gc();
            return resultArray;
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            String segmentName = gcsUri.substring(gcsUri.lastIndexOf("/")+1);
            myLog.logger.info("Diarization error at" + segmentName);
            myLog.logger.info("error message: " + String.valueOf(e.getCause()));
            resultArray.add(new HashMap<String, String>(){{
                put("speaker", "1");
                put("start", "0.0");
                put("stop", "0.0");
                put("text", "");
            }});
            return resultArray;
        }
        catch (CancellationException e){
            String segmentName = gcsUri.substring(gcsUri.lastIndexOf("/")+1);
            myLog.logger.info("Diarization error at" + segmentName);
            myLog.logger.info("error message: " + e.getMessage());
            resultArray.add(new HashMap<String, String>(){{
                put("speaker", "1");
                put("start", "0.0");
                put("stop", "0.0");
                put("text", "");
            }});
            return resultArray;
        }
        catch (ExecutionException e) {
            String error = String.valueOf(e.getCause());
            if (error.contains("InvalidArgumentException")) {
                myLog.logger.info("Handling InvalidArgumentException");
                String[] array = gcsUri.split("GCP_AUDIO/");
                String obj_name = array[1];

                String SOURCE_PATH = System.getProperty("user.dir") + "/src/static";
                DownloadObject d_obj = new DownloadObject();
                d_obj.downloadObject("voitra", "voitra-stt", "GCP_AUDIO/" + obj_name, SOURCE_PATH + "/audio_files/" + obj_name);

                String file_to_convert_from = SOURCE_PATH + "/audio_files/" + obj_name;
                String file_to_convert_to = SOURCE_PATH + "/wav_files/" + obj_name.substring(0, obj_name.lastIndexOf(".")) + ".wav";
                AudioConverter audioConverter = new AudioConverter(file_to_convert_from, file_to_convert_to, 16000);
                audioConverter.convertAudio();

                UploadObject.uploadObject("voitra", "voitra-stt", "GCP_AUDIO/" + obj_name, SOURCE_PATH + "/wav_files/" + obj_name);

                resultArray = CloudDiarization.transcribeDiarization(
                        "gs://voitra-stt/GCP_AUDIO/" + obj_name,
                        customLang,
                        "LINEAR16",
                        16000);
            }
            System.gc();
            Runtime.getRuntime().gc();
            return resultArray;
        }
    }


    private static double TimeInSeconds(Duration time) {
        // Concatenates seconds and nanoseconds.
        return Double.parseDouble(
                new DecimalFormat("0.000")
                        .format((time.getSeconds() + (time.getNanos() / 1E+9))));
    }
}