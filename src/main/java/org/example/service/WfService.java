package org.example.service;

import org.example.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WfService {



    //filename wf_yyyy.....csv
    //current raw header: "DATE","DESCRIPTION","AMOUNT","CHECK #","STATUS"
    List<String> putALlInOneFile(String fileNameWoPath, List<String> lines){
        List<String> oneFileLines = new ArrayList<>();

        if (!lines.isEmpty()) {
            lines.remove(0);//remove header
        }

        for (String line : lines) {
           if (line.startsWith("\"")) {
               line =line.substring(1 , line.length());
           }

            if (line.endsWith("\"")) {
                line =line.substring(0 , line.length()-1);
            }
            String[] fields = line.split("\",\"", -1);
            List<String> escapedFields = new ArrayList<>();
            for (String field : fields) {
                escapedFields.add(field.replace("|", "_"));
            }
            String lineNewValue = fileNameWoPath + "|" + String.join("|", escapedFields);
            oneFileLines.add(lineNewValue);

        }
        return oneFileLines;
    }

    //fileName|DATE|DESCRIPTION|AMOUNT|CHECK #|STATUS
    public List<RecordInfo> getRecordInfo(List<String> lines){
        RecordConfigWf config = new RecordConfigWf();
        List<String> outLines = new ArrayList<>();
        List<RecordInfo> recordInfos = new ArrayList<>();

        for (String line : lines) {
            String[] strArray = line.split("\\|");
            ConfigInfo configInfo;
            try {
                 configInfo = config.getGroupInfo(strArray[2]);
            }
            catch (Exception ex) {
                System.out.println("lines:" + lines);
                throw ex;
            }

            RecordInfo recordInfo = RecordInfo.builder()
                    .raw(line)
                    .fileName(strArray[0])
                    .description(strArray[2])
                    .amount(Double.parseDouble(strArray[3]))
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
