package org.example.service;

import org.example.domain.*;

import java.util.ArrayList;
import java.util.List;

public class ChaseService {


    //current raw header: Card,Transaction Date,Post Date,Description,Category,Type,Amount,Memo
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

    //business cards: fileName|Card|Transaction Date|Post Date|Description|Category|Type|Amount|Memo (9 fields)
    //personal cards: fileName|Transaction Date|Post Date|Description|Category|Type|Amount|Memo (8 fields, no Card column)
    public List<RecordInfo> getRecordInfo(List<String> lines){
        RecordConfigShare config = new RecordConfigShare();
        List<String> outLines = new ArrayList<>();
        List<RecordInfo> recordInfos = new ArrayList<>();

        int index = 0;
        for (String line : lines) {

            try {


                String[] strArray = line.split("\\|", -1); //-1: keep a trailing empty Memo field so length reliably reflects whether Card is present
//            String[] strArray = line.split(",");
                boolean hasCardColumn = strArray.length >= 9;
                int descriptionIndex = hasCardColumn ? 4 : 3;
                int amountIndex = hasCardColumn ? 7 : 6;
                ConfigInfo configInfo = config.getGroupInfo(strArray[descriptionIndex]);

                RecordInfo recordInfo = RecordInfo.builder()
                        .raw(line)
                        .fileName(strArray[0])
                        .description(strArray[descriptionIndex])
                        .amount(Double.parseDouble(strArray[amountIndex]))
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
