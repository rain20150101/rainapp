package org.example.service;

import org.example.domain.GroupRecord;
import org.example.domain.GroupSummary;
import org.example.domain.RecordInfo;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HelperService {

    public static String getTime(){
        Instant instant = Instant.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        return formatter.format(instant.atZone(ZoneId.systemDefault()));

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
