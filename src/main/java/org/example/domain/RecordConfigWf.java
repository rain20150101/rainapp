package org.example.domain;

import org.apache.commons.lang3.StringUtils;

public class RecordConfigWf extends RecordConfigBase {


    public RecordConfigWf(){

        addConfig("ATM CHECK DEPOSIT ON 01/19 6360 Irvine Blvd Irvine CA 0003552 ATM ID 9906S CARD 2403", Group1.tax, Group2.gov_check);

        addConfig("CHECK # 105",Group1.liuRealestate, Group2.house_rebate);

        addConfig("ATM CHECK DEPOSIT ON 05/21 6360 Irvine Blvd Irvine CA 0008884 ATM ID 9906S CARD 2403", Group1.liuRealestate, Group2.commission_rental);
        addConfig("ATM CHECK DEPOSIT ON 06/10 6360 Irvine Blvd Irvine CA 0001467 ATM ID 9906S CARD 2403", Group1.liuRealestate, Group2.commission_rental);
        addConfig("ATM CHECK DEPOSIT ON 08/03 6360 Irvine Blvd Irvine CA 0008396 ATM ID 9906S CARD 2403",Group1.liuRealestate, Group2.commission_rental);
        addConfig("ATM CHECK DEPOSIT ON 11/12 6360 Irvine Blvd Irvine CA 0001607 ATM ID 9906S CARD 2403", Group1.liuRealestate, Group2.commission_rental);
        addConfig("ZELLE FROM PANDA CLEAN INC", Group1.liuRealestate_income, Group2.commission_referal); //
        addConfig("WT FED", Group1.question);
        addConfig("WORLDFINANCIALGR COMMISSION",Group1.liuRealestate_income,Group2._ignore_as_in_1099); //
        addConfig("VENMO PAYMENT", Group1.unkown, Group2.venmoexpenase);
        addConfig("VENMO CASHOUT", Group1.unkown, Group2.venmoincome);
        addConfig("TOYOTA FINANCIAL LEASE", Group1.liuRealestate,Group2.carlease);
        addConfig("TEKTON", Group1.rentalRiverside, Group2.riverside_rental_income);
        addConfig("LA Rivera", Group1.rentalRiverside, Group2.hoa);

        addConfig("ST OF CA DMV INTERNET 221018 018049155221018 GUOSI LIU", Group1.liuRealestate, Group2.license); //

        addConfig("SO CAL GAS PAID", Group1.irvine169, Group2.utility);
        addConfig("SO CAL EDISON", Group1.irvine169, Group2.utility);
        addConfig("FID BKG SVC LLC MONEYLINE", Group1.notCount);  //Fidelity transfer

        addConfig("CA DIR ACH CONTRIB", Group1.adaneducation);  //

        addConfig("BUSINESS TO BUSINESS ACH GODADDYP", Group1.liuRealestate, Group2.telcom);


        addConfig("BILL PAY LA RIVERA HOA", Group1.rentalRiverside,Group2.hoa);

        addConfig("ATM WITHDRAWAL", Group1.unkown);
        addConfig("ATM CHECK DEPOSIT", Group1.unkown);
        addConfig("AMAZON", Group1.sprill);




        addConfig("FRANCHISE TAX BO", Group1.tax);
        addConfig("IRS USATAXPYM", Group1.tax);

        addConfig("eDeposit in Branch", Group1.question);



        addConfig("PAYPAL TRANSFER", Group1.question);
        addConfig("Orange County's Transfer", Group1.question);


        addConfig("BRIGHTHOUSE INSPREMIUM", Group1.notCount, Group2.insurance);

        addConfig("ZELLE TO", Group1.notCount);
        addConfig("ZELLE FROM", Group1.notCount);

        addConfig("TD AMERITRADE ACH IN", Group1.notCount); //transfer to stock

        addConfig("METROPOLITAN LIF POL PREM", Group1.notCount, Group2.insurance); //
        addConfig("WF Bus Credit AUTO PAY", Group1.notCount);
        addConfig("VW CREDIT", Group1.notCount, Group2.carlease);
        addConfig("TD AMERITRADE", Group1.notCount);
        addConfig("RECURRING TRANSFER TO", Group1.notCount);
        addConfig("RECURRING TRANSFER FROM", Group1.notCount);
        addConfig("LOANDEPOT COM LL DIRECT", Group1.notCount);
        addConfig("ONLINE TRANSFER", Group1.notCount);
        addConfig("METROPOLITAN LIF POL", Group1.notCount);
        addConfig("MetLife PAYMENT", Group1.notCount);
        addConfig("loanDepotcom LLC LD_MRTGPM", Group1.notCount);
        addConfig("LOANDEPOT 1 TIME", Group1.notCount);
        addConfig("INTEREST PAYMENT", Group1.notCount, Group2.interest);
        addConfig("eBay PAYOUT", Group1.sprill, Group2.ebay);
        addConfig("eBay Inc", Group1.sprill, Group2.ebay);
        addConfig("eBay Com", Group1.sprill, Group2.ebay);
        addConfig("DISCOVER E-PAYMENT", Group1.notCount);
        addConfig("CITI AUTOPAY PAYMENT", Group1.notCount);
        addConfig("CHASE CREDIT CRD AUTOPAY", Group1.notCount);

        addConfig("ATGPay Online", Group1.irvine169, Group2.hoa);
        addConfig("Atgpay Online", Group1.irvine169, Group2.hoa);
        addConfig("Stonegate Villag", Group1.irvine169, Group2.hoa);
        addConfig("BILL PAY Cambia HOA", Group1.irvine169,Group2.hoa);

        addConfig("Orange County's Transfer", Group1.ARecord, Group2.car);
        addConfig("IRVINE BUSINESS LICENSE", Group1.sprill, Group2.license);

        addConfig("WIRE TRANS SVC", Group1.liuRealestate, Group2.service);





        addConfig("CA DMV", Group1.ARecord, Group2.dmv);
        addConfig("CALIF DMV", Group1.ARecord, Group2.dmv);

        addConfig("Godaddyps Deposit", Group1.liuRealestate, Group2.telcom);

    }

    void addConfig(String des, Group1 group1, Group2 group2) {
        configInfos.add(ConfigInfo.builder()
                .searchKey(des.toLowerCase())
                .group1(group1)
                        .group2(group2)
                .build()

        );
    }
    void addConfig(String des, Group1 group1) {
        configInfos.add(ConfigInfo.builder()
                .searchKey(des.toLowerCase())
                .group1(group1)
                .build()

        );
    }


    public ConfigInfo getGroupInfo(String des){
       return  configInfos.stream().filter( x -> StringUtils.containsIgnoreCase(des, x.getSearchKey()))
               .findFirst()
               .orElse(new ConfigInfo());
    }


}
