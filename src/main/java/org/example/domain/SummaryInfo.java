package org.example.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SummaryInfo {
    public String group1;
    public String group2;
    public double groupSum;
    public int groupCount;

}
