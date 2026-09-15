package com.example.bookrunner.dto.auth;

import com.example.bookrunner.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private Role role;
    private LocalDateTime createdAt;
}
