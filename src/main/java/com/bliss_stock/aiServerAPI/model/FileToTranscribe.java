package com.bliss_stock.aiServerAPI.model;

public class FileToTranscribe {
    private Speech2text parameters;
    private String wf;
    private InitialResult s;
    private int currentFileNumber;
    private int totalFileNumber;

    public FileToTranscribe(Speech2text parameters, InitialResult success, String wf) {
        this.parameters = parameters;
        this.s = success;
        this.wf = wf;
    }
    
    public FileToTranscribe(Speech2text parameters, InitialResult success, String wf, int currentFileNumber, int totalFileNumber){
        this(parameters, success, wf);
        this.currentFileNumber = currentFileNumber;
        this.totalFileNumber = totalFileNumber;
    }

    public InitialResult getSuccess() {
        return s;
    }

    public Speech2text getParameters() {
        return parameters;
    }

    public String getWf() {
        return wf;
    }
    
    public int getCurrentFileNumber() {
        return currentFileNumber;
    }
    
    public int getTotalFileNumber(){
        return totalFileNumber;
    }
}
