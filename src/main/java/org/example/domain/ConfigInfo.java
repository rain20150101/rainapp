package org.example.domain;


import lombok.*;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigInfo {

    String searchKey;
    Group1 group1;
    Group2 group2;

}
