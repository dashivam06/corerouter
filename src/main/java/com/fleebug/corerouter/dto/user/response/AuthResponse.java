package com.fleebug.corerouter.dto.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private Integer userId;
    private String username;
    private String email;
    private String token;
    private String message;
    private boolean success;
}
