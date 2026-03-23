package com.hus.mim_backend.shared.constants;

/**
 * Shared cache names used by both application and infrastructure layers.
 */
public final class CacheNames {
    public static final String PUBLIC_POSTS = "publicPosts";
    public static final String PUBLIC_POST_DETAILS = "publicPostDetails";
    public static final String PUBLIC_RESEARCH_PAPERS = "publicResearchPapers";
    public static final String PUBLIC_RESEARCH_PAPER_DETAILS = "publicResearchPaperDetails";
    public static final String PUBLIC_NEWS = "publicNews";
    public static final String PUBLIC_NEWS_DETAILS = "publicNewsDetails";
    public static final String PUBLIC_RESEARCH_CATEGORIES = "publicResearchCategories";
    public static final String RESEARCH_CATEGORIES_ALL = "researchCategoriesAll";
    public static final String PUBLIC_SPECIALIZATIONS = "publicSpecializations";
    public static final String SPECIALIZATIONS_ALL = "specializationsAll";
    public static final String PUBLIC_RECRUITMENT_CATEGORIES = "publicRecruitmentCategories";
    public static final String RECRUITMENT_CATEGORIES_ALL = "recruitmentCategoriesAll";

    private CacheNames() {
    }
}
