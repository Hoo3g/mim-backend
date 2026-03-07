package com.hus.mim_backend.shared.constants;

/**
 * Canonical RBAC permission names.
 */
public final class RbacPermissions {
    public static final String RESEARCH_VIEW = "RESEARCH_VIEW";
    public static final String RESEARCH_CREATE = "RESEARCH_CREATE";
    public static final String RESEARCH_EDIT_OWN = "RESEARCH_EDIT_OWN";
    public static final String RECRUITMENT_VIEW = "RECRUITMENT_VIEW";
    public static final String ADMIN_DASHBOARD_VIEW = "ADMIN_DASHBOARD_VIEW";
    public static final String MODERATION_POSTS_VIEW = "MODERATION_POSTS_VIEW";
    public static final String MODERATION_POSTS_ACTION = "MODERATION_POSTS_ACTION";
    public static final String MODERATION_PAPERS_VIEW = "MODERATION_PAPERS_VIEW";
    public static final String MODERATION_PAPERS_ACTION = "MODERATION_PAPERS_ACTION";
    public static final String RESEARCH_HERO_EDIT = "RESEARCH_HERO_EDIT";
    public static final String RESEARCH_CATEGORY_MANAGE = "RESEARCH_CATEGORY_MANAGE";
    public static final String RBAC_MANAGE = "RBAC_MANAGE";

    private RbacPermissions() {
    }
}
