package org.example.service;

import org.example.domain.*;

import java.util.ArrayList;
import java.util.List;

public class CitiService {


    List<String> putALlInOneFile(String fileNameWoPath, List<String> lines){
        List<String> oneFileLines = new ArrayList<>();

        lines.remove(0);//remove header
        for (String line : lines) {
            String[] fields = line.split(",", -1);
            List<String> escapedFields = new ArrayList<>();
            for (String field : fields) {
                escapedFields.add(field.replace("|", "_"));
            }
            String lineNewValue = fileNameWoPath + "|" + String.join("|", escapedFields);
            oneFileLines.add(lineNewValue);

        }
        return oneFileLines;
    }

    //fileName|Status|Date|Description|Debit|Credit|Member Name
    //citi has no single signed Amount column - Debit (money out, negative) and
    //Credit (money in, positive) are separate columns, only one of which is set.
    public List<RecordInfo> getRecordInfo(List<String> lines){
        RecordConfigShare config = new RecordConfigShare();
        List<String> outLines = new ArrayList<>();
        List<RecordInfo> recordInfos = new ArrayList<>();

        int index = 0;
        for (String line : lines) {
            try {
                String[] strArray = line.split("\\|", -1);
                ConfigInfo configInfo = config.getGroupInfo(strArray[3]);

                String debit = strArray[4];
                String credit = strArray[5];
                double amount = (credit != null && !credit.isBlank())
                        ? Double.parseDouble(credit.replace(",", ""))
                        : -Double.parseDouble(debit.replace(",", ""));

                RecordInfo recordInfo = RecordInfo.builder()
                        .raw(line)
                        .fileName(strArray[0])
                        .description(strArray[3])
                        .amount(amount)
                        .group1( configInfo.getGroup1()== null ? Group1.nullValue: configInfo.getGroup1())
                        .group2(configInfo.getGroup2()== null ? Group2.nullvalue : configInfo.getGroup2())
                        .searchKey(configInfo.getSearchKey())
                        .build();
//            outLines.add(recordInfo.toALine());
                recordInfos.add(recordInfo);
            } catch (Exception ex) {
                System.out.println("index:" + index + "_line:" + line);
            }
            index++;

        }
        return recordInfos;

    }

}
