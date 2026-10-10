package org.example.service;

import org.example.domain.*;

import java.util.ArrayList;
import java.util.List;

public class ChaseService {


    List<String> putALlInOneFile(String fileNameWoPath, List<String> lines){
        List<String> oneFileLines = new ArrayList<>();

        lines.remove(0);//remove header
        for (String line : lines) {
//            if (line.startsWith("\"")) {
//                line =line.substring(1 , line.length());
//            }
//
//            if (line.endsWith("\"")) {
//                line =line.substring(0 , line.length()-1);
//            }
//            line.replace("\",\"", "|");
            String lineNewValue  = fileNameWoPath+ "|" + line.replace(",", "|");
            oneFileLines.add(lineNewValue);

        }
        return oneFileLines;
    }

    public List<RecordInfo> getRecordInfo(List<String> lines){
        RecordConfigChase config = new RecordConfigChase();
        List<String> outLines = new ArrayList<>();
        List<RecordInfo> recordInfos = new ArrayList<>();

        int index = 0;
        for (String line : lines) {

            try {


                String[] strArray = line.split("\\|");
//            String[] strArray = line.split(",");
                ConfigInfo configInfo = config.getGroupInfo(strArray[3]);

                RecordInfo recordInfo = RecordInfo.builder()
                        .raw(line)
                        .fileName(strArray[0])
                        .description(strArray[3])
                        .amount(Double.parseDouble(strArray[6]))
                        .group1(configInfo.getGroup1() == null ? Group1.nullValue : configInfo.getGroup1())
                        .group2(configInfo.getGroup2() == null ? Group2.nullvalue : configInfo.getGroup2())
                        .searchKey(configInfo.getSearchKey())
                        .build();
//            outLines.add(recordInfo.toALine());
                recordInfos.add(recordInfo);
            }catch(Exception e) {
                System.out.println("index:" +String.valueOf(index) + "_line:" + line);
            }
            index ++;


        }
        return recordInfos;

    }

}
