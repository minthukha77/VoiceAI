package com.bliss_stock.aiServerAPI.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.stereotype.Service;

@Service
public class AsyncService {
    private ExecutorService executorService;

    @PostConstruct
    private void create() {
        executorService = Executors.newSingleThreadExecutor();
    }

    public void process(Runnable operation) {
        // no result operation
        executorService.submit(operation);
    }


    @PreDestroy
    private void destroy() {
        executorService.shutdown();
    }
}
