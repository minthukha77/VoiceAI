package com.bliss_stock.aiServerAPI.gcp;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.longrunning.OperationTimedPollAlgorithm;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.retrying.TimedRetryAlgorithm;
import com.google.cloud.speech.v1.*;

import com.bliss_stock.aiServerAPI.audioControl.AudioConverter;
import org.threeten.bp.Duration;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.bliss_stock.aiServerAPI.controller.Speech2textController.myLog;
import static com.bliss_stock.aiServerAPI.gcp.GcpUtil.replaceSpecialChars;

public class CloudAsync {
  public static String asyncRecognizeGcs(
          String gcsUri,
          String customLang,
          String customEncoding,
          int customHertz)
          throws Exception {
    String transcriptionResult = "";
    try {

      SpeechSettings.Builder speechSettings = SpeechSettings.newBuilder();
      TimedRetryAlgorithm timedRetryAlgorithm =
              OperationTimedPollAlgorithm.create(
                      RetrySettings.newBuilder()
                              .setInitialRetryDelay(Duration.ofMillis(500L))
                              .setRetryDelayMultiplier(1.5)
                              .setMaxRetryDelay(Duration.ofMillis(5000L))
                              .setInitialRpcTimeout(Duration.ZERO)
                              .setRpcTimeoutMultiplier(1.0)
                              .setMaxRpcTimeout(Duration.ZERO)
                              .setTotalTimeout(Duration.ofHours(24L))
                              .build());
      speechSettings.longRunningRecognizeOperationSettings().setPollingAlgorithm(timedRetryAlgorithm);

      SpeechClient speech = SpeechClient.create(speechSettings.build());

      RecognitionConfig config =
              RecognitionConfig.newBuilder()
                      .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                      .setLanguageCode(customLang)
                      .setSampleRateHertz(customHertz)
                      .setEnableAutomaticPunctuation(true)
                      .build();
      RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();

      OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response =
              speech.longRunningRecognizeAsync(config, audio);


      List<SpeechRecognitionResult> results = response.get().getResultsList();

      for (SpeechRecognitionResult result : results) {
        SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
        transcriptionResult += alternative.getTranscript();
      }
      System.gc();
      Runtime.getRuntime().gc();
      return transcriptionResult;
    }
    catch (ExecutionException e) {
      myLog.logger.info("GCP ERROR in CloudAsync.java: " + e.getMessage());
      String error = String.valueOf(e.getCause());
      if (error.contains("InvalidArgumentException")){
        myLog.logger.info("Handling InvalidArgumentException");
        String[] array = gcsUri.split("GCP_AUDIO/");
        String obj_name = array[1];

        String SOURCE_PATH = "/usr/local/src/static";
        DownloadObject d_obj = new DownloadObject();
        d_obj.downloadObject("voitra", "voitra-stt", "GCP_AUDIO/" +obj_name, SOURCE_PATH + "/audio_files/"+ obj_name);

        String file_to_convert_from = SOURCE_PATH + "/audio_files/" + obj_name;
        String file_to_convert_to = SOURCE_PATH + "/wav_files/" + obj_name.substring(0, obj_name.lastIndexOf(".")) + ".wav";
        AudioConverter audioConverter = new AudioConverter(file_to_convert_from, file_to_convert_to, 16000);
        audioConverter.convertAudio();

        UploadObject.uploadObject("voitra", "voitra-stt", "GCP_AUDIO/" + obj_name, SOURCE_PATH + "/wav_files/" + obj_name);

        transcriptionResult = CloudAsync.asyncRecognizeGcs(
                "gs://voitra-stt/GCP_AUDIO/" + obj_name,
                customLang,
                "LINEAR16",
                16000);
      }

      System.gc();
      Runtime.getRuntime().gc();
      return replaceSpecialChars(transcriptionResult);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
