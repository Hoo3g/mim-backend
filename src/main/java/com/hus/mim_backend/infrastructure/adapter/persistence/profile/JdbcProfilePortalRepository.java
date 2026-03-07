package com.hus.mim_backend.infrastructure.adapter.persistence.profile;

import com.hus.mim_backend.application.port.output.ProfilePortalRepository;
import com.hus.mim_backend.application.profile.dto.ProfileDashboardResponse;
import com.hus.mim_backend.application.profile.dto.ProfileMeResponse;
import com.hus.mim_backend.application.profile.dto.UpdateCompanyProfileRequest;
import com.hus.mim_backend.application.profile.dto.UpdateLecturerProfileRequest;
import com.hus.mim_backend.application.profile.dto.UpdateStudentProfileRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JdbcProfilePortalRepository implements ProfilePortalRepository {
    private static final String SELECT_PROFILE_BY_EMAIL_SQL = """
            SELECT u.id,
                   u.email,
                   u.avatar_url,
                   u.account_status,
                   COALESCE((
                       SELECT r.name
                       FROM roles r
                       JOIN user_roles ur ON ur.role_id = r.id
                       WHERE ur.user_id = u.id
                       ORDER BY CASE r.name
                           WHEN 'ADMIN' THEN 1
                           WHEN 'LECTURER' THEN 2
                           WHEN 'COMPANY' THEN 3
                           WHEN 'STUDENT' THEN 4
                           ELSE 99
                       END
                       LIMIT 1
                   ), 'STUDENT') AS primary_role,
                   s.first_name AS student_first_name,
                   s.last_name AS student_last_name,
                   s.university AS student_university,
                   s.major AS student_major,
                   s.bio AS student_bio,
                   s.cv_url AS student_cv_url,
                   s.student_type,
                   s.student_code,
                   s.achievements,
                   s.career_goal,
                   s.desired_position,
                   c.name AS company_name,
                   c.industry AS company_industry,
                   c.website AS company_website,
                   c.location AS company_location,
                   c.description AS company_description,
                   c.logo_url AS company_logo_url,
                   l.first_name AS lecturer_first_name,
                   l.last_name AS lecturer_last_name,
                   l.title AS lecturer_title,
                   l.academic_rank AS lecturer_academic_rank,
                   l.bio AS lecturer_bio,
                   l.avatar_url AS lecturer_avatar_url,
                   l.research_interests
            FROM users u
            LEFT JOIN students s ON s.id = u.id
            LEFT JOIN companies c ON c.id = u.id
            LEFT JOIN lecturers l ON l.id = u.id
            WHERE u.email = ?
            """;

    private static final String SELECT_USER_ID_BY_EMAIL_SQL = """
            SELECT id FROM users WHERE email = ?
            """;

    private static final String SELECT_PRIMARY_ROLE_SQL = """
            SELECT COALESCE(r.name, 'STUDENT')
            FROM users u
            LEFT JOIN user_roles ur ON ur.user_id = u.id
            LEFT JOIN roles r ON r.id = ur.role_id
            WHERE u.id = ?
            ORDER BY CASE r.name
                WHEN 'ADMIN' THEN 1
                WHEN 'LECTURER' THEN 2
                WHEN 'COMPANY' THEN 3
                WHEN 'STUDENT' THEN 4
                ELSE 99
            END
            LIMIT 1
            """;

    private static final String SELECT_STUDENT_SAVED_PAPERS_SQL = """
            SELECT sr.paper_id,
                   rp.title,
                   COALESCE(rp.research_area, 'Chưa phân loại') AS research_area,
                   COALESCE(rp.category, 'STUDENT') AS category,
                   rp.publication_year,
                   sr.created_at
            FROM saved_research_papers sr
            JOIN research_papers rp ON rp.id = sr.paper_id
            WHERE sr.user_id = ?
              AND COALESCE(rp.approval_status, 'PENDING') = 'APPROVED'
            ORDER BY sr.created_at DESC
            """;

    private static final String SELECT_STUDENT_PENDING_APPLICATIONS_SQL = """
            SELECT a.id AS application_id,
                   p.id AS post_id,
                   p.title AS post_title,
                   p.post_type,
                   p.location,
                   a.status,
                   a.created_at,
                   COALESCE(
                       NULLIF(c.name, ''),
                       NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), ''),
                       NULLIF(TRIM(COALESCE(l.first_name, '') || ' ' || COALESCE(l.last_name, '')), ''),
                       SPLIT_PART(COALESCE(u.email, ''), '@', 1),
                       'Unknown'
                   ) AS company_name
            FROM applications a
            JOIN posts p ON p.id = a.post_id
            LEFT JOIN users u ON u.id = p.author_id
            LEFT JOIN companies c ON c.id = p.author_id
            LEFT JOIN students s ON s.id = p.author_id
            LEFT JOIN lecturers l ON l.id = p.author_id
            WHERE a.applicant_id = ?
              AND a.status = 'PENDING'
            ORDER BY a.created_at DESC
            """;

    private static final String SELECT_COMPANY_POSTS_SQL = """
            SELECT p.id AS post_id,
                   p.title,
                   p.status,
                   p.approval_status,
                   p.created_at,
                   COALESCE(SUM(CASE WHEN a.status = 'PENDING' THEN 1 ELSE 0 END), 0) AS pending_count
            FROM posts p
            LEFT JOIN applications a ON a.post_id = p.id
            WHERE p.author_id = ?
              AND p.post_type LIKE 'COMPANY_%'
            GROUP BY p.id, p.title, p.status, p.approval_status, p.created_at
            ORDER BY p.created_at DESC
            """;

    private static final String SELECT_COMPANY_PENDING_APPLICANTS_SQL = """
            SELECT a.id AS application_id,
                   p.id AS post_id,
                   p.title AS post_title,
                   a.applicant_id,
                   COALESCE(
                       NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), ''),
                       NULLIF(TRIM(COALESCE(l.first_name, '') || ' ' || COALESCE(l.last_name, '')), ''),
                       NULLIF(c.name, ''),
                       SPLIT_PART(COALESCE(u.email, ''), '@', 1),
                       'Unknown'
                   ) AS applicant_name,
                   a.message,
                   a.cv_url,
                   a.created_at
            FROM applications a
            JOIN posts p ON p.id = a.post_id
            LEFT JOIN users u ON u.id = a.applicant_id
            LEFT JOIN students s ON s.id = a.applicant_id
            LEFT JOIN lecturers l ON l.id = a.applicant_id
            LEFT JOIN companies c ON c.id = a.applicant_id
            WHERE p.author_id = ?
              AND p.post_type LIKE 'COMPANY_%'
              AND a.status = 'PENDING'
            ORDER BY a.created_at DESC
            """;

    private static final String SELECT_LECTURER_PAPERS_SQL = """
            SELECT DISTINCT rp.id AS paper_id,
                   rp.title,
                   COALESCE(rp.research_area, 'Chưa phân loại') AS research_area,
                   COALESCE(rp.approval_status, 'PENDING') AS approval_status,
                   rp.publication_year,
                   rp.created_at
            FROM research_papers rp
            JOIN paper_authors pa ON pa.paper_id = rp.id
            WHERE pa.lecturer_id = ?
            ORDER BY rp.created_at DESC
            """;

    private static final String SELECT_LECTURER_COLLABORATORS_SQL = """
            SELECT COALESCE(pa.student_id, pa.lecturer_id) AS collaborator_id,
                   CASE WHEN pa.student_id IS NOT NULL THEN 'STUDENT' ELSE 'LECTURER' END AS collaborator_type,
                   COALESCE(
                       NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), ''),
                       NULLIF(TRIM(COALESCE(l.first_name, '') || ' ' || COALESCE(l.last_name, '')), ''),
                       SPLIT_PART(COALESCE(u.email, ''), '@', 1),
                       'Unknown'
                   ) AS collaborator_name,
                   COUNT(DISTINCT pa.paper_id) AS paper_count
            FROM paper_authors owner
            JOIN paper_authors pa ON pa.paper_id = owner.paper_id
            LEFT JOIN students s ON s.id = pa.student_id
            LEFT JOIN lecturers l ON l.id = pa.lecturer_id
            LEFT JOIN users u ON u.id = COALESCE(pa.student_id, pa.lecturer_id)
            WHERE owner.lecturer_id = ?
              AND COALESCE(pa.student_id, pa.lecturer_id) IS NOT NULL
              AND COALESCE(pa.student_id, pa.lecturer_id) <> ?
            GROUP BY collaborator_id, collaborator_type, collaborator_name
            ORDER BY paper_count DESC, collaborator_name ASC
            """;

    private static final String UPSERT_STUDENT_PROFILE_SQL = """
            INSERT INTO students (
                id,
                first_name,
                last_name,
                university,
                major,
                bio,
                cv_url,
                student_type,
                achievements,
                career_goal,
                desired_position,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET
                first_name = EXCLUDED.first_name,
                last_name = EXCLUDED.last_name,
                university = EXCLUDED.university,
                major = EXCLUDED.major,
                bio = EXCLUDED.bio,
                cv_url = COALESCE(EXCLUDED.cv_url, students.cv_url),
                student_type = EXCLUDED.student_type,
                achievements = EXCLUDED.achievements,
                career_goal = EXCLUDED.career_goal,
                desired_position = EXCLUDED.desired_position,
                updated_at = CURRENT_TIMESTAMP
            """;

    private static final String UPSERT_COMPANY_PROFILE_SQL = """
            INSERT INTO companies (
                id,
                name,
                industry,
                website,
                location,
                description,
                logo_url,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                industry = EXCLUDED.industry,
                website = EXCLUDED.website,
                location = EXCLUDED.location,
                description = EXCLUDED.description,
                logo_url = EXCLUDED.logo_url,
                updated_at = CURRENT_TIMESTAMP
            """;

    private static final String UPSERT_LECTURER_PROFILE_SQL = """
            INSERT INTO lecturers (
                id,
                first_name,
                last_name,
                title,
                academic_rank,
                bio,
                research_interests,
                avatar_url,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET
                first_name = EXCLUDED.first_name,
                last_name = EXCLUDED.last_name,
                title = EXCLUDED.title,
                academic_rank = EXCLUDED.academic_rank,
                bio = EXCLUDED.bio,
                research_interests = EXCLUDED.research_interests,
                avatar_url = EXCLUDED.avatar_url,
                updated_at = CURRENT_TIMESTAMP
            """;

    private static final String UPDATE_STUDENT_CV_SQL = """
            INSERT INTO students (id, cv_url, updated_at)
            VALUES (?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET
                cv_url = EXCLUDED.cv_url,
                updated_at = CURRENT_TIMESTAMP
            """;

    private static final String UPDATE_USER_AVATAR_SQL = """
            UPDATE users
            SET avatar_url = ?
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcProfilePortalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ProfileMeResponse> findProfileByEmail(String email) {
        List<ProfileMeResponse> rows = jdbcTemplate.query(SELECT_PROFILE_BY_EMAIL_SQL, (rs, rowNum) -> {
            ProfileMeResponse response = new ProfileMeResponse();
            response.setUserId(rs.getObject("id", UUID.class));
            response.setEmail(rs.getString("email"));
            response.setAvatarUrl(rs.getString("avatar_url"));
            response.setAccountStatus(rs.getString("account_status"));
            response.setRole(rs.getString("primary_role"));

            ProfileMeResponse.StudentProfile student = new ProfileMeResponse.StudentProfile();
            student.setFirstName(rs.getString("student_first_name"));
            student.setLastName(rs.getString("student_last_name"));
            student.setUniversity(rs.getString("student_university"));
            student.setMajor(rs.getString("student_major"));
            student.setBio(rs.getString("student_bio"));
            student.setCvUrl(rs.getString("student_cv_url"));
            student.setStudentType(rs.getString("student_type"));
            student.setStudentCode(rs.getString("student_code"));
            student.setAchievements(rs.getString("achievements"));
            student.setCareerGoal(rs.getString("career_goal"));
            student.setDesiredPosition(rs.getString("desired_position"));
            response.setStudent(student);

            ProfileMeResponse.CompanyProfile company = new ProfileMeResponse.CompanyProfile();
            company.setName(rs.getString("company_name"));
            company.setIndustry(rs.getString("company_industry"));
            company.setWebsite(rs.getString("company_website"));
            company.setLocation(rs.getString("company_location"));
            company.setDescription(rs.getString("company_description"));
            company.setLogoUrl(rs.getString("company_logo_url"));
            response.setCompany(company);

            ProfileMeResponse.LecturerProfile lecturer = new ProfileMeResponse.LecturerProfile();
            lecturer.setFirstName(rs.getString("lecturer_first_name"));
            lecturer.setLastName(rs.getString("lecturer_last_name"));
            lecturer.setTitle(rs.getString("lecturer_title"));
            lecturer.setAcademicRank(rs.getString("lecturer_academic_rank"));
            lecturer.setBio(rs.getString("lecturer_bio"));
            lecturer.setAvatarUrl(rs.getString("lecturer_avatar_url"));
            lecturer.setResearchInterests(toStringList(rs.getArray("research_interests")));
            response.setLecturer(lecturer);
            return response;
        }, email);

        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Override
    public Optional<UUID> findUserIdByEmail(String email) {
        List<UUID> ids = jdbcTemplate.query(SELECT_USER_ID_BY_EMAIL_SQL,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                email);
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ids.getFirst());
    }

    @Override
    public Optional<String> findPrimaryRole(UUID userId) {
        List<String> roles = jdbcTemplate.query(SELECT_PRIMARY_ROLE_SQL,
                (rs, rowNum) -> rs.getString(1),
                userId);
        if (roles.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(roles.getFirst());
    }

    @Override
    public ProfileDashboardResponse.StudentDashboard getStudentDashboard(UUID userId) {
        ProfileDashboardResponse.StudentDashboard dashboard = new ProfileDashboardResponse.StudentDashboard();

        List<ProfileDashboardResponse.SavedPaperItem> savedPapers = jdbcTemplate.query(
                SELECT_STUDENT_SAVED_PAPERS_SQL,
                (rs, rowNum) -> {
                    ProfileDashboardResponse.SavedPaperItem item = new ProfileDashboardResponse.SavedPaperItem();
                    item.setPaperId(rs.getObject("paper_id", UUID.class));
                    item.setTitle(rs.getString("title"));
                    item.setResearchArea(rs.getString("research_area"));
                    item.setCategory(rs.getString("category"));
                    item.setPublicationYear(rs.getObject("publication_year", Integer.class));
                    item.setSavedAt(toLocalDateTime(rs.getTimestamp("created_at")));
                    return item;
                },
                userId);

        List<ProfileDashboardResponse.PendingApplicationItem> pendingApplications = jdbcTemplate.query(
                SELECT_STUDENT_PENDING_APPLICATIONS_SQL,
                (rs, rowNum) -> {
                    ProfileDashboardResponse.PendingApplicationItem item = new ProfileDashboardResponse.PendingApplicationItem();
                    item.setApplicationId(rs.getObject("application_id", UUID.class));
                    item.setPostId(rs.getObject("post_id", UUID.class));
                    item.setPostTitle(rs.getString("post_title"));
                    item.setCompanyName(rs.getString("company_name"));
                    item.setPostType(rs.getString("post_type"));
                    item.setLocation(rs.getString("location"));
                    item.setStatus(rs.getString("status"));
                    item.setAppliedAt(toLocalDateTime(rs.getTimestamp("created_at")));
                    return item;
                },
                userId);

        dashboard.setSavedPapers(savedPapers);
        dashboard.setPendingApplications(pendingApplications);
        return dashboard;
    }

    @Override
    public ProfileDashboardResponse.CompanyDashboard getCompanyDashboard(UUID userId) {
        ProfileDashboardResponse.CompanyDashboard dashboard = new ProfileDashboardResponse.CompanyDashboard();

        List<ProfileDashboardResponse.CompanyPostItem> posts = jdbcTemplate.query(
                SELECT_COMPANY_POSTS_SQL,
                (rs, rowNum) -> {
                    ProfileDashboardResponse.CompanyPostItem item = new ProfileDashboardResponse.CompanyPostItem();
                    item.setPostId(rs.getObject("post_id", UUID.class));
                    item.setTitle(rs.getString("title"));
                    item.setStatus(rs.getString("status"));
                    item.setApprovalStatus(rs.getString("approval_status"));
                    item.setPendingCount(rs.getInt("pending_count"));
                    item.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
                    return item;
                },
                userId);

        List<ProfileDashboardResponse.PendingApplicantItem> pendingApplicants = jdbcTemplate.query(
                SELECT_COMPANY_PENDING_APPLICANTS_SQL,
                (rs, rowNum) -> {
                    ProfileDashboardResponse.PendingApplicantItem item = new ProfileDashboardResponse.PendingApplicantItem();
                    item.setApplicationId(rs.getObject("application_id", UUID.class));
                    item.setPostId(rs.getObject("post_id", UUID.class));
                    item.setPostTitle(rs.getString("post_title"));
                    item.setApplicantId(rs.getObject("applicant_id", UUID.class));
                    item.setApplicantName(rs.getString("applicant_name"));
                    item.setMessage(rs.getString("message"));
                    item.setCvUrl(rs.getString("cv_url"));
                    item.setAppliedAt(toLocalDateTime(rs.getTimestamp("created_at")));
                    return item;
                },
                userId);

        dashboard.setMyPosts(posts);
        dashboard.setPendingApplicants(pendingApplicants);
        return dashboard;
    }

    @Override
    public ProfileDashboardResponse.LecturerDashboard getLecturerDashboard(UUID userId) {
        ProfileDashboardResponse.LecturerDashboard dashboard = new ProfileDashboardResponse.LecturerDashboard();

        List<ProfileDashboardResponse.LecturerPaperItem> papers = jdbcTemplate.query(
                SELECT_LECTURER_PAPERS_SQL,
                (rs, rowNum) -> {
                    ProfileDashboardResponse.LecturerPaperItem item = new ProfileDashboardResponse.LecturerPaperItem();
                    item.setPaperId(rs.getObject("paper_id", UUID.class));
                    item.setTitle(rs.getString("title"));
                    item.setResearchArea(rs.getString("research_area"));
                    item.setApprovalStatus(rs.getString("approval_status"));
                    item.setPublicationYear(rs.getObject("publication_year", Integer.class));
                    item.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
                    return item;
                },
                userId);

        List<ProfileDashboardResponse.CollaboratorItem> collaborators = jdbcTemplate.query(
                SELECT_LECTURER_COLLABORATORS_SQL,
                (rs, rowNum) -> {
                    ProfileDashboardResponse.CollaboratorItem item = new ProfileDashboardResponse.CollaboratorItem();
                    item.setCollaboratorId(rs.getObject("collaborator_id", UUID.class));
                    item.setName(rs.getString("collaborator_name"));
                    item.setCollaboratorType(rs.getString("collaborator_type"));
                    item.setPaperCount(rs.getInt("paper_count"));
                    return item;
                },
                userId,
                userId);

        dashboard.setMyPapers(papers);
        dashboard.setCollaborators(collaborators);
        return dashboard;
    }

    @Override
    public void upsertStudentProfile(UUID userId, UpdateStudentProfileRequest request) {
        jdbcTemplate.update(
                UPSERT_STUDENT_PROFILE_SQL,
                userId,
                trimToNull(request.getFirstName()),
                trimToNull(request.getLastName()),
                trimToNull(request.getUniversity()),
                trimToNull(request.getMajor()),
                trimToNull(request.getBio()),
                trimToNull(request.getCvUrl()),
                trimToNull(request.getStudentType()),
                trimToNull(request.getAchievements()),
                trimToNull(request.getCareerGoal()),
                trimToNull(request.getDesiredPosition()));
    }

    @Override
    public void upsertCompanyProfile(UUID userId, UpdateCompanyProfileRequest request) {
        jdbcTemplate.update(
                UPSERT_COMPANY_PROFILE_SQL,
                userId,
                coalesceName(request.getName()),
                trimToNull(request.getIndustry()),
                trimToNull(request.getWebsite()),
                trimToNull(request.getLocation()),
                trimToNull(request.getDescription()),
                trimToNull(request.getLogoUrl()));
    }

    @Override
    public void upsertLecturerProfile(UUID userId, UpdateLecturerProfileRequest request) {
        List<String> interests = request.getResearchInterests() == null ? List.of() : request.getResearchInterests().stream()
                .map(this::trimToNull)
                .filter(item -> item != null && !item.isBlank())
                .toList();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(UPSERT_LECTURER_PROFILE_SQL);
            ps.setObject(1, userId);
            ps.setString(2, trimToNull(request.getFirstName()));
            ps.setString(3, trimToNull(request.getLastName()));
            ps.setString(4, trimToNull(request.getTitle()));
            ps.setString(5, trimToNull(request.getAcademicRank()));
            ps.setString(6, trimToNull(request.getBio()));
            Array array = connection.createArrayOf("text", interests.toArray(new String[0]));
            ps.setArray(7, array);
            ps.setString(8, trimToNull(request.getAvatarUrl()));
            return ps;
        });
    }

    @Override
    public void updateStudentCv(UUID userId, String cvUrl) {
        jdbcTemplate.update(UPDATE_STUDENT_CV_SQL, userId, trimToNull(cvUrl));
    }

    @Override
    public void updateUserAvatar(UUID userId, String avatarUrl) {
        jdbcTemplate.update(UPDATE_USER_AVATAR_SQL, trimToNull(avatarUrl), userId);
    }

    private String coalesceName(String name) {
        String value = trimToNull(name);
        if (value == null) {
            return "Doanh nghiệp";
        }
        return value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime();
    }

    private List<String> toStringList(Array sqlArray) {
        if (sqlArray == null) {
            return new ArrayList<>();
        }
        try {
            Object value = sqlArray.getArray();
            if (value instanceof String[] items) {
                return Arrays.stream(items).toList();
            }
        } catch (SQLException ignored) {
            // return empty list
        }
        return new ArrayList<>();
    }
}
