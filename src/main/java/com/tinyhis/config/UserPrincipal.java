package com.tinyhis.config;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * User Principal for authentication context
 */
@Data
@AllArgsConstructor
public class UserPrincipal {
    private Long userId;
    private String username;
    private String role;
    private String userType; // "patient" or "staff"
}
