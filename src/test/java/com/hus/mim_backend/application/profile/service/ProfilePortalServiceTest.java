package com.hus.mim_backend.application.profile.service;

import com.hus.mim_backend.application.port.output.ProfilePortalRepository;
import com.hus.mim_backend.application.port.output.SpecializationRepository;
import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.application.profile.dto.ProfileDashboardResponse;
import com.hus.mim_backend.application.profile.dto.ProfileMeResponse;
import com.hus.mim_backend.application.profile.dto.UpdateCompanyProfileRequest;
import com.hus.mim_backend.application.profile.dto.UpdateLecturerProfileRequest;
import com.hus.mim_backend.application.profile.dto.UpdateStudentProfileRequest;
import com.hus.mim_backend.application.rbac.dto.PermissionDefinitionResponse;
import com.hus.mim_backend.application.rbac.dto.RolePermissionResponse;
import com.hus.mim_backend.application.rbac.dto.UpdateUserPermissionOverridesRequest;
import com.hus.mim_backend.application.rbac.dto.UpdateUserRoleRequest;
import com.hus.mim_backend.application.rbac.dto.UserRbacAssignmentResponse;
import com.hus.mim_backend.application.rbac.usecase.ManageRbacUseCase;
import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import com.hus.mim_backend.domain.auth.model.Email;
import com.hus.mim_backend.domain.auth.model.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfilePortalServiceTest {

    @Test
    void getMyProfileShouldNormalizeAndSortPermissions() {
        UUID userId = UUID.randomUUID();
        ProfilePortalService service = new ProfilePortalService(
                new StubProfilePortalRepository(profile(userId, "role_admin")),
                new NoopSpecializationRepository(),
                new NoopUserRepository(),
                new StubManageRbacUseCase(Set.of(
                        " moderation_posts_view ",
                        "admin_dashboard_view",
                        "ADMIN_DASHBOARD_VIEW")));

        ProfileMeResponse response = service.getMyProfile("admin@example.com");

        assertEquals("ADMIN", response.getRole());
        assertEquals(List.of("ADMIN_DASHBOARD_VIEW", "MODERATION_POSTS_VIEW"), response.getPermissions());
    }

    @Test
    void getMyProfileShouldFallbackToEmptyPermissionsWhenRbacLookupFails() {
        UUID userId = UUID.randomUUID();
        ProfilePortalService service = new ProfilePortalService(
                new StubProfilePortalRepository(profile(userId, "student")),
                new NoopSpecializationRepository(),
                new NoopUserRepository(),
                new StubManageRbacUseCase(new IllegalStateException("RBAC unavailable")));

        ProfileMeResponse response = service.getMyProfile("student@example.com");

        assertNotNull(response.getPermissions());
        assertTrue(response.getPermissions().isEmpty());
    }

    private static ProfileMeResponse profile(UUID userId, String role) {
        ProfileMeResponse response = new ProfileMeResponse();
        response.setUserId(userId);
        response.setEmail("user@example.com");
        response.setRole(role);
        return response;
    }

    private static final class StubProfilePortalRepository implements ProfilePortalRepository {
        private final ProfileMeResponse profile;

        private StubProfilePortalRepository(ProfileMeResponse profile) {
            this.profile = profile;
        }

        @Override
        public Optional<ProfileMeResponse> findProfileByEmail(String email) {
            return Optional.of(profile);
        }

        @Override
        public Optional<ProfileMeResponse> findProfileByUserId(UUID userId) {
            return Optional.of(profile);
        }

        @Override
        public Optional<UUID> findUserIdByEmail(String email) {
            return Optional.ofNullable(profile.getUserId());
        }

        @Override
        public Optional<String> findPrimaryRole(UUID userId) {
            return Optional.ofNullable(profile.getRole());
        }

        @Override
        public ProfileDashboardResponse.StudentDashboard getStudentDashboard(UUID userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProfileDashboardResponse.CompanyDashboard getCompanyDashboard(UUID userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProfileDashboardResponse.LecturerDashboard getLecturerDashboard(UUID userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void upsertStudentProfile(UUID userId, UpdateStudentProfileRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void upsertCompanyProfile(UUID userId, UpdateCompanyProfileRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void upsertLecturerProfile(UUID userId, UpdateLecturerProfileRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateStudentCv(UUID userId, String cvUrl) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateUserAvatar(UUID userId, String avatarUrl) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class NoopSpecializationRepository implements SpecializationRepository {
        @Override
        public List<ResearchCategoryResponse> findActiveSpecializations() {
            return List.of();
        }

        @Override
        public List<ResearchCategoryResponse> findAllSpecializations() {
            return List.of();
        }

        @Override
        public Optional<ResearchCategoryResponse> findById(UUID specializationId) {
            return Optional.empty();
        }

        @Override
        public Optional<String> findActiveSpecializationName(String specializationName) {
            return Optional.ofNullable(specializationName);
        }

        @Override
        public boolean existsSpecializationWithSameName(String specializationName) {
            return false;
        }

        @Override
        public boolean existsOtherSpecializationWithSameName(UUID specializationId, String specializationName) {
            return false;
        }

        @Override
        public UUID createSpecialization(String specializationName, int sortOrder, boolean active) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateSpecialization(UUID specializationId, String specializationName, int sortOrder, boolean active) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deactivateSpecialization(UUID specializationId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class NoopUserRepository implements UserRepository {
        @Override
        public Optional<User> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findByEmail(Email email) {
            return Optional.empty();
        }

        @Override
        public Optional<UUID> findIdByEmail(String email) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findByStudentCode(String studentCode) {
            return Optional.empty();
        }

        @Override
        public User save(User user) {
            return user;
        }

        @Override
        public boolean existsByEmail(Email email) {
            return false;
        }

        @Override
        public boolean existsByStudentCode(String studentCode) {
            return false;
        }

        @Override
        public boolean hasStudentRegistration(UUID userId) {
            return false;
        }

        @Override
        public boolean hasLecturerRegistration(UUID userId) {
            return false;
        }

        @Override
        public boolean hasCompanyRegistration(UUID userId) {
            return false;
        }

        @Override
        public void upsertStudentRegistration(UUID userId, String firstName, String lastName, String major) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void upsertStudentCode(UUID userId, String studentCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void upsertLecturerRegistration(UUID userId, String firstName, String lastName, String title) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void upsertCompanyRegistration(UUID userId, String companyName, String website) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<User> findByAccountStatus(String status) {
            return List.of();
        }

        @Override
        public void deleteById(UUID id) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class StubManageRbacUseCase implements ManageRbacUseCase {
        private final Set<String> permissions;
        private final RuntimeException failure;

        private StubManageRbacUseCase(Set<String> permissions) {
            this.permissions = permissions;
            this.failure = null;
        }

        private StubManageRbacUseCase(RuntimeException failure) {
            this.permissions = Set.of();
            this.failure = failure;
        }

        @Override
        public Set<String> getRolesByEmail(String email) {
            return Set.of();
        }

        @Override
        public Set<String> getEffectivePermissionsByEmail(String email) {
            return permissions;
        }

        @Override
        public Set<String> getEffectivePermissionsByUserId(UUID userId) {
            if (failure != null) {
                throw failure;
            }
            return permissions;
        }

        @Override
        public List<PermissionDefinitionResponse> getDelegablePermissions() {
            return List.of();
        }

        @Override
        public List<RolePermissionResponse> getRolePermissionMatrix() {
            return List.of();
        }

        @Override
        public List<UserRbacAssignmentResponse> getUserAssignments() {
            return List.of();
        }

        @Override
        public UserRbacAssignmentResponse updateUserOverrides(UUID userId, UpdateUserPermissionOverridesRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserRbacAssignmentResponse updateUserRole(String actorEmail, UUID userId, UpdateUserRoleRequest request) {
            throw new UnsupportedOperationException();
        }
    }
}
