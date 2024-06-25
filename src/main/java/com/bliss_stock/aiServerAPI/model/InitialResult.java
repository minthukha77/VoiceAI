package com.bliss_stock.aiServerAPI.model;

public class InitialResult {
    private boolean success;
    public InitialResult(boolean success){
        this.success = success;
    }

    public boolean getSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
