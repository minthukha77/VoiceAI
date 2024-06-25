
package com.bliss_stock.aiServerAPI.model;

import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

public class Speech2text implements Serializable {

    private final String number, lang, token, diarization, audio_id, callback, wait;
    private final MultipartFile file;

    public Speech2text(MultipartFile file, String number, String lang, String token, String diarization, String audio_id, String callback, String wait) {
        this.file = file;
        this.number = number;
        this.lang = lang;
        this.token = token;
        this.diarization = diarization;
        this.audio_id = audio_id;
        this.callback = callback;
        this.wait = wait;
    }

    public MultipartFile getFile() {
        return file;
    }

    public String getNumber() {
        return number;
    }

    public String getLang() {
        return lang;
    }

    public String getToken() {
        return token;
    }

    public String getDiarization() {
        return diarization;
    }

    public String getAudio_id() {
        return audio_id;
    }

    public String getCallback() {
        return callback;
    }

    public String getWait() {
        return wait;
    }

}