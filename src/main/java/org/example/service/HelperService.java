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

    //Files.readAllLines splits a CSV on every newline, including ones embedded
    //inside a quoted field (e.g. a multi-line Discover description). A line with
    //an odd number of quote characters means its quoted field isn't closed yet,
    //so fold the following physical line(s) into it until the quotes balance.
    public static List<String> mergeMultilineCsvRecords(List<String> rawLines){
        List<String> merged = new ArrayList<>();
        StringBuilder pending = null;

        for (String line : rawLines) {
            pending = pending == null ? new StringBuilder(line) : pending.append(" ").append(line);

            long quoteCount = pending.chars().filter(c -> c == '"').count();
            if (quoteCount % 2 == 0) {
                merged.add(pending.toString());
                pending = null;
            }
        }
        if (pending != null) {
            //unbalanced quotes at end of file - keep as-is, downstream parsing will skip it
            merged.add(pending.toString());
        }
        return merged;
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
