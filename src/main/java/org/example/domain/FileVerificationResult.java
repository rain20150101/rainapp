package org.example.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileVerificationResult {
    String fileName;
    int rawCount;
    int reformatCount;
    double rawCreditSum;
    double reformatCreditSum;
    double rawDebitSum;
    double reformatDebitSum;

    private static final double EPSILON = 0.01;

    public boolean isCountMatch() {
        return rawCount == reformatCount;
    }

    public boolean isCreditMatch() {
        return Math.abs(rawCreditSum - reformatCreditSum) < EPSILON;
    }

    public boolean isDebitMatch() {
        return Math.abs(rawDebitSum - reformatDebitSum) < EPSILON;
    }

    public boolean isAllMatch() {
        return isCountMatch() && isCreditMatch() && isDebitMatch();
    }

    public double getRawNetAmount() {
        return rawCreditSum + rawDebitSum;
    }

    public double getReformatNetAmount() {
        return reformatCreditSum + reformatDebitSum;
    }

    public boolean isNetAmountMatch() {
        return Math.abs(getRawNetAmount() - getReformatNetAmount()) < EPSILON;
    }

    public String toReportLine() {
        return String.format(
                "%s : %s | record count raw=%d reformat=%d [%s] | credit(+) raw=%.2f reformat=%.2f [%s] | debt(-) raw=%.2f reformat=%.2f [%s] | net(+/-) raw=%.2f reformat=%.2f [%s]",
                fileName,
                isAllMatch() ? "MATCH" : "NOT MATCH",
                rawCount, reformatCount, isCountMatch() ? "match" : "NOT MATCH",
                rawCreditSum, reformatCreditSum, isCreditMatch() ? "match" : "NOT MATCH",
                rawDebitSum, reformatDebitSum, isDebitMatch() ? "match" : "NOT MATCH",
                getRawNetAmount(), getReformatNetAmount(), isNetAmountMatch() ? "match" : "NOT MATCH"
        );
    }
}
