package org.example.domain;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorInfo {
    String inputFilePrefix;
    AccountVender accountVender;
    public VendorInfo(String inputFilePrefix, AccountVender accountVender){
        this.inputFilePrefix = inputFilePrefix;
        this.accountVender = accountVender;

    }
}
