package com.hus.mim_backend.application.auth.dto;

/**
 * Registration request DTO
 */
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}
