package org.example.service;

import org.apache.commons.lang3.StringUtils;
import org.example.Constant.TaxYear;
import org.example.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.Constant.TaxYear.taxYear;

public class WfManualInputService {



    //filename wfmanual_yyyy.....csv
    //"10/16/2022","156.61","*","","AUTOMATIC PAYMENT - THANK YOU"
    List<String> putALlInOneFile(String fileNameWoPath, List<String> lines) throws Exception{
        List<String> oneFileLines = new ArrayList<>();

        for (String line : lines) {
            String[] items = line.split("\\|",-1);

//           if (line.startsWith("\"")) {
//               line =line.substring(1 , line.length());
//           }
//
//            if (line.endsWith("\"")) {
//                line =line.substring(0 , line.length()-1);
//            }
//            line.replace("\",\"", "|");
//            String lineNewValue  = fileNameWoPath+ "|" + line.replace("\",\"", "|");

            int spaceIndex = line.trim().indexOf(" ");
            if (spaceIndex !=-1
                    &&  line.trim().substring(0, spaceIndex).contains("/")){
                String dateValue = line.trim().substring(0, spaceIndex) +"/"+ taxYear;
                if (StringUtils.isBlank(items[1]) &&StringUtils.isBlank(items[2]) ) {
                    throw new Exception("wfmanualfile amount is empty:" +line);
                }
                String amount = StringUtils.isNotBlank(items[1])
                        ? items[1].trim()
                        : "-" + items[2].trim();
                String lineNewValue  = fileNameWoPath+ "|"
                        + dateValue + "|"  //0
                        + amount.replace(",","") + "|"
                        + "||"
                        + items[0].replace("|", "_");

                oneFileLines.add(lineNewValue);

            } else {
                System.out.println("error input format in wf manual:" + line);
                throw new Exception("error input format in wfmanual" +line);
            }


        }
        return oneFileLines;
    }

    public List<RecordInfo> getRecordInfo(List<String> lines){
        RecordConfigWf config = new RecordConfigWf();
        List<String> outLines = new ArrayList<>();
        List<RecordInfo> recordInfos = new ArrayList<>();

        for (String line : lines) {
            String[] strArray = line.split("\\|");
            ConfigInfo configInfo;
            try {
                 configInfo = config.getGroupInfo(strArray[5]);
            }
            catch (Exception ex) {
                System.out.println("lines:" + lines);
                throw ex;
            }

            RecordInfo recordInfo = RecordInfo.builder()
                    .raw(line)
                    .fileName(strArray[0])
                    .description(strArray[5])
                    .amount(Double.parseDouble(strArray[2]))
                    .group1( configInfo.getGroup1()== null ? Group1.nullValue: configInfo.getGroup1())
                    .group2(configInfo.getGroup2()== null ? Group2.nullvalue : configInfo.getGroup2())
                    .searchKey(configInfo.getSearchKey())
                    .build();
//            outLines.add(recordInfo.toALine());
            recordInfos.add(recordInfo);


        }
        return recordInfos;

    }

    public List<String> processSummary(List<RecordInfo> recordInfos){

        Map<String, GroupSummary> groupedSummary = recordInfos.stream()
                .collect(Collectors.groupingBy(
                        x  -> getGroupedKey(x),
                        Collectors.collectingAndThen(Collectors.summarizingDouble(RecordInfo::getAmount),
                                summary -> new GroupSummary(summary.getSum(), summary.getCount())
                        )
                ));
        List<GroupRecord> groupRecords = new ArrayList<>();
        groupedSummary.forEach((key, summary) -> groupRecords.add(GroupRecord.builder()
                .groupKey(key)
                .sum(summary.getSum())
                .count(summary.getCount())
                .build()));


        List<String> GroupRecordLines = new ArrayList<>();
        groupedSummary.forEach((key, summary) ->
                GroupRecordLines.add(String.format("%s|%s|%s", key,summary.getSum(), summary.getCount())));


        return GroupRecordLines;

    }



    public String getGroupedKey (RecordInfo recordInfo)
    {
        return  (recordInfo.getSearchKey() == null ?"null" : recordInfo.getSearchKey()) + "|"
                + (recordInfo.getGroup1() == null ? "null": recordInfo.getGroup1()) +
                "|"+ (recordInfo.getGroup2() == null ? "null": recordInfo.getGroup2());

    }


}
