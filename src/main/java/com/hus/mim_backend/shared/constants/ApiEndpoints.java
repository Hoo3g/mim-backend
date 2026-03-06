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
    public static final String MODERATION = "/moderation";

    // Research Endpoints
    public static final String RESEARCH = API_V1 + "/research-papers";

    // Recruitment Endpoints
    public static final String RECRUITMENT = API_V1 + "/recruitment";

    // Storage Endpoints
    public static final String STORAGE = API_V1 + "/storage";
    public static final String PUBLIC_STORAGE = API_PUBLIC + "/storage";
    public static final String RESEARCH_PDFS = "/research-pdfs";

    private ApiEndpoints() {
        // Private constructor to prevent instantiation
    }
}
