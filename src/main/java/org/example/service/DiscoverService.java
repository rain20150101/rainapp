package org.example.service;

import org.example.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscoverService {
    //file format DFS_
//    current raw header: Trans. Date,Description,Amount,Category
//01/01/2023,"TST* PARIS BAGUETTE - ZI IRVINE CA",21.75,"Restaurants"
//    fileName|Trans. Date|Description|Amount|Category
    // common outside double quotes string
    Pattern pattern = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*(?![^\"]*\"))");


    List<String> putALlInOneFile(String fileNameWoPath, List<String> lines){

        List<String> oneFileLines = new ArrayList<>();

        lines.remove(0);//remove header


        for (String line : lines) {
            oneFileLines.add( fileNameWoPath+ "|" + reformatRaw(line));

        }
        return oneFileLines;
    }

    public List<RecordInfo> getRecordInfo(List<String> lines){
//        DFS_2022_activity.csv|01/01/2022|01/01/2022|CHEVRON 0306957 IRVINE CA00306957 5723073|27.32|Gasoline
        RecordConfigDiscover config = new RecordConfigDiscover();
        List<String> outLines = new ArrayList<>();
        List<RecordInfo> recordInfos = new ArrayList<>();

        int index = 0;
        for (String line : lines) {
            try {
                String[] strArray = line.split("\\|");
                ConfigInfo configInfo = config.getGroupInfo(strArray[2]);

                RecordInfo recordInfo = RecordInfo.builder()
                        .raw(line)
                        .fileName(strArray[0])
                        .description(strArray[2])
                        .amount(-Double.parseDouble(strArray[3].replace(",", ""))) //discover raw data : expense is +, amount may have thousands-separator commas
                        .group1(configInfo.getGroup1() == null ? Group1.nullValue : configInfo.getGroup1())
                        .group2(configInfo.getGroup2() == null ? Group2.nullvalue : configInfo.getGroup2())
                        .searchKey(configInfo.getSearchKey())
                        .build();
//            outLines.add(recordInfo.toALine());
                recordInfos.add(recordInfo);
            } catch (Exception ex) {
                //raw CSV has description fields with embedded newlines, which
                //Files.readAllLines splits across physical lines - those fragments
                //fail to parse here and are skipped rather than aborting the file.
                System.out.println("index:" + index + "_line:" + line);
            }
            index++;

        }
        return recordInfos;

    }


   public String reformatRaw(String input) {

        // Define a regex pattern to match commas outside of double quotes


       List<String> items = new ArrayList<>();
        // Use a Matcher to split the string
        Matcher matcher = pattern.matcher(input);
        int start = 0;

        while (matcher.find()) {
            String token = input.substring(start, matcher.start()).trim();
            items.add(token);
            start = matcher.end();
        }

        items.add(input.substring(start).trim());


       List<String> trimmedList = new ArrayList<>();
       for (String item : items) {
           if (item.startsWith("\"") && item.endsWith("\"")) {
               item = item.substring(1, item.length() - 1);
           }
           trimmedList.add(item.replace("|", "_"));
       }
        return  String.join("|", trimmedList);
    }


}
