package org.example.service;

import org.example.Constant.Cons;
import org.example.Constant.TaxYear;
import org.example.domain.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class FileService {

    HelperService helperService = new HelperService();
    WfManualInputService wfManualInputService = new WfManualInputService();
    WfService wfService = new WfService();
    DiscoverService discoverService = new DiscoverService();
    CitiService citiService = new CitiService();

    ChaseService chaseService = new ChaseService();
    String timeStamp = HelperService.getTime();
    String outFilePrefix = "out_" + timeStamp;

    public void processFiles() throws IOException{
        List<RecordInfo> allRecordInfos = new ArrayList<>();
        List<VendorInfo> venderList = new ArrayList<>();
        venderList.add(new VendorInfo("wfmanual_", AccountVender.wellsfargomanual));
        venderList.add(new VendorInfo("wf_", AccountVender.wellsfargo));
        venderList.add(new VendorInfo("DFS_", AccountVender.discovery));
        venderList.add(new VendorInfo("Chase_", AccountVender.chase));
//        2024 citi card bypass as no taxable transaction
//        venderList.add(new VendorInfo("citi_", AccountVender.citi));
        for(VendorInfo vendor: venderList)
        {
            allRecordInfos.addAll(
                    processRawFile(vendor.getInputFilePrefix() + TaxYear.taxYear, vendor.getAccountVender(), outFilePrefix)

            );
        }


        Map<List<String>, Double> groupSums = allRecordInfos.stream()
                .collect(Collectors.groupingBy(
                        r -> Arrays.asList(r.getGroup1().toString(), r.getGroup2().toString()),  // Group by ga and gb
                        Collectors.summingDouble(RecordInfo::getAmount)  // Sum the 'amount'
                ));

        List<RecordInfo> recordWithSum = new ArrayList<>();
        // Assign groupAmount to each record
        RecordInfo recordInfoHelper = new RecordInfo();
        allRecordInfos.forEach(record -> {
            double sum = groupSums.get(Arrays.asList(record.getGroup1().toString(), record.getGroup2().toString()));
            RecordInfo newRecord = recordInfoHelper.getNewRecordInfo(record);
            newRecord.setGroupSum(sum);
            recordWithSum.add(newRecord);

        });

        List<RecordInfo> sortedRecords = recordWithSum.stream()
                .sorted(Comparator.comparing(RecordInfo::getGroup1, Comparator.reverseOrder())
                        .thenComparing(RecordInfo::getGroup2, Comparator.reverseOrder())
                        .thenComparing(RecordInfo::getDescription,Comparator.reverseOrder()))
                .collect(Collectors.toList());

        String outFileALlWithGroupAmount =  Cons.output  + "/all_" + outFilePrefix +  "withGroupSum.csv";
        List<String> lines1 = new ArrayList<>();
        sortedRecords.forEach( x -> lines1.add(x.toLineWithGroupAmmount()));
        appendFile( lines1, Paths.get(outFileALlWithGroupAmount));

////write to Group Sum Count

        Map<List<String>, List<RecordInfo>> groupedRecords = allRecordInfos.stream()
                .collect(Collectors.groupingBy(r -> Arrays.asList(r.getGroup1().toString(), r.getGroup2().toString())));



        List<RecordInfo> withSumAndCounts = new ArrayList<>();
        groupedRecords.forEach((key, recordList) -> {
            double totalAmount = recordList.stream().mapToDouble(RecordInfo::getAmount).sum();
            int groupCount = recordList.size();
            recordList.forEach(record -> {
                RecordInfo newRecord = recordInfoHelper.getNewRecordInfo(record);
                newRecord.setGroupSum(totalAmount);
                newRecord.setGroupCount(groupCount);
                withSumAndCounts.add(newRecord);

            });
        });

        List<RecordInfo> sortedWithSumAndCountRecords = withSumAndCounts.stream()
                .sorted(Comparator.comparing(RecordInfo::getGroup1, Comparator.reverseOrder())
                        .thenComparing(RecordInfo::getGroup2, Comparator.reverseOrder())
                        .thenComparing(RecordInfo::getDescription,Comparator.reverseOrder()))
                .collect(Collectors.toList());

        List<String> linesSumCount = new ArrayList<>();

        sortedWithSumAndCountRecords.forEach( x -> linesSumCount.add(x.toLineWithGroupSumAndCount()));

        String outFileALlWithGroupSumCount =  Cons.output  + "/all_" + outFilePrefix +  "withGroupSumCount.csv";


        appendFile( linesSumCount, Paths.get(outFileALlWithGroupSumCount));


        List<SummaryInfo> summaryInfoList = new ArrayList<>();
        groupedRecords.forEach((key, recordList) -> {
            double totalAmount = recordList.stream().mapToDouble(RecordInfo::getAmount).sum();
            int count = recordList.size();
            summaryInfoList.add(
                    SummaryInfo.builder()
                            .group1(key.get(0))
                            .group2(key.get(1))
                            .groupSum(totalAmount)
                            .groupCount(count)
                            .build()
            );

        });

        List<SummaryInfo> sortSummarys = summaryInfoList.stream()
                .sorted(Comparator.comparing(SummaryInfo::getGroup1, Comparator.reverseOrder())
                        .thenComparing(SummaryInfo::getGroup2, Comparator.reverseOrder())
                       )
                .collect(Collectors.toList());


        List<String> summaryLines = new ArrayList<>();
        sortSummarys.forEach(x -> {
            summaryLines.add(
                 String.format("%s|%s|%.2f|%d",
                       x.getGroup1(),x.getGroup2(),x.getGroupSum(), x.getGroupCount()));
        });

        String outFileSummary =  Cons.output  + "/all_" + outFilePrefix +  "summary.csv";


        appendFile( summaryLines, Paths.get(outFileSummary));


//        processRawFile("wf_" + TaxYear.taxYear, AccountVender.wellsfargo, outFilePrefix);;
//        processRawFile("DFS_" +  TaxYear.taxYear, AccountVender.discovery, outFilePrefix);;
//        processRawFile("Chase_" +  TaxYear.taxYear, AccountVender.chase, outFilePrefix);;
//
        summaryInOne(outFilePrefix);
    }


    //process all files owned by one vendor
    public  List<RecordInfo> processRawFile(String preFix, AccountVender accountVender, String outFilePrefix) throws IOException {

        File dir = new File(Cons.input);
//        File dir1 = new File("./doc")

        File[] filesArray = dir.listFiles(fileName ->
                fileName.getName().startsWith(preFix));

        List<File> files = filesArray == null
                ? new ArrayList<>()
                : Arrays.asList(filesArray);
        if (filesArray == null) {
            System.out.println("Input directory not found or not a directory, skipping: " + dir.getPath());
        }

        new File(Cons.output).mkdirs();



        String fileNameBase = outFilePrefix + "_" + accountVender.toString() + "_" + TaxYear.taxYear;
        String outFile0 = Cons.output  + "/" + fileNameBase +  "_0.csv";
//        BufferedWriter bw = new BufferedWriter(new FileWriter(outFile0));

        //put all file in one file , filename | xx |...
        //wf_2022_b_card_7877.csv|10/16/2022|156.61|*||AUTOMATIC PAYMENT - THANK YOU
        for(File file : files) {
               try {
               Path myPath = Paths.get(file.getPath());

               List<String> lines = Files.readAllLines(myPath, StandardCharsets.UTF_8);


               List<String> linesReformat = reformatLines(accountVender, file.getName(), lines);

               appendFile(linesReformat,Paths.get(outFile0));

               System.out.println("read source one file");

           } catch (Exception ex) {
               System.out.println(ex.getMessage());
           }

        }

//convert to record info, all in one file from one vender
        Path file0Path = Paths.get(outFile0);

        if (!Files.exists(file0Path)) {
            System.out.println("No input files found for prefix '" + preFix + "', skipping vendor: " + accountVender);
            return new ArrayList<>();
        }

        List<String> allLines = Files.readAllLines(file0Path, StandardCharsets.UTF_8);

        String outFile1 =  Cons.output  + "/" + fileNameBase +  "_1.csv";
        //wf_2022_b_checking_0697.csv|12/16/2022|2358.79|*||AMAZON.CKCH0ZSMK PAYMENTS 221215 5KSSKYOGSAO0UFF Adan Liu|sprill||amazon

//        appendFile( wfService.processRecord(allLines), Paths.get(fileName));

        List<RecordInfo> recordInfos = getRecordInfos(accountVender, allLines);

        List<String> linesInFile1 = new ArrayList<>();
        recordInfos.forEach( x -> linesInFile1.add(x.toALine()));
        appendFile( linesInFile1, Paths.get(outFile1));

        //generate grouped summary file

        String outFile2 =  Cons.output  + "/" + fileNameBase +  "_2.csv";
        appendFile( helperService.processSummary(recordInfos), Paths.get(outFile2));

        String outFile3 =  Cons.output  + "/" + fileNameBase +  "_3.csv";
        List<String> linesInGeneral = new ArrayList<>();
        recordInfos.forEach( x -> linesInFile1.add(x.toLineInGeneral()));
        appendFile( linesInFile1, Paths.get(outFile3));




        return recordInfos;

    }

    public void appendFile(List<String> lines, Path filePath){
        try{
            if (!Files.exists(filePath)){
                Files.createFile(filePath);
            }
            Files.write(filePath, lines, StandardOpenOption.APPEND);

        } catch (Exception ex){
            System.out.println(ex.getMessage());
        }


    }


    public List<String> reformatLines(AccountVender vender, String fileName, List<String> rawLines) throws Exception{
        // in order generate _0 file
        List<String> result = new ArrayList<>();
        switch (vender){
            case wellsfargo ->result.addAll( wfService.putALlInOneFile(fileName,rawLines));
            case wellsfargomanual -> result.addAll(wfManualInputService.putALlInOneFile(fileName,rawLines));
            case discovery -> result.addAll( discoverService.putALlInOneFile(fileName,rawLines));
            case chase -> result.addAll( chaseService.putALlInOneFile(fileName,rawLines));
            case citi -> result.addAll( citiService.putALlInOneFile(fileName,rawLines));


        }

        return result;

    }

    //reformat lines - | as split, remove header
    public List<RecordInfo> getRecordInfos(AccountVender vender, List<String> allLines){


        List<RecordInfo> result = new ArrayList<>();
        switch (vender){
            case wellsfargo ->result.addAll( wfService.getRecordInfo(allLines));
            case wellsfargomanual ->result.addAll( wfManualInputService.getRecordInfo(allLines));
            case discovery -> result.addAll( discoverService.getRecordInfo(allLines));
            case chase -> result.addAll( chaseService.getRecordInfo(allLines));
            case citi ->  result.addAll(citiService.getRecordInfo(allLines));

        }

        return result;

    }


    public void summaryInOne(String preFix){
        File dir = new File(Cons.output);
        dir.mkdirs();

        File[] filesArray = dir.listFiles(fileName ->
                fileName.getName().startsWith(preFix) && fileName.getName().endsWith("_2.csv"));

        List<File> files = filesArray == null
                ? new ArrayList<>()
                : Arrays.asList(filesArray);




        String outFile = Cons.output  + "/" + "all_" + preFix +  ".csv";
//        BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));

        //put all file in one file , filename | xx |...
        //wf_2022_b_card_7877.csv|10/16/2022|156.61|*||AUTOMATIC PAYMENT - THANK YOU
        for(File file : files) {
            try {
                Path myPath = Paths.get(file.getPath());

                List<String> lines = Files.readAllLines(myPath, StandardCharsets.UTF_8);




                appendFile(lines,Paths.get(outFile));

                System.out.println("read source one file");

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }

        }

    }

}
