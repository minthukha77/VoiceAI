package com.bliss_stock.aiServerAPI.gcp;

import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class ListObjectsWithPrefix {
    public static String[] addBlob(int n, String arr[], String x)
    {
        int i;

        // create a new array of size n+1
        String newarr[] = new String[n + 1];

        // insert the elements from
        // the old array into the new array
        // insert all elements till n
        // then insert x at n+1
        for (i = 0; i < n; i++)
            newarr[i] = arr[i];

        newarr[n] = x;

        return newarr;
    }
    public static String[] listObjectsWithPrefix(
            String projectId, String bucketName, String directoryPrefix) throws IOException {
        // The ID of your GCP project
        // String projectId = "your-project-id";

        // The ID of your GCS bucket
        // String bucketName = "your-unique-bucket-name";

        // The directory prefix to search for
        // String directoryPrefix = "myDirectory/"

        String SOURCE_PATH = "/usr/local/src/static/key.json";
//        String SOURCE_PATH = System.getProperty("user.dir") + "/src/static/key.json";
    
        GoogleCredentials googleCredentails = ServiceAccountCredentials.fromStream(new FileInputStream(SOURCE_PATH));

        Storage storage = StorageOptions.newBuilder().setCredentials(googleCredentails)
                .setProjectId(projectId).build().getService();
        /**
         * Using the Storage.BlobListOption.currentDirectory() option here causes the results to display
         * in a "directory-like" mode, showing what objects are in the directory you've specified, as
         * well as what other directories exist in that directory. For example, given these blobs:
         *
         * <p>a/1.txt a/b/2.txt a/b/3.txt
         *
         * <p>If you specify prefix = "a/" and don't use Storage.BlobListOption.currentDirectory(),
         * you'll get back:
         *
         * <p>a/1.txt a/b/2.txt a/b/3.txt
         *
         * <p>However, if you specify prefix = "a/" and do use
         * Storage.BlobListOption.currentDirectory(), you'll get back:
         *
         * <p>a/1.txt a/b/
         *
         * <p>Because a/1.txt is the only file in the a/ directory and a/b/ is a directory inside the
         * /a/ directory.
         */
        Page<Blob> blobs =
                storage.list(
                        bucketName,
                        Storage.BlobListOption.prefix(directoryPrefix),
                        Storage.BlobListOption.currentDirectory());
        String[] blobArr = {};
        for (Blob blob : blobs.iterateAll()) {
            int blobSize = blobArr.length;
            if(!Objects.equals(blob.getName(), "GCP_AUDIO/")) {
                blobArr = addBlob(blobSize, blobArr, blob.getName());
            }
        }
        System.gc();
        Runtime.getRuntime().gc();
        return blobArr;
    }
}