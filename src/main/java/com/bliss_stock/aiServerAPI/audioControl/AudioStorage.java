package com.bliss_stock.aiServerAPI.audioControl;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AudioStorage {
    public static void delete(String filePath) {
        File file = new File(filePath);
        // store all the paths of files and folders present
        // inside directory
        if(file.isDirectory()){
            for (File subfile : file.listFiles()) {
                // recursiley call function to empty subfolder
                if (subfile.isDirectory()) {
                    delete(subfile.getAbsolutePath());
                }
                // delete files and empty subfolders
                subfile.delete();
            }
            if(file.listFiles().length == 0){
                file.delete();
            }
        }
        else {
            file.delete();
        }
        System.out.println(filePath + " deleted.");
    }
    public static String zipper(Boolean isFile, String filePath) throws IOException {
        String zippedFilePath = isFile ? filePath+".zip" : filePath.substring(0, filePath.lastIndexOf("/")) + ".zip";
        FileOutputStream fos = new FileOutputStream(zippedFilePath);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(filePath);
        System.out.println("file name: " + fileToZip.getName());
        zip(fileToZip, fileToZip.getName(), zipOut);
        zipOut.close();
        fos.close();
        return zippedFilePath;
    }
    public static void zip(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zip(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }
}
