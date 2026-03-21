package com.hus.mim_backend.infrastructure.adapter.persistence.post;

import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.post.dto.UpsertRecruitmentPostRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JdbcPostPortalRepositoryTest {

    @Autowired
    private JdbcPostPortalRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createPostShouldPersistRowAndReturnReadablePost() {
        UUID authorId = UUID.randomUUID();
        String email = "post-test-" + UUID.randomUUID() + "@example.com";

        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password, account_status)
                VALUES (?, ?, ?, ?)
                """,
                authorId,
                email,
                "noop-password",
                "APPROVED");

        UpsertRecruitmentPostRequest request = new UpsertRecruitmentPostRequest();
        request.setTitle("Repository create post test");
        request.setDescription("Ensures INSERT works without JDBC result-set mismatch.");
        request.setPostType("STUDENT_SEEKING_JOB");
        request.setJobType("FULL_TIME");
        request.setStatus("OPEN");
        request.setLocation("Ha Noi");
        request.setContactEmail(email);
        request.setTags(List.of("AI", "Data"));

        UUID postId = repository.createPost(authorId, request, null, "AI,Data");

        Optional<PublicPostResponse> created = repository.findPostByIdForAuthor(postId, authorId);

        assertTrue(created.isPresent());
        assertEquals("Repository create post test", created.get().getTitle());
        assertEquals("OPEN", created.get().getStatus());
        assertEquals(List.of("AI", "Data"), created.get().getTags());
    }

    @Test
    void createPostShouldAllowEmptyTags() {
        UUID authorId = UUID.randomUUID();
        String email = "post-empty-tags-" + UUID.randomUUID() + "@example.com";

        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password, account_status)
                VALUES (?, ?, ?, ?)
                """,
                authorId,
                email,
                "noop-password",
                "APPROVED");

        UpsertRecruitmentPostRequest request = new UpsertRecruitmentPostRequest();
        request.setTitle("Post without tags");
        request.setDescription("Ensures null tags do not break PostgreSQL prepared statements.");
        request.setPostType("STUDENT_SEEKING_JOB");
        request.setJobType("FULL_TIME");
        request.setStatus("OPEN");
        request.setLocation("Ha Noi");
        request.setContactEmail(email);
        request.setTags(null);

        UUID postId = repository.createPost(authorId, request, null, null);

        Optional<PublicPostResponse> created = repository.findPostByIdForAuthor(postId, authorId);

        assertTrue(created.isPresent());
        assertEquals("Post without tags", created.get().getTitle());
        assertTrue(created.get().getTags() == null || created.get().getTags().isEmpty());
    }
}
