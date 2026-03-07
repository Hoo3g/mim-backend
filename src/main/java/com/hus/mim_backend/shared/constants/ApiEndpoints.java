package com.hus.mim_backend.shared.constants;

public class ApiEndpoints {
    public static final String API_V1 = "/api/v1";
    public static final String API_PUBLIC = "/api/public";

    // Auth Endpoints
    public static final String AUTH = API_V1 + "/auth";
    public static final String LOGIN = "/login";
    public static final String GOOGLE_LOGIN = "/google";
    public static final String REGISTER = "/register";
    public static final String LOGOUT = "/logout";
    public static final String REFRESH_TOKEN = "/refresh-token";

    // Admin Endpoints
    public static final String ADMIN = API_V1 + "/admin";
    public static final String ADMIN_CONTENT = ADMIN + "/content";
    public static final String MODERATION = "/moderation";
    public static final String MODERATION_POSTS = "/posts";
    public static final String MODERATION_PAPERS = "/papers";
    public static final String MODERATION_POST_BY_ID = "/posts/{postId}";
    public static final String MODERATION_PAPER_BY_ID = "/papers/{paperId}";
    public static final String RBAC = "/rbac";
    public static final String RBAC_PERMISSIONS = "/permissions";
    public static final String RBAC_ROLES = "/roles";
    public static final String RBAC_USERS = "/users";
    public static final String RBAC_USER_OVERRIDES = "/users/{userId}/overrides";
    public static final String RBAC_USER_ROLES = "/users/{userId}/roles";
    public static final String ADMIN_STORAGE = ADMIN + "/storage";
    public static final String CONTENT = API_V1 + "/content";
    public static final String RESEARCH_HERO = "/research-hero";

    // Research Endpoints
    public static final String RESEARCH = API_V1 + "/research-papers";
    public static final String RESEARCH_MY = "/my";
    public static final String RESEARCH_BY_ID = "/{paperId}";

    // Recruitment Endpoints
    public static final String RECRUITMENT = API_V1 + "/recruitment";
    public static final String POSTS = API_V1 + "/posts";
    public static final String POST_BY_ID = "/{postId}";

    // Storage Endpoints
    public static final String STORAGE = API_V1 + "/storage";
    public static final String PUBLIC_STORAGE = API_PUBLIC + "/storage";
    public static final String RESEARCH_PDFS = "/research-pdfs";
    public static final String RESEARCH_HERO_IMAGES = "/research-hero-images";

    private ApiEndpoints() {
        // Private constructor to prevent instantiation
    }
}
