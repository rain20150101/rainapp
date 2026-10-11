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
    VerificationService verificationService = new VerificationService();
    List<FileVerificationResult> verificationResults = new ArrayList<>();
    String timeStamp = HelperService.getTime();
    String outFilePrefix = "out_" + timeStamp;

    public void processFiles() throws IOException{
        List<RecordInfo> allRecordInfos = new ArrayList<>();
        List<VendorInfo> venderList = new ArrayList<>();
        venderList.add(new VendorInfo("wfmanual_", AccountVender.wellsfargomanual));
        venderList.add(new VendorInfo("wf_", AccountVender.wellsfargo));
        venderList.add(new VendorInfo("discovery_", AccountVender.discovery));
        venderList.add(new VendorInfo("chase_", AccountVender.chase));
        venderList.add(new VendorInfo("citi_", AccountVender.citi));
        for(VendorInfo vendor: venderList)
        {
            allRecordInfos.addAll(
                    processRawFile(vendor.getInputFilePrefix(), vendor.getAccountVender(), outFilePrefix)

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

        List<String> lines1 = new ArrayList<>();
        sortedRecords.forEach( x -> lines1.add(x.toLineWithGroupAmmount()));
        writeOutput(lines1, "all_" + outFilePrefix + "withGroupSum.csv", "all_withGroupSum.csv");

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

        writeOutput(linesSumCount, "all_" + outFilePrefix + "withGroupSumCount.csv", "all_withGroupSumCount.csv");


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

        writeOutput(summaryLines, "all_" + outFilePrefix + "summary.csv", "all_summary.csv");


        summaryInOne(outFilePrefix);

        printOverallVerification();
    }

    //aggregates every per-file verification result into one overall match/not-match
    //on total record count, total positive (credit) amount, total negative (debt) amount
    public void printOverallVerification(){
        FileVerificationResult total = FileVerificationResult.builder()
                .fileName("OVERALL")
                .build();
        for (FileVerificationResult result : verificationResults) {
            total.setRawCount(total.getRawCount() + result.getRawCount());
            total.setReformatCount(total.getReformatCount() + result.getReformatCount());
            total.setRawCreditSum(total.getRawCreditSum() + result.getRawCreditSum());
            total.setReformatCreditSum(total.getReformatCreditSum() + result.getReformatCreditSum());
            total.setRawDebitSum(total.getRawDebitSum() + result.getRawDebitSum());
            total.setReformatDebitSum(total.getReformatDebitSum() + result.getReformatDebitSum());
        }
        System.out.println(total.toReportLine());
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
        new File(Cons.outputArchive).mkdirs();



        String fileNameBase = outFilePrefix + "_" + accountVender.toString() + "_" + TaxYear.taxYear;
        String stableBase = accountVender.toString() + "_" + TaxYear.taxYear;
        String outFile0Archive = Cons.outputArchive  + "/" + fileNameBase +  "_0.csv";
//        BufferedWriter bw = new BufferedWriter(new FileWriter(outFile0));

        //put all file in one file , filename | xx |...
        //wf_2022_b_card_7877.csv|10/16/2022|156.61|*||AUTOMATIC PAYMENT - THANK YOU
        for(File file : files) {
               try {
               Path myPath = Paths.get(file.getPath());

               List<String> lines = Files.readAllLines(myPath, StandardCharsets.UTF_8);
               List<String> mergedLines = HelperService.mergeMultilineCsvRecords(lines);

               if (accountVender == AccountVender.discovery) {
                   String baseName = file.getName().replaceFirst("\\.[^.]+$", "");
                   writeOutput(mergedLines, outFilePrefix + "_" + baseName + "_correct_format.csv", baseName + "_correct_format.csv");
               }

               List<String> linesReformat = reformatLines(accountVender, file.getName(), mergedLines);

               writeOutput(linesReformat, fileNameBase + "_0.csv", stableBase + "_0.csv");

               System.out.println("read source one file");

           } catch (Exception ex) {
               System.out.println(ex.getMessage());
           }

        }

//convert to record info, all in one file from one vender
        Path file0Path = Paths.get(outFile0Archive);

        if (!Files.exists(file0Path)) {
            System.out.println("No input files found for prefix '" + preFix + "', skipping vendor: " + accountVender);
            return new ArrayList<>();
        }

        List<String> allLines = Files.readAllLines(file0Path, StandardCharsets.UTF_8);

        //wf_2022_b_checking_0697.csv|12/16/2022|2358.79|*||AMAZON.CKCH0ZSMK PAYMENTS 221215 5KSSKYOGSAO0UFF Adan Liu|sprill||amazon

//        appendFile( wfService.processRecord(allLines), Paths.get(fileName));

        List<RecordInfo> recordInfos = getRecordInfos(accountVender, allLines);

        Map<String, List<RecordInfo>> recordInfosByFile = recordInfos.stream()
                .collect(Collectors.groupingBy(RecordInfo::getFileName));
        for (File file : files) {
            List<RecordInfo> fileRecordInfos = recordInfosByFile.getOrDefault(file.getName(), new ArrayList<>());
            try {
                FileVerificationResult verificationResult = verificationService.verify(file, accountVender, fileRecordInfos);
                System.out.println(verificationResult.toReportLine());
                verificationResults.add(verificationResult);
            } catch (Exception ex) {
                System.out.println("verification failed for " + file.getName() + ": " + ex.getMessage());
            }
        }

        List<String> linesInFile1 = new ArrayList<>();
        recordInfos.forEach( x -> linesInFile1.add(x.toALine()));
        writeOutput(linesInFile1, fileNameBase + "_1.csv", stableBase + "_1.csv");

        //generate grouped summary file

        writeOutput(helperService.processSummary(recordInfos), fileNameBase + "_2.csv", stableBase + "_2.csv");

        List<String> linesInGeneral = new ArrayList<>();
        recordInfos.forEach( x -> linesInFile1.add(x.toLineInGeneral()));
        writeOutput(linesInFile1, fileNameBase + "_3.csv", stableBase + "_3.csv");




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

    Set<String> stableFilesWrittenThisRun = new HashSet<>();

    //writes a timestamped copy to 2025/output/archive/<archiveFileName> (appended,
    //same as before, since a run may add to the same archive file across several
    //input files of one vendor) and a second copy to 2025/output/<stableFileName>
    //whose name never changes between runs - truncated on the first write of this
    //run so it reflects only the latest run, then appended to for the rest of it.
    public void writeOutput(List<String> lines, String archiveFileName, String stableFileName){
        new File(Cons.outputArchive).mkdirs();
        appendFile(lines, Paths.get(Cons.outputArchive + "/" + archiveFileName));

        new File(Cons.output).mkdirs();
        Path stablePath = Paths.get(Cons.output + "/" + stableFileName);
        try {
            if (stableFilesWrittenThisRun.add(stableFileName)) {
                Files.write(stablePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                Files.write(stablePath, lines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (Exception ex) {
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
        File dir = new File(Cons.outputArchive);
        dir.mkdirs();

        File[] filesArray = dir.listFiles(fileName ->
                fileName.getName().startsWith(preFix) && fileName.getName().endsWith("_2.csv"));

        List<File> files = filesArray == null
                ? new ArrayList<>()
                : Arrays.asList(filesArray);




        //put all file in one file , filename | xx |...
        //wf_2022_b_card_7877.csv|10/16/2022|156.61|*||AUTOMATIC PAYMENT - THANK YOU
        for(File file : files) {
            try {
                Path myPath = Paths.get(file.getPath());

                List<String> lines = Files.readAllLines(myPath, StandardCharsets.UTF_8);

                writeOutput(lines, "all_" + preFix + ".csv", "all.csv");

                System.out.println("read source one file");

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }

        }

    }

}
