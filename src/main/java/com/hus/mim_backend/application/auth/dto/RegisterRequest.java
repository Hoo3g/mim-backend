package com.hus.mim_backend.application.auth.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Registration request DTO
 */
@Setter
@Getter
public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
    private String studentId;
    private String userType;

    public RegisterRequest() {
    }

    public RegisterRequest(String email, String password, String fullName, String studentId, String userType) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.studentId = studentId;
        this.userType = userType;
    }

}
