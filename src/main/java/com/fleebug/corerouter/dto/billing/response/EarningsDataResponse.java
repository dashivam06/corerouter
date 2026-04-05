package com.fleebug.corerouter.dto.billing.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EarningsDataResponse {

  
    private Map<String, String> earningsByDate;

 
    private String totalEarnings;

    private Integer totalTransactionCount;

    private String filterPeriod;
    private String filterType;
}
