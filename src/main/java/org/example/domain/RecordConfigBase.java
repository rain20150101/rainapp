package org.example.domain;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class RecordConfigBase {
    public List<ConfigInfo> configInfos = new ArrayList<>();

    //BAU rules shared by every vendor - checked before any vendor-specific rule,
    //since subclasses run this constructor first (implicit super()) and
    //getGroupInfo() returns the first match in configInfos order.
    public RecordConfigBase(){
        addConfig("PETSMART", Group1.notCount);
        addConfig("TRADER JOE", Group1.notCount, Group2.TraderJoe);
        addConfig("RALPHS #029", Group1.notCount, Group2.RALPHS);
        addConfig("ZION MARKET", Group1.notCount, Group2.ZION);
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
                .group2(Group2.nullvalue)
                .build()

        );
    }


    public ConfigInfo getGroupInfo(String des){
        return  configInfos.stream().filter( x -> StringUtils.containsIgnoreCase(des, x.getSearchKey()))
                .findFirst()
                .orElse(ConfigInfo.builder().searchKey(des).build());
    }
}
