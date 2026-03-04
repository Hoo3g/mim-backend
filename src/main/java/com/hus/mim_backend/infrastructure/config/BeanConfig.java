package com.hus.mim_backend.infrastructure.config;

import com.hus.mim_backend.application.auth.service.AuthServiceImpl;
import com.hus.mim_backend.application.auth.usecase.LoginUseCase;
import com.hus.mim_backend.application.auth.usecase.LogoutUseCase;
import com.hus.mim_backend.application.auth.usecase.RefreshTokenUseCase;
import com.hus.mim_backend.application.auth.usecase.RegisterUseCase;
import com.hus.mim_backend.application.moderation.service.ModerationServiceImpl;
import com.hus.mim_backend.application.moderation.usecase.ModerationUseCase;
import com.hus.mim_backend.application.news.service.NewsServiceImpl;
import com.hus.mim_backend.application.news.usecase.ManageNewsUseCase;
import com.hus.mim_backend.application.port.output.ApplicationRepository;
import com.hus.mim_backend.application.port.output.CompanyRepository;
import com.hus.mim_backend.application.port.output.LecturerRepository;
import com.hus.mim_backend.application.port.output.ModerationLogRepository;
import com.hus.mim_backend.application.port.output.NewsRepository;
import com.hus.mim_backend.application.port.output.PasswordEncoder;
import com.hus.mim_backend.application.port.output.PostRepository;
import com.hus.mim_backend.application.port.output.ResearchPaperRepository;
import com.hus.mim_backend.application.port.output.SavedPostRepository;
import com.hus.mim_backend.application.port.output.StudentRepository;
import com.hus.mim_backend.application.port.output.TokenProvider;
import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.application.post.service.PostServiceImpl;
import com.hus.mim_backend.application.post.usecase.ApplyToPostUseCase;
import com.hus.mim_backend.application.post.usecase.ManagePostUseCase;
import com.hus.mim_backend.application.profile.service.CompanyProfileService;
import com.hus.mim_backend.application.profile.service.LecturerProfileService;
import com.hus.mim_backend.application.profile.service.StudentProfileService;
import com.hus.mim_backend.application.profile.usecase.ManageCompanyProfileUseCase;
import com.hus.mim_backend.application.profile.usecase.ManageLecturerProfileUseCase;
import com.hus.mim_backend.application.profile.usecase.ManageStudentProfileUseCase;
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
            TokenProvider tokenProvider) {
        return new AuthServiceImpl(userRepository, passwordEncoder, tokenProvider);
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

    // -------------------------------------------------------
    // Post & Application
    // -------------------------------------------------------

    @Bean
    public PostServiceImpl postService(PostRepository postRepository,
            ApplicationRepository applicationRepository,
            SavedPostRepository savedPostRepository) {
        return new PostServiceImpl(postRepository, applicationRepository, savedPostRepository);
    }

    @Bean
    public ManagePostUseCase managePostUseCase(PostServiceImpl postService) {
        return postService;
    }

    @Bean
    public ApplyToPostUseCase applyToPostUseCase(PostServiceImpl postService) {
        return postService;
    }

    // -------------------------------------------------------
    // News
    // -------------------------------------------------------

    @Bean
    public NewsServiceImpl newsService(NewsRepository newsRepository) {
        return new NewsServiceImpl(newsRepository);
    }

    @Bean
    public ManageNewsUseCase manageNewsUseCase(NewsServiceImpl newsService) {
        return newsService;
    }

    // -------------------------------------------------------
    // Moderation
    // -------------------------------------------------------

    @Bean
    public ModerationServiceImpl moderationService(PostRepository postRepository,
            ResearchPaperRepository paperRepository,
            UserRepository userRepository,
            ModerationLogRepository logRepository) {
        return new ModerationServiceImpl(postRepository, paperRepository, userRepository, logRepository);
    }

    @Bean
    public ModerationUseCase moderationUseCase(ModerationServiceImpl moderationService) {
        return moderationService;
    }

    // -------------------------------------------------------
    // Profiles (3 separate services — no method conflict)
    // -------------------------------------------------------

    @Bean
    public StudentProfileService studentProfileService(StudentRepository studentRepository) {
        return new StudentProfileService(studentRepository);
    }

    @Bean
    public ManageStudentProfileUseCase manageStudentProfileUseCase(StudentProfileService service) {
        return service;
    }

    @Bean
    public CompanyProfileService companyProfileService(CompanyRepository companyRepository) {
        return new CompanyProfileService(companyRepository);
    }

    @Bean
    public ManageCompanyProfileUseCase manageCompanyProfileUseCase(CompanyProfileService service) {
        return service;
    }

    @Bean
    public LecturerProfileService lecturerProfileService(LecturerRepository lecturerRepository) {
        return new LecturerProfileService(lecturerRepository);
    }

    @Bean
    public ManageLecturerProfileUseCase manageLecturerProfileUseCase(LecturerProfileService service) {
        return service;
    }
}
