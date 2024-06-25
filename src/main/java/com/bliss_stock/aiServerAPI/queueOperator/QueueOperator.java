package com.bliss_stock.aiServerAPI.queueOperator;

import com.bliss_stock.aiServerAPI.common.AutoLogger;
import com.bliss_stock.aiServerAPI.common.Log;
import com.bliss_stock.aiServerAPI.common.ReadableBytesConverter;
import com.bliss_stock.aiServerAPI.model.FileToTranscribe;
import com.bliss_stock.aiServerAPI.model.InitialResult;
import com.bliss_stock.aiServerAPI.model.Speech2text;
import com.bliss_stock.aiServerAPI.webhook.WebhookRunnable;
import org.springframework.web.servlet.tags.ParamAware;

import java.io.File;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class QueueOperator {

    public Queue<FileToTranscribe> gcsUris = new LinkedList<>();

    private Queue<FileToTranscribe> shortQueue = new LinkedList<>();
    private boolean shortQueueOperating = false;

    private Queue<FileToTranscribe> longQueue = new LinkedList<>();

    public HashMap<Integer, Boolean> queueStatus = new HashMap<>();
    public int threadCount = 0;
    public int longThreadCount = 0;
    protected int limit;
    protected Log myLog;

    /*
     * 1. The Log.
     * 2. Thread limit.
     * 3. The Queue.
     * */
    public QueueOperator(Log myLog, int limit) {
        this.myLog = myLog;
        this.limit = limit;
        for (int i = 1; i <= limit; i++) {
            this.queueStatus.put(i, true); // true means vacant.
        }
    }

    public void addToLongQueue(FileToTranscribe fileToTranscribe) {

        Speech2text params = fileToTranscribe.getParameters();
        InitialResult success = fileToTranscribe.getSuccess();

        File[] files = (new File(fileToTranscribe.getWf())).listFiles();

        if (files != null) {

            int totalNumber = files.length;

            for (File file : files) {
                String[] fileInfo = file.getName().split("_");//Eg. 1_audio_123.wav
                int fileNumber = Integer.parseInt(fileInfo[0]);
                String fileName = file.getName();
                longQueue.add(
                        new FileToTranscribe(
                                params, success, fileName, fileNumber, totalNumber
                        )
                );
            }
        } else {
            System.out.println("NO DIRECTORY AT: " + fileToTranscribe.getWf());
        }
        operateLongQueue();
    }

    public void addToShortQueue(FileToTranscribe fileToTranscribe) {
        shortQueue.add(fileToTranscribe);
        operateShortQueue();
    }

    public void operateShortQueue() {

        System.out.println("\n Operating Short Queue......................");

        if
        (
                shortQueue.size() == 0
                        || threadCount >= this.limit
                        || (shortQueueOperating && shortQueue.size() < longQueue.size())
        ) {
            System.out.println("No New Thread");
            return;
        }


        FileToTranscribe objAtHead = shortQueue.remove();
        String fileSize = ReadableBytesConverter.humanReadableByteCountBin(objAtHead.getParameters().getFile().getSize());

        synchronized (queueStatus){
            int vacantQueueIndex = computeThreadNumber();
            AutoLogger autoLogger = new AutoLogger(vacantQueueIndex, objAtHead.getParameters().getAudio_id(), this.myLog, fileSize);
            try {
                (new Thread(
                   new WebhookRunnable(
//               objAtHead.getParameters(), objAtHead.isSuccess(), objAtHead.getWf(), autoLogger, this, vacantQueueIndex))
                      objAtHead.getParameters(),
                      objAtHead.getSuccess(),
                      objAtHead.getWf(),
                      autoLogger,
                      this,
                      vacantQueueIndex,
                      false))
                ).start();
                threadCount += 1;
            } catch (ParseException e) {
                myLog.logger.info("QueueOperator.java, operateShortQueue() Error: " + e.getMessage());
                throw new RuntimeException(e);
            }
        
        
        
        }
        

//         (new Thread(new Mockup(vacantQueueIndex, autoLogger, this))).start();


    }

    public void operateLongQueue() {

        System.out.println("\n Operating Long Queue......................");

        if
        (
                longQueue.size() == 0
                        || threadCount >= this.limit
                        || (longThreadCount >= 3)
        ) {
            System.out.println("No New Thread");
            return;
        }
    
        int vacantQueueIndex;
        synchronized (queueStatus) {
            vacantQueueIndex = computeThreadNumber();
        }

        System.out.println("\n New Long File Thread Added");

        FileToTranscribe objAtHead = longQueue.remove();
        String fileSize = ReadableBytesConverter.humanReadableByteCountBin(objAtHead.getParameters().getFile().getSize());

        AutoLogger autoLogger = new AutoLogger(vacantQueueIndex, objAtHead.getParameters().getAudio_id() + "_" + objAtHead.getCurrentFileNumber(), this.myLog, fileSize);
         try{
            (new Thread(
                    new WebhookRunnable(
//               objAtHead.getParameters(), objAtHead.isSuccess(), objAtHead.getWf(), autoLogger, this, vacantQueueIndex))
                            objAtHead.getParameters(),
                            objAtHead.getSuccess(),
                            objAtHead.getWf(),
                            autoLogger,
                            this,
                            vacantQueueIndex,
                            true,
                            objAtHead.getCurrentFileNumber(),
                            objAtHead.getTotalFileNumber()))
            ).start();
         }
        catch (ParseException e){
            myLog.logger.info("QueueOperator.java, operateLongQueue() Error: " + e.getMessage());
            throw new RuntimeException(e);
        }

//         (new Thread(new Mockup(vacantQueueIndex, autoLogger, this))).start();

        threadCount += 1;
        operateQueue();

    }

    public void operateQueue() {
        operateLongQueue();
        operateShortQueue();
    }

    public int computeThreadNumber() {
        // Idk who wrote this, so I am not deleting it. Please delete this if not needed anymore.
//        vacantQueueIndex = 1;
//        Boolean vacantQueueBool = this.queueStatus.get(vacantQueueIndex);
//        System.out.println("computeThreadNumber(): " + vacantQueueBool);
//        while (!vacantQueueBool) {
//            vacantQueueIndex++;
//        }
        
        int tmpIndex = 1;
        while (!queueStatus.get(tmpIndex)){
            if(tmpIndex<4){
                tmpIndex++;
            } else {
                System.out.println("Threads are at their limit.");
                tmpIndex = 0;
            }
        }
        queueStatus.put(tmpIndex, false);
        return tmpIndex;
    }
}


class Mockup implements Runnable {
    private final int vacantQueueIndex;
    private final AutoLogger autoLogger;
    private final QueueOperator diarizer;

    public Mockup(int vacantQueueIndex, AutoLogger autoLogger, QueueOperator diarizer) {
        this.vacantQueueIndex = vacantQueueIndex;
        this.autoLogger = autoLogger;
        this.diarizer = diarizer;
    }


    @Override
    public void run() {
//        Random random = new Random();
//        autoLogger.logStart();
//        autoLogger.logMemory();
//
//        try {
//            Thread.sleep(random.nextInt(10) * 1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        autoLogger.gcpCompleted();
//        synchronized (diarizer) {
//            diarizer.queueStatus.put(vacantQueueIndex, true);
//            diarizer.threadCount -= 1;
////               autoLogger.queStatus(diarizer.queueStatus, vacantQueueIndex, diarizer.threadCount);
//        }
//        autoLogger.logEnd("mockup result", "true");
    }
}
