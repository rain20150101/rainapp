package org.example;

import org.example.domain.AccountVender;
import org.example.service.FileService;
import org.example.service.HelperService;

import java.io.IOException;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) throws IOException {

//        String year="2023"
        String year= "2025";
                ;


        FileService fileService = new FileService();
        String timeStamp = HelperService.getTime();
        String outFilePrefix = "out_" + timeStamp;
        fileService.processFiles();
//        fileService.processRawFile("wf_" + year, AccountVender.wellsfargo, outFilePrefix);;
//        fileService.processRawFile("DFS_" + year, AccountVender.discovery, outFilePrefix);;
//        fileService.processRawFile("Chase_" + year, AccountVender.chase, outFilePrefix);;
//
        fileService.summaryInOne(outFilePrefix);
//        fileService.readFile(fileNames[0]);
    }
}