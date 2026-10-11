package org.example;

import org.example.service.FileService;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        FileService fileService = new FileService();
        fileService.processFiles();
    }
}
