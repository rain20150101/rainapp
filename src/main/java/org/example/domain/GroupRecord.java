package org.example.domain;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupRecord {
    String groupKey;
    long count;
    double sum;
    double amount;
    Group1 group1;
    Group2 group2;
    String searchKey;

}
