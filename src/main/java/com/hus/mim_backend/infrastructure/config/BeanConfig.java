package com.hus.mim_backend.infrastructure.config;

import com.hus.mim_backend.application.auth.service.AuthServiceImpl;
import com.hus.mim_backend.application.auth.usecase.GoogleLoginUseCase;
import com.hus.mim_backend.application.auth.usecase.LoginUseCase;
import com.hus.mim_backend.application.auth.usecase.LogoutUseCase;
import com.hus.mim_backend.application.auth.usecase.RefreshTokenUseCase;
import com.hus.mim_backend.application.auth.usecase.RegisterUseCase;
import com.hus.mim_backend.application.auth.usecase.ResendEmailVerificationUseCase;
import com.hus.mim_backend.application.auth.usecase.VerifyEmailUseCase;
import com.hus.mim_backend.application.analytics.service.AnalyticsServiceImpl;
import com.hus.mim_backend.application.analytics.usecase.QueryAdminAnalyticsUseCase;
import com.hus.mim_backend.application.analytics.usecase.RecordAnalyticsTrackingUseCase;
import com.hus.mim_backend.application.content.service.ResearchHeroContentServiceImpl;
import com.hus.mim_backend.application.content.usecase.ManageResearchHeroContentUseCase;
import com.hus.mim_backend.application.moderation.service.AdminModerationServiceImpl;
import com.hus.mim_backend.application.moderation.usecase.AdminModerationUseCase;
import com.hus.mim_backend.application.port.output.AnalyticsRepository;
import com.hus.mim_backend.application.port.output.GoogleTokenVerifier;
import com.hus.mim_backend.application.moderation.service.ModerationServiceImpl;
import com.hus.mim_backend.application.moderation.usecase.ModerationUseCase;
import com.hus.mim_backend.application.news.service.NewsServiceImpl;
import com.hus.mim_backend.application.news.usecase.ManageNewsUseCase;
import com.hus.mim_backend.application.port.output.AdminModerationRepository;
import com.hus.mim_backend.application.port.output.AdminActivityNotificationPort;
import com.hus.mim_backend.application.port.output.ApplicationRepository;
import com.hus.mim_backend.application.port.output.ApplicationPortalRepository;
import com.hus.mim_backend.application.port.output.CompanyRepository;
import com.hus.mim_backend.application.port.output.EmailVerificationNotificationPort;
import com.hus.mim_backend.application.port.output.EmailVerificationTokenRepository;
import com.hus.mim_backend.application.port.output.LecturerRepository;
import com.hus.mim_backend.application.port.output.ModerationLogRepository;
import com.hus.mim_backend.application.port.output.NewsRepository;
import com.hus.mim_backend.application.port.output.PasswordEncoder;
import com.hus.mim_backend.application.port.output.PendingContentNotificationPort;
import com.hus.mim_backend.application.port.output.PostPortalRepository;
import com.hus.mim_backend.application.port.output.PostRepository;
import com.hus.mim_backend.application.port.output.ProfilePortalRepository;
import com.hus.mim_backend.application.port.output.PublicPostPageRepository;
import com.hus.mim_backend.application.port.output.PublicPostRepository;
import com.hus.mim_backend.application.port.output.RecruitmentCategoryRepository;
import com.hus.mim_backend.application.port.output.RbacRepository;
import com.hus.mim_backend.application.port.output.RefreshTokenRepository;
import com.hus.mim_backend.application.port.output.ResearchBookmarkRepository;
import com.hus.mim_backend.application.port.output.ResearchCategoryRepository;
import com.hus.mim_backend.application.port.output.ResearchHeroContentRepository;
import com.hus.mim_backend.application.port.output.ResearchPaperPageRepository;
import com.hus.mim_backend.application.port.output.ResearchPortalRepository;
import com.hus.mim_backend.application.port.output.ResearchPaperRepository;
import com.hus.mim_backend.application.port.output.SavedPostRepository;
import com.hus.mim_backend.application.port.output.SpecializationRepository;
import com.hus.mim_backend.application.port.output.StudentRepository;
import com.hus.mim_backend.application.port.output.TokenProvider;
import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.application.post.service.PostServiceImpl;
import com.hus.mim_backend.application.post.service.PostPortalService;
import com.hus.mim_backend.application.post.service.RecruitmentCategoryServiceImpl;
import com.hus.mim_backend.application.post.service.PublicPostPageQueryServiceImpl;
import com.hus.mim_backend.application.post.service.PublicPostQueryServiceImpl;
import com.hus.mim_backend.application.post.service.ApplicationPortalService;
import com.hus.mim_backend.application.post.usecase.ApplicationPortalUseCase;
import com.hus.mim_backend.application.post.usecase.ApplyToPostUseCase;
import com.hus.mim_backend.application.post.usecase.ManageRecruitmentCategoryUseCase;
import com.hus.mim_backend.application.post.usecase.ManagePostUseCase;
import com.hus.mim_backend.application.post.usecase.PostPortalUseCase;
import com.hus.mim_backend.application.post.usecase.QueryPublicPostsPageUseCase;
import com.hus.mim_backend.application.post.usecase.QueryPublicPostsUseCase;
import com.hus.mim_backend.application.post.usecase.QueryRecruitmentCategoryUseCase;
import com.hus.mim_backend.application.profile.service.CompanyProfileService;
import com.hus.mim_backend.application.profile.service.LecturerProfileService;
import com.hus.mim_backend.application.profile.service.ProfilePortalService;
import com.hus.mim_backend.application.profile.service.StudentProfileService;
import com.hus.mim_backend.application.profile.usecase.ManageCompanyProfileUseCase;
import com.hus.mim_backend.application.profile.usecase.ManageLecturerProfileUseCase;
import com.hus.mim_backend.application.profile.usecase.ManageStudentProfileUseCase;
import com.hus.mim_backend.application.profile.usecase.ProfilePortalUseCase;
import com.hus.mim_backend.application.rbac.service.RbacServiceImpl;
import com.hus.mim_backend.application.rbac.usecase.ManageRbacUseCase;
import com.hus.mim_backend.application.research.service.ResearchBookmarkService;
import com.hus.mim_backend.application.research.service.ResearchCategoryServiceImpl;
import com.hus.mim_backend.application.research.service.PublicResearchPaperPageQueryServiceImpl;
import com.hus.mim_backend.application.research.service.ResearchPortalServiceImpl;
import com.hus.mim_backend.application.research.service.SpecializationServiceImpl;
import com.hus.mim_backend.application.research.usecase.ManageSpecializationUseCase;
import com.hus.mim_backend.application.research.usecase.ManageResearchCategoryUseCase;
import com.hus.mim_backend.application.research.usecase.ManageResearchPortalUseCase;
import com.hus.mim_backend.application.research.usecase.QueryPublicResearchPapersPageUseCase;
import com.hus.mim_backend.application.research.usecase.QueryResearchCategoryUseCase;
import com.hus.mim_backend.application.research.usecase.QuerySpecializationUseCase;
import com.hus.mim_backend.application.research.usecase.ResearchBookmarkUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bean configuration — wires all use cases with their service implementations
 * and ports.
 * Keeps Spring annotations out of the application/domain layers.
 */
@Configuration
public class BeanConfig {

    // -------------------------------------------------------
    // Auth
    // -------------------------------------------------------

    @Bean
    public AuthServiceImpl authService(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenProvider tokenProvider,
            RefreshTokenRepository refreshTokenRepository,
            GoogleTokenVerifier googleTokenVerifier,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            EmailVerificationNotificationPort emailVerificationNotificationPort,
            EmailVerificationProperties emailVerificationProperties) {
        return new AuthServiceImpl(userRepository, passwordEncoder, tokenProvider, refreshTokenRepository,
                googleTokenVerifier, emailVerificationTokenRepository, emailVerificationNotificationPort,
                emailVerificationProperties.getTokenTtlMinutes());
    }

    @Bean
    public LoginUseCase loginUseCase(AuthServiceImpl authService) {
        return authService;
    }

    @Bean
    public RegisterUseCase registerUseCase(AuthServiceImpl authService) {
        return authService;
    }

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(AuthServiceImpl authService) {
        return authService;
    }

    @Bean
    public LogoutUseCase logoutUseCase(AuthServiceImpl authService) {
        return authService;
    }

    @Bean
    public GoogleLoginUseCase googleLoginUseCase(AuthServiceImpl authService) {
        return authService;
    }

    @Bean
    public VerifyEmailUseCase verifyEmailUseCase(AuthServiceImpl authService) {
        return authService;
    }

    @Bean
    public ResendEmailVerificationUseCase resendEmailVerificationUseCase(AuthServiceImpl authService) {
        return authService;
    }

    // -------------------------------------------------------
    // RBAC
    // -------------------------------------------------------

    @Bean
    @ConditionalOnBean(RbacRepository.class)
    public RbacServiceImpl rbacService(RbacRepository rbacRepository) {
        return new RbacServiceImpl(rbacRepository);
    }

    @Bean
    @ConditionalOnBean(RbacServiceImpl.class)
    public ManageRbacUseCase manageRbacUseCase(RbacServiceImpl rbacService) {
        return rbacService;
    }

    // -------------------------------------------------------
    // Analytics
    // -------------------------------------------------------

    @Bean
    @ConditionalOnBean(AnalyticsRepository.class)
    public AnalyticsServiceImpl analyticsService(AnalyticsRepository analyticsRepository) {
        return new AnalyticsServiceImpl(analyticsRepository);
    }

    @Bean
    @ConditionalOnBean(AnalyticsServiceImpl.class)
    public RecordAnalyticsTrackingUseCase recordAnalyticsTrackingUseCase(AnalyticsServiceImpl analyticsService) {
        return analyticsService;
    }

    @Bean
    @ConditionalOnBean(AnalyticsServiceImpl.class)
    public QueryAdminAnalyticsUseCase queryAdminAnalyticsUseCase(AnalyticsServiceImpl analyticsService) {
        return analyticsService;
    }

    // -------------------------------------------------------
    // Content
    // -------------------------------------------------------

    @Bean
    @ConditionalOnBean(ResearchHeroContentRepository.class)
    public ResearchHeroContentServiceImpl researchHeroContentService(ResearchHeroContentRepository repository) {
        return new ResearchHeroContentServiceImpl(repository);
    }

    @Bean
    @ConditionalOnBean(ResearchHeroContentServiceImpl.class)
    public ManageResearchHeroContentUseCase manageResearchHeroContentUseCase(
            ResearchHeroContentServiceImpl researchHeroContentService) {
        return researchHeroContentService;
    }

    // -------------------------------------------------------
    // Post & Application
    // -------------------------------------------------------

    @Bean
    @ConditionalOnBean({ PostRepository.class, ApplicationRepository.class, SavedPostRepository.class })
    public PostServiceImpl postService(PostRepository postRepository,
            ApplicationRepository applicationRepository,
            SavedPostRepository savedPostRepository) {
        return new PostServiceImpl(postRepository, applicationRepository, savedPostRepository);
    }

    @Bean
    @ConditionalOnBean(PostServiceImpl.class)
    public ManagePostUseCase managePostUseCase(PostServiceImpl postService) {
        return postService;
    }

    @Bean
    @ConditionalOnBean(PostServiceImpl.class)
    public ApplyToPostUseCase applyToPostUseCase(PostServiceImpl postService) {
        return postService;
    }

    @Bean
    @ConditionalOnBean(PublicPostRepository.class)
    public PublicPostQueryServiceImpl publicPostQueryService(PublicPostRepository publicPostRepository) {
        return new PublicPostQueryServiceImpl(publicPostRepository);
    }

    @Bean
    @ConditionalOnBean(PublicPostQueryServiceImpl.class)
    public QueryPublicPostsUseCase queryPublicPostsUseCase(PublicPostQueryServiceImpl publicPostQueryService) {
        return publicPostQueryService;
    }

    @Bean
    @ConditionalOnBean(PublicPostPageRepository.class)
    public PublicPostPageQueryServiceImpl publicPostPageQueryService(PublicPostPageRepository publicPostPageRepository) {
        return new PublicPostPageQueryServiceImpl(publicPostPageRepository);
    }

    @Bean
    @ConditionalOnBean(PublicPostPageQueryServiceImpl.class)
    public QueryPublicPostsPageUseCase queryPublicPostsPageUseCase(
            PublicPostPageQueryServiceImpl publicPostPageQueryService) {
        return publicPostPageQueryService;
    }

    @Bean
    @ConditionalOnBean({ PostPortalRepository.class, RecruitmentCategoryRepository.class })
    public PostPortalService postPortalService(PostPortalRepository postPortalRepository,
            UserRepository userRepository,
            RecruitmentCategoryRepository recruitmentCategoryRepository,
            PendingContentNotificationPort pendingContentNotificationPort) {
        return new PostPortalService(
                postPortalRepository,
                userRepository,
                recruitmentCategoryRepository,
                pendingContentNotificationPort);
    }

    @Bean
    @ConditionalOnBean(PostPortalService.class)
    public PostPortalUseCase postPortalUseCase(PostPortalService postPortalService) {
        return postPortalService;
    }

    @Bean
    @ConditionalOnBean(ApplicationPortalRepository.class)
    public ApplicationPortalService applicationPortalService(ApplicationPortalRepository applicationPortalRepository) {
        return new ApplicationPortalService(applicationPortalRepository);
    }

    @Bean
    @ConditionalOnBean(ApplicationPortalService.class)
    public ApplicationPortalUseCase applicationPortalUseCase(ApplicationPortalService applicationPortalService) {
        return applicationPortalService;
    }

    // -------------------------------------------------------
    // News
    // -------------------------------------------------------

    @Bean
    @ConditionalOnBean(NewsRepository.class)
    public NewsServiceImpl newsService(NewsRepository newsRepository) {
        return new NewsServiceImpl(newsRepository);
    }

    @Bean
    @ConditionalOnBean(NewsServiceImpl.class)
    public ManageNewsUseCase manageNewsUseCase(NewsServiceImpl newsService) {
        return newsService;
    }

    // -------------------------------------------------------
    // Moderation
    // -------------------------------------------------------

    @Bean
    @ConditionalOnBean({ PostRepository.class, ResearchPaperRepository.class, UserRepository.class,
            ModerationLogRepository.class })
    public ModerationServiceImpl moderationService(PostRepository postRepository,
            ResearchPaperRepository paperRepository,
            UserRepository userRepository,
            ModerationLogRepository logRepository) {
        return new ModerationServiceImpl(postRepository, paperRepository, userRepository, logRepository);
    }

    @Bean
    @ConditionalOnBean(ModerationServiceImpl.class)
    public ModerationUseCase moderationUseCase(ModerationServiceImpl moderationService) {
        return moderationService;
    }

    @Bean
    @ConditionalOnBean({ AdminModerationRepository.class, AdminActivityNotificationPort.class })
    public AdminModerationServiceImpl adminModerationService(AdminModerationRepository adminModerationRepository,
            AdminActivityNotificationPort adminActivityNotificationPort) {
        return new AdminModerationServiceImpl(adminModerationRepository, adminActivityNotificationPort);
    }

    @Bean
    @ConditionalOnBean(AdminModerationServiceImpl.class)
    public AdminModerationUseCase adminModerationUseCase(AdminModerationServiceImpl adminModerationService) {
        return adminModerationService;
    }

    // -------------------------------------------------------
    // Research Portal
    // -------------------------------------------------------

    @Bean
    @ConditionalOnBean(ResearchPortalRepository.class)
    public ResearchPortalServiceImpl researchPortalService(ResearchPortalRepository researchPortalRepository,
            UserRepository userRepository,
            PendingContentNotificationPort pendingContentNotificationPort) {
        return new ResearchPortalServiceImpl(researchPortalRepository, userRepository, pendingContentNotificationPort);
    }

    @Bean
    @ConditionalOnBean(ResearchPortalServiceImpl.class)
    public ManageResearchPortalUseCase manageResearchPortalUseCase(ResearchPortalServiceImpl researchPortalService) {
        return researchPortalService;
    }

    @Bean
    @ConditionalOnBean(ResearchPaperPageRepository.class)
    public PublicResearchPaperPageQueryServiceImpl publicResearchPaperPageQueryService(
            ResearchPaperPageRepository researchPaperPageRepository) {
        return new PublicResearchPaperPageQueryServiceImpl(researchPaperPageRepository);
    }

    @Bean
    @ConditionalOnBean(PublicResearchPaperPageQueryServiceImpl.class)
    public QueryPublicResearchPapersPageUseCase queryPublicResearchPapersPageUseCase(
            PublicResearchPaperPageQueryServiceImpl publicResearchPaperPageQueryService) {
        return publicResearchPaperPageQueryService;
    }

    @Bean
    @ConditionalOnBean(ResearchBookmarkRepository.class)
    public ResearchBookmarkService researchBookmarkService(ResearchBookmarkRepository researchBookmarkRepository) {
        return new ResearchBookmarkService(researchBookmarkRepository);
    }

    @Bean
    @ConditionalOnBean(ResearchBookmarkService.class)
    public ResearchBookmarkUseCase researchBookmarkUseCase(ResearchBookmarkService researchBookmarkService) {
        return researchBookmarkService;
    }

    @Bean
    @ConditionalOnBean(ResearchCategoryRepository.class)
    public ResearchCategoryServiceImpl researchCategoryService(ResearchCategoryRepository researchCategoryRepository) {
        return new ResearchCategoryServiceImpl(researchCategoryRepository);
    }

    @Bean
    @ConditionalOnBean(ResearchCategoryServiceImpl.class)
    public QueryResearchCategoryUseCase queryResearchCategoryUseCase(ResearchCategoryServiceImpl researchCategoryService) {
        return researchCategoryService;
    }

    @Bean
    @ConditionalOnBean(ResearchCategoryServiceImpl.class)
    public ManageResearchCategoryUseCase manageResearchCategoryUseCase(
            ResearchCategoryServiceImpl researchCategoryService) {
        return researchCategoryService;
    }

    @Bean
    @ConditionalOnBean(SpecializationRepository.class)
    public SpecializationServiceImpl specializationService(SpecializationRepository specializationRepository) {
        return new SpecializationServiceImpl(specializationRepository);
    }

    @Bean
    @ConditionalOnBean(SpecializationServiceImpl.class)
    public QuerySpecializationUseCase querySpecializationUseCase(SpecializationServiceImpl specializationService) {
        return specializationService;
    }

    @Bean
    @ConditionalOnBean(SpecializationServiceImpl.class)
    public ManageSpecializationUseCase manageSpecializationUseCase(SpecializationServiceImpl specializationService) {
        return specializationService;
    }

    @Bean
    @ConditionalOnBean(RecruitmentCategoryRepository.class)
    public RecruitmentCategoryServiceImpl recruitmentCategoryService(
            RecruitmentCategoryRepository recruitmentCategoryRepository) {
        return new RecruitmentCategoryServiceImpl(recruitmentCategoryRepository);
    }

    @Bean
    @ConditionalOnBean(RecruitmentCategoryServiceImpl.class)
    public QueryRecruitmentCategoryUseCase queryRecruitmentCategoryUseCase(
            RecruitmentCategoryServiceImpl recruitmentCategoryService) {
        return recruitmentCategoryService;
    }

    @Bean
    @ConditionalOnBean(RecruitmentCategoryServiceImpl.class)
    public ManageRecruitmentCategoryUseCase manageRecruitmentCategoryUseCase(
            RecruitmentCategoryServiceImpl recruitmentCategoryService) {
        return recruitmentCategoryService;
    }

    // -------------------------------------------------------
    // Profiles (3 separate services — no method conflict)
    // -------------------------------------------------------

    @Bean
    @ConditionalOnBean(StudentRepository.class)
    public StudentProfileService studentProfileService(StudentRepository studentRepository) {
        return new StudentProfileService(studentRepository);
    }

    @Bean
    @ConditionalOnBean(StudentProfileService.class)
    public ManageStudentProfileUseCase manageStudentProfileUseCase(StudentProfileService service) {
        return service;
    }

    @Bean
    @ConditionalOnBean(CompanyRepository.class)
    public CompanyProfileService companyProfileService(CompanyRepository companyRepository) {
        return new CompanyProfileService(companyRepository);
    }

    @Bean
    @ConditionalOnBean(CompanyProfileService.class)
    public ManageCompanyProfileUseCase manageCompanyProfileUseCase(CompanyProfileService service) {
        return service;
    }

    @Bean
    @ConditionalOnBean(LecturerRepository.class)
    public LecturerProfileService lecturerProfileService(LecturerRepository lecturerRepository) {
        return new LecturerProfileService(lecturerRepository);
    }

    @Bean
    @ConditionalOnBean(LecturerProfileService.class)
    public ManageLecturerProfileUseCase manageLecturerProfileUseCase(LecturerProfileService service) {
        return service;
    }

    @Bean
    @ConditionalOnBean({ ProfilePortalRepository.class, SpecializationRepository.class, UserRepository.class,
            ManageRbacUseCase.class })
    public ProfilePortalService profilePortalService(ProfilePortalRepository profilePortalRepository,
            SpecializationRepository specializationRepository,
            UserRepository userRepository,
            ManageRbacUseCase manageRbacUseCase) {
        return new ProfilePortalService(profilePortalRepository, specializationRepository, userRepository,
                manageRbacUseCase);
    }

    @Bean
    @ConditionalOnBean(ProfilePortalService.class)
    public ProfilePortalUseCase profilePortalUseCase(ProfilePortalService profilePortalService) {
        return profilePortalService;
    }
}
