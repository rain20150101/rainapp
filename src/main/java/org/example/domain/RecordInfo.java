package org.example.domain;


import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordInfo {
    String raw;
    String fileName;
    String description;

    String Date;
    double amount;
    Group1 group1;
    Group2 group2;
    String searchKey;
    double groupSum;
    int groupCount;

    public RecordInfo getNewRecordInfo(RecordInfo recordInfo){
        return RecordInfo.builder()
                .raw(recordInfo.raw)
                .fileName(recordInfo.fileName)
                .description(recordInfo.description)
                .amount(recordInfo.amount)
                .group1(recordInfo.group1)
                .group2(recordInfo.group2)
                .build();

    }

    public String toALine(){
//        return String.format("%s|%s|%s|%s",raw, group1 == null ? "": group1.toString(),
//                group2 == null ? "": group2.toString(), searchKey);
        return String.format("%s|%s|%s|%s",raw, group1 == null ? "": group1.toString(),
                group2 == null ? "": group2.toString(), searchKey);

    }

    public String toLineInGeneral(){
//        return String.format("%s|%s|%s|%s",raw, group1 == null ? "": group1.toString(),
//                group2 == null ? "": group2.toString(), searchKey);
        return String.format("%s|%s|%s|%s|%.2f|%s",fileName,description, group1 == null ? "": group1.toString(),
                group2 == null ? "": group2.toString(),amount, searchKey);

    }


    public String toLineWithGroupAmmount(){
//        return String.format("%s|%s|%s|%s",raw, group1 == null ? "": group1.toString(),
//                group2 == null ? "": group2.toString(), searchKey);
        return String.format("%s|%s|%s|%s|%.2f|%.2f|%s",fileName,description, group1 == null ? "": group1.toString(),
                group2 == null ? "": group2.toString(),amount, groupSum,searchKey);

    }

    public String toLineWithGroupSumAndCount(){
//        return String.format("%s|%s|%s|%s",raw, group1 == null ? "": group1.toString(),
//                group2 == null ? "": group2.toString(), searchKey);
        return String.format("%s|%s|%s|%s|%.2f|%.2f|%d|%s",fileName,description, group1 == null ? "": group1.toString(),
                group2 == null ? "": group2.toString(),amount, groupSum,groupCount, searchKey);

    }


}
