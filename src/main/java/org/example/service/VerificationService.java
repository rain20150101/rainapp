package org.example.service;

import org.example.domain.AccountVender;
import org.example.domain.FileVerificationResult;
import org.example.domain.RecordInfo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Re-derives record count / credit(+) sum / debt(-) sum directly from the raw input
// file, independent of reformatLines()/getRecordInfo(), so a bug that silently drops
// or miscounts records anywhere in the pipeline surfaces as a NOT MATCH instead of
// going unnoticed.
public class VerificationService {

    private static final Pattern DISCOVER_SPLIT =
            Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*(?![^\"]*\"))");

    public FileVerificationResult verify(File rawFile, AccountVender vendor, List<RecordInfo> reformattedRecords) throws IOException {
        List<Double> rawAmounts = extractRawAmounts(rawFile, vendor);

        double rawCreditSum = 0;
        double rawDebitSum = 0;
        for (double amount : rawAmounts) {
            if (amount >= 0) rawCreditSum += amount; else rawDebitSum += amount;
        }

        double reformatCreditSum = 0;
        double reformatDebitSum = 0;
        for (RecordInfo r : reformattedRecords) {
            if (r.getAmount() >= 0) reformatCreditSum += r.getAmount(); else reformatDebitSum += r.getAmount();
        }

        return FileVerificationResult.builder()
                .fileName(rawFile.getName())
                .rawCount(rawAmounts.size())
                .reformatCount(reformattedRecords.size())
                .rawCreditSum(rawCreditSum)
                .reformatCreditSum(reformatCreditSum)
                .rawDebitSum(rawDebitSum)
                .reformatDebitSum(reformatDebitSum)
                .build();
    }

    //column positions below mirror the real raw headers in 2025/input/*, independent
    //of the (also vendor-specific) reformatLines()/getRecordInfo() column reads.
    private List<Double> extractRawAmounts(File rawFile, AccountVender vendor) throws IOException {
        List<String> lines = Files.readAllLines(rawFile.toPath(), StandardCharsets.UTF_8);
        List<Double> amounts = new ArrayList<>();

        switch (vendor) {
            //DATE,DESCRIPTION,AMOUNT,CHECK #,STATUS
            case wellsfargo -> {
                for (int i = 1; i < lines.size(); i++) {
                    addAmount(amounts, rawFile, i, lines.get(i), line -> {
                        String stripped = line;
                        if (stripped.startsWith("\"")) stripped = stripped.substring(1);
                        if (stripped.endsWith("\"")) stripped = stripped.substring(0, stripped.length() - 1);
                        String[] fields = stripped.split("\",\"", -1);
                        return Double.parseDouble(fields[2]);
                    });
                }
            }
            //business cards: Card,Transaction Date,Post Date,Description,Category,Type,Amount,Memo (8 fields)
            //personal cards: Transaction Date,Post Date,Description,Category,Type,Amount,Memo (7 fields, no Card column)
            case chase -> {
                for (int i = 1; i < lines.size(); i++) {
                    addAmount(amounts, rawFile, i, lines.get(i), line -> {
                        String[] fields = line.split(",", -1);
                        int amountIndex = fields.length >= 8 ? 6 : 5;
                        return Double.parseDouble(fields[amountIndex]);
                    });
                }
            }
            //Trans. Date,Description,Amount,Category
            //descriptions can embed raw newlines, splitting one logical record
            //across several physical lines - merge those back together first.
            case discovery -> {
                List<String> mergedLines = HelperService.mergeMultilineCsvRecords(lines);
                for (int i = 1; i < mergedLines.size(); i++) {
                    addAmount(amounts, rawFile, i, mergedLines.get(i), line -> {
                        String[] fields = splitCsvLine(line);
                        return -Double.parseDouble(fields[2].replace(",", "")); //discover raw data: expense is +, amount may have thousands-separator commas
                    });
                }
            }
            //Status,Date,Description,Debit,Credit,Member Name
            //citi has no single signed Amount column - only one of Debit (money
            //out, negative) / Credit (money in, positive) is set per row.
            case citi -> {
                for (int i = 1; i < lines.size(); i++) {
                    addAmount(amounts, rawFile, i, lines.get(i), line -> {
                        String[] fields = splitCsvLine(line);
                        String debit = fields[3];
                        String credit = fields[4];
                        return (credit != null && !credit.isBlank())
                                ? Double.parseDouble(credit.replace(",", ""))
                                : -Double.parseDouble(debit.replace(",", ""));
                    });
                }
            }
            //wellsfargomanual is free-text, not a vendor CSV export - no raw-side
            //amount source comparable to the other vendors; skip raw verification.
            default -> {
            }
        }
        return amounts;
    }

    private interface AmountParser {
        double parse(String line) throws Exception;
    }

    private void addAmount(List<Double> amounts, File rawFile, int lineIndex, String line, AmountParser parser) {
        if (line == null || line.isBlank()) {
            return;
        }
        try {
            amounts.add(parser.parse(line));
        } catch (Exception ex) {
            System.out.println("verification: could not parse raw amount in " + rawFile.getName()
                    + " line " + (lineIndex + 1) + ": " + line);
        }
    }

    private String[] splitCsvLine(String input) {
        List<String> items = new ArrayList<>();
        Matcher matcher = DISCOVER_SPLIT.matcher(input);
        int start = 0;
        while (matcher.find()) {
            items.add(input.substring(start, matcher.start()).trim());
            start = matcher.end();
        }
        items.add(input.substring(start).trim());

        List<String> trimmed = new ArrayList<>();
        for (String item : items) {
            if (item.startsWith("\"") && item.endsWith("\"")) {
                item = item.substring(1, item.length() - 1);
            }
            trimmed.add(item);
        }
        return trimmed.toArray(new String[0]);
    }
}
