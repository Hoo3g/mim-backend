package com.hus.mim_backend.application.profile.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProfileDashboardResponse {
    private String role;
    private StudentDashboard student;
    private CompanyDashboard company;
    private LecturerDashboard lecturer;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public StudentDashboard getStudent() {
        return student;
    }

    public void setStudent(StudentDashboard student) {
        this.student = student;
    }

    public CompanyDashboard getCompany() {
        return company;
    }

    public void setCompany(CompanyDashboard company) {
        this.company = company;
    }

    public LecturerDashboard getLecturer() {
        return lecturer;
    }

    public void setLecturer(LecturerDashboard lecturer) {
        this.lecturer = lecturer;
    }

    public static class StudentDashboard {
        private List<SavedPaperItem> savedPapers = new ArrayList<>();
        private List<PendingApplicationItem> pendingApplications = new ArrayList<>();

        public List<SavedPaperItem> getSavedPapers() {
            return savedPapers;
        }

        public void setSavedPapers(List<SavedPaperItem> savedPapers) {
            this.savedPapers = savedPapers;
        }

        public List<PendingApplicationItem> getPendingApplications() {
            return pendingApplications;
        }

        public void setPendingApplications(List<PendingApplicationItem> pendingApplications) {
            this.pendingApplications = pendingApplications;
        }
    }

    public static class SavedPaperItem {
        private UUID paperId;
        private String title;
        private String researchArea;
        private String category;
        private Integer publicationYear;
        private LocalDateTime savedAt;

        public UUID getPaperId() {
            return paperId;
        }

        public void setPaperId(UUID paperId) {
            this.paperId = paperId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getResearchArea() {
            return researchArea;
        }

        public void setResearchArea(String researchArea) {
            this.researchArea = researchArea;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Integer getPublicationYear() {
            return publicationYear;
        }

        public void setPublicationYear(Integer publicationYear) {
            this.publicationYear = publicationYear;
        }

        public LocalDateTime getSavedAt() {
            return savedAt;
        }

        public void setSavedAt(LocalDateTime savedAt) {
            this.savedAt = savedAt;
        }
    }

    public static class PendingApplicationItem {
        private UUID applicationId;
        private UUID postId;
        private String postTitle;
        private String companyName;
        private String postType;
        private String location;
        private String status;
        private LocalDateTime appliedAt;

        public UUID getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(UUID applicationId) {
            this.applicationId = applicationId;
        }

        public UUID getPostId() {
            return postId;
        }

        public void setPostId(UUID postId) {
            this.postId = postId;
        }

        public String getPostTitle() {
            return postTitle;
        }

        public void setPostTitle(String postTitle) {
            this.postTitle = postTitle;
        }

        public String getCompanyName() {
            return companyName;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        public String getPostType() {
            return postType;
        }

        public void setPostType(String postType) {
            this.postType = postType;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getAppliedAt() {
            return appliedAt;
        }

        public void setAppliedAt(LocalDateTime appliedAt) {
            this.appliedAt = appliedAt;
        }
    }

    public static class CompanyDashboard {
        private List<CompanyPostItem> myPosts = new ArrayList<>();
        private List<PendingApplicantItem> pendingApplicants = new ArrayList<>();

        public List<CompanyPostItem> getMyPosts() {
            return myPosts;
        }

        public void setMyPosts(List<CompanyPostItem> myPosts) {
            this.myPosts = myPosts;
        }

        public List<PendingApplicantItem> getPendingApplicants() {
            return pendingApplicants;
        }

        public void setPendingApplicants(List<PendingApplicantItem> pendingApplicants) {
            this.pendingApplicants = pendingApplicants;
        }
    }

    public static class CompanyPostItem {
        private UUID postId;
        private String title;
        private String status;
        private String approvalStatus;
        private Integer pendingCount;
        private LocalDateTime createdAt;

        public UUID getPostId() {
            return postId;
        }

        public void setPostId(UUID postId) {
            this.postId = postId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getApprovalStatus() {
            return approvalStatus;
        }

        public void setApprovalStatus(String approvalStatus) {
            this.approvalStatus = approvalStatus;
        }

        public Integer getPendingCount() {
            return pendingCount;
        }

        public void setPendingCount(Integer pendingCount) {
            this.pendingCount = pendingCount;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class PendingApplicantItem {
        private UUID applicationId;
        private UUID postId;
        private String postTitle;
        private UUID applicantId;
        private String applicantName;
        private String message;
        private String cvUrl;
        private LocalDateTime appliedAt;

        public UUID getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(UUID applicationId) {
            this.applicationId = applicationId;
        }

        public UUID getPostId() {
            return postId;
        }

        public void setPostId(UUID postId) {
            this.postId = postId;
        }

        public String getPostTitle() {
            return postTitle;
        }

        public void setPostTitle(String postTitle) {
            this.postTitle = postTitle;
        }

        public UUID getApplicantId() {
            return applicantId;
        }

        public void setApplicantId(UUID applicantId) {
            this.applicantId = applicantId;
        }

        public String getApplicantName() {
            return applicantName;
        }

        public void setApplicantName(String applicantName) {
            this.applicantName = applicantName;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getCvUrl() {
            return cvUrl;
        }

        public void setCvUrl(String cvUrl) {
            this.cvUrl = cvUrl;
        }

        public LocalDateTime getAppliedAt() {
            return appliedAt;
        }

        public void setAppliedAt(LocalDateTime appliedAt) {
            this.appliedAt = appliedAt;
        }
    }

    public static class LecturerDashboard {
        private List<LecturerPaperItem> myPapers = new ArrayList<>();
        private List<CollaboratorItem> collaborators = new ArrayList<>();

        public List<LecturerPaperItem> getMyPapers() {
            return myPapers;
        }

        public void setMyPapers(List<LecturerPaperItem> myPapers) {
            this.myPapers = myPapers;
        }

        public List<CollaboratorItem> getCollaborators() {
            return collaborators;
        }

        public void setCollaborators(List<CollaboratorItem> collaborators) {
            this.collaborators = collaborators;
        }
    }

    public static class LecturerPaperItem {
        private UUID paperId;
        private String title;
        private String researchArea;
        private String approvalStatus;
        private Integer publicationYear;
        private LocalDateTime createdAt;

        public UUID getPaperId() {
            return paperId;
        }

        public void setPaperId(UUID paperId) {
            this.paperId = paperId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getResearchArea() {
            return researchArea;
        }

        public void setResearchArea(String researchArea) {
            this.researchArea = researchArea;
        }

        public String getApprovalStatus() {
            return approvalStatus;
        }

        public void setApprovalStatus(String approvalStatus) {
            this.approvalStatus = approvalStatus;
        }

        public Integer getPublicationYear() {
            return publicationYear;
        }

        public void setPublicationYear(Integer publicationYear) {
            this.publicationYear = publicationYear;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class CollaboratorItem {
        private UUID collaboratorId;
        private String name;
        private String collaboratorType;
        private Integer paperCount;

        public UUID getCollaboratorId() {
            return collaboratorId;
        }

        public void setCollaboratorId(UUID collaboratorId) {
            this.collaboratorId = collaboratorId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCollaboratorType() {
            return collaboratorType;
        }

        public void setCollaboratorType(String collaboratorType) {
            this.collaboratorType = collaboratorType;
        }

        public Integer getPaperCount() {
            return paperCount;
        }

        public void setPaperCount(Integer paperCount) {
            this.paperCount = paperCount;
        }
    }
}
