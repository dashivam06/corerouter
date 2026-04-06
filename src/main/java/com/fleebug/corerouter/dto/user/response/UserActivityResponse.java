package com.fleebug.corerouter.dto.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityResponse {
    private String action;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
}
