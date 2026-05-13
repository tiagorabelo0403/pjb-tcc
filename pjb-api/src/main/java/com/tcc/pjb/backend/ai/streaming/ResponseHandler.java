package com.tcc.pjb.backend.ai.streaming;


public interface ResponseHandler {
    void onPartial(String chunk);
    void onComplete();
    void onError(Exception e);
}