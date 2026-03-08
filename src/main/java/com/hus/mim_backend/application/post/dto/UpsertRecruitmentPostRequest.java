package com.hus.mim_backend.application.post.dto;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating/updating recruitment posts via portal editor.
 */
public class UpsertRecruitmentPostRequest {
    private String title;
    private String description;
    private String postType;
    private String jobType;
    private String requirements;
    private String benefits;
    private String achievements;
    private String location;
    private String salaryRange;
    private String contactEmail;
    private String contactPhone;
    private List<String> tags;
    private String status;
    private String studentCvUrl;
    private Map<String, Object> displayInfo;
    private List<ResearchPaperLinkItem> researchPaperLinks;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public String getBenefits() {
        return benefits;
    }

    public void setBenefits(String benefits) {
        this.benefits = benefits;
    }

    public String getAchievements() {
        return achievements;
    }

    public void setAchievements(String achievements) {
        this.achievements = achievements;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStudentCvUrl() {
        return studentCvUrl;
    }

    public void setStudentCvUrl(String studentCvUrl) {
        this.studentCvUrl = studentCvUrl;
    }

    public Map<String, Object> getDisplayInfo() {
        return displayInfo;
    }

    public void setDisplayInfo(Map<String, Object> displayInfo) {
        this.displayInfo = displayInfo;
    }

    public List<ResearchPaperLinkItem> getResearchPaperLinks() {
        return researchPaperLinks;
    }

    public void setResearchPaperLinks(List<ResearchPaperLinkItem> researchPaperLinks) {
        this.researchPaperLinks = researchPaperLinks;
    }

    public static class ResearchPaperLinkItem {
        private String id;
        private String title;
        private String url;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}

