package org.example.service;

import org.example.domain.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WfServiceBk01 {
    List<String> putALlInOneFile(File file, List<String> lines){
        List<String> oneFileLines = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith("\"")) {
                line =line.substring(1 , line.length());
            }

            if (line.endsWith("\"")) {
                line =line.substring(0 , line.length()-1);
            }
            line.replace("\",\"", "|");
            String lineNewValue = file.getName() + "|" + line.replace("\",\"", "|");
            oneFileLines.add(lineNewValue);

        }
        return oneFileLines;
    }


    public List<String> processRecord(List<String> lines){
        RecordConfigWf config = new RecordConfigWf();
        List<String> outLines = new ArrayList<>();

        for (String line : lines) {
            String[] strArray = line.split("\\|");
            ConfigInfo configInfo = config.getGroupInfo(strArray[5]);

            RecordInfo recordInfo = RecordInfo.builder()
                    .raw(line)
                    .amount(Double.parseDouble(strArray[2]))
                    .group1( configInfo.getGroup1())
                    .group2(configInfo.getGroup2())
                    .searchKey(configInfo.getSearchKey())
                    .build();
            outLines.add(recordInfo.toALine());


        }
        return outLines;

    }


    public List<String> processSummary(List<String> lines){
        RecordConfigWf config = new RecordConfigWf();
        List<String> outLines = new ArrayList<>();
        List<RecordInfo> recordInfos = new ArrayList<>();

        for (String line : lines) {
            String[] strArray = line.split("\\|");
            ConfigInfo configInfo = config.getGroupInfo(strArray[5]);

            RecordInfo recordInfo = RecordInfo.builder()
                    .raw(line)
                    .amount(Double.parseDouble(strArray[2]))
                    .group1( configInfo.getGroup1())
                    .group2(configInfo.getGroup2())
                    .searchKey(configInfo.getSearchKey())
                    .build();
            recordInfos.add(recordInfo);



        }


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
