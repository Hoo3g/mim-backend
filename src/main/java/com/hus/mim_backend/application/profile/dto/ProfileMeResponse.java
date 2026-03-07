package com.hus.mim_backend.application.profile.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProfileMeResponse {
    private UUID userId;
    private String email;
    private String role;
    private String accountStatus;
    private String avatarUrl;
    private StudentProfile student;
    private CompanyProfile company;
    private LecturerProfile lecturer;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public StudentProfile getStudent() {
        return student;
    }

    public void setStudent(StudentProfile student) {
        this.student = student;
    }

    public CompanyProfile getCompany() {
        return company;
    }

    public void setCompany(CompanyProfile company) {
        this.company = company;
    }

    public LecturerProfile getLecturer() {
        return lecturer;
    }

    public void setLecturer(LecturerProfile lecturer) {
        this.lecturer = lecturer;
    }

    public static class StudentProfile {
        private String firstName;
        private String lastName;
        private String university;
        private String major;
        private String bio;
        private String cvUrl;
        private String studentType;
        private String studentCode;
        private String achievements;
        private String careerGoal;
        private String desiredPosition;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getUniversity() {
            return university;
        }

        public void setUniversity(String university) {
            this.university = university;
        }

        public String getMajor() {
            return major;
        }

        public void setMajor(String major) {
            this.major = major;
        }

        public String getBio() {
            return bio;
        }

        public void setBio(String bio) {
            this.bio = bio;
        }

        public String getCvUrl() {
            return cvUrl;
        }

        public void setCvUrl(String cvUrl) {
            this.cvUrl = cvUrl;
        }

        public String getStudentType() {
            return studentType;
        }

        public void setStudentType(String studentType) {
            this.studentType = studentType;
        }

        public String getStudentCode() {
            return studentCode;
        }

        public void setStudentCode(String studentCode) {
            this.studentCode = studentCode;
        }

        public String getAchievements() {
            return achievements;
        }

        public void setAchievements(String achievements) {
            this.achievements = achievements;
        }

        public String getCareerGoal() {
            return careerGoal;
        }

        public void setCareerGoal(String careerGoal) {
            this.careerGoal = careerGoal;
        }

        public String getDesiredPosition() {
            return desiredPosition;
        }

        public void setDesiredPosition(String desiredPosition) {
            this.desiredPosition = desiredPosition;
        }
    }

    public static class CompanyProfile {
        private String name;
        private String industry;
        private String website;
        private String location;
        private String description;
        private String logoUrl;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIndustry() {
            return industry;
        }

        public void setIndustry(String industry) {
            this.industry = industry;
        }

        public String getWebsite() {
            return website;
        }

        public void setWebsite(String website) {
            this.website = website;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getLogoUrl() {
            return logoUrl;
        }

        public void setLogoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
        }
    }

    public static class LecturerProfile {
        private String firstName;
        private String lastName;
        private String title;
        private String academicRank;
        private String bio;
        private String avatarUrl;
        private List<String> researchInterests = new ArrayList<>();

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAcademicRank() {
            return academicRank;
        }

        public void setAcademicRank(String academicRank) {
            this.academicRank = academicRank;
        }

        public String getBio() {
            return bio;
        }

        public void setBio(String bio) {
            this.bio = bio;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        public List<String> getResearchInterests() {
            return researchInterests;
        }

        public void setResearchInterests(List<String> researchInterests) {
            this.researchInterests = researchInterests;
        }
    }
}
