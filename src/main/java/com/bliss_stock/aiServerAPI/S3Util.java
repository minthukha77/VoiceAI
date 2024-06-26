package com.bliss_stock.aiServerAPI;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.bliss_stock.aiServerAPI.controller.Speech2textController;

import java.io.File;

public class S3Util {

    public static void uploadObject(String filePath) {
        Regions clientRegion = Regions.AP_NORTHEAST_1;
        String bucketName = "processedaudiofile";
        String fileName = filePath.split("/")[filePath.split("/").length - 1];
        String stringObjKeyName = fileName;
        String fileObjKeyName = stringObjKeyName;
        try {
            //This code expects that you have AWS credentials set up per:
            // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion)
                    .build();

            // Upload a text string as a new object.
            s3Client.putObject(bucketName, stringObjKeyName, "Uploaded String Object");
            // Upload a file as a new object with ContentType and title specified.
            PutObjectRequest request = new PutObjectRequest(bucketName, fileObjKeyName, new File(filePath));
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("plain/text");
            metadata.addUserMetadata("title", filePath);
            request.setMetadata(metadata);
            s3Client.putObject(request);
            Speech2textController.myLog.logger.info("File " + filePath + " uploaded to S3 bucket " + bucketName + " as " + fileObjKeyName);
            System.out.println("File " + filePath + " uploaded to S3 bucket " + bucketName + " as " + fileObjKeyName);
        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            Speech2textController.myLog.logger.info("Failed to upload " + fileObjKeyName + " to S3 bucket, " + e.getMessage());
            System.out.println("Failed to upload " + fileObjKeyName + " to S3 bucket, " + e.getMessage());
            e.printStackTrace();
        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            Speech2textController.myLog.logger.info("Failed to upload " + fileObjKeyName + " to S3 bucket, " + e.getMessage());
            System.out.println("Failed to upload " + fileObjKeyName + " to S3 bucket, " + e.getMessage());
            e.printStackTrace();
        }
    }
}

