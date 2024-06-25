package com.bliss_stock.aiServerAPI.audioControl;

import com.bliss_stock.aiServerAPI.common.Log;
import com.bliss_stock.aiServerAPI.controller.Speech2textController;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


/**
 * SETUP
 * https://www.wikihow.com/Install-FFmpeg-on-Windows
 * https://www.howtoforge.com/tutorial/ffmpeg-audio-conversion/
 */

/*
* CODE EXAMPLE

*  >>> Converting Audio Files >>>
   String file_to_convert_from = "C:/Users/DELL/eclipse-workspace/gcpSandbox/src/main/resources/Recording.flac";
   String file_to_convert_to = "C:/Users/DELL/eclipse-workspace/gcpSandbox/src/main/resources/wavFile.wav";
   AudioConverter audioConverter = new AudioConverter();
   audioConverter.convertAudio(file_to_convert_from, file_to_convert_to, 16000);

*  >>> Printing Frequency of A File >>>
   String file = "C:/Users/DELL/eclipse-workspace/gcpSandbox/src/main/resources/Recording.flac";
   AudioConverter audioConverter = new AudioConverter();
   System.out.println(audioConverter.frequencyFromFile(file));

* */

public class AudioConverter {
    private final Runtime runtime;
    private static Log myLog = Speech2textController.myLog;
    private String file_to_convert_from;
    private String file_to_convert_to;
    private int required_frequency;

    public AudioConverter(String file_to_convert_from, String file_to_convert_to, int required_frequency) {
        // Instantiating a runtime object.
        this.runtime = Runtime.getRuntime();
        this.file_to_convert_from = file_to_convert_from;
        this.file_to_convert_to = file_to_convert_to;
        this.required_frequency = required_frequency;
    }

    public String convertAudio() {
        try {
            System.out.println("conversion command: " + "ffmpeg -i " + this.file_to_convert_from + " -c:a pcm_s16le -ar " + this.required_frequency + " -ac 1 " + this.file_to_convert_to);
            // Executing the command using runtime object.
            Process process = this.runtime.exec("ffmpeg -i " + this.file_to_convert_from + " -c:a pcm_s16le -ar " + this.required_frequency + " -ac 1 " + this.file_to_convert_to);
            //ffmpeg -i "{0}" -acodec pcm_s16le -ar 16000 "{1}" -y
            // exitVal 0 means OK, 1 means file not found.
            int exitVal = process.waitFor();
//            System.out.println("Exited with error code "+exitVal);
            if (exitVal == 0) {
                System.out.println("Converted successfully.");
            }
            else {
                System.out.println("wav format conversion failed.");

            }
        } catch (Exception e) {
            myLog.logger.info("AudioConverter.java, convertAudio() Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
        System.gc();
        Runtime.getRuntime().gc();
        return file_to_convert_to;
    }
}

