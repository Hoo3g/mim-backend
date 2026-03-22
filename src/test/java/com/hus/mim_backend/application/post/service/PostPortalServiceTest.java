package com.hus.mim_backend.application.post.service;

import com.hus.mim_backend.application.port.output.PendingContentNotificationPort;
import com.hus.mim_backend.application.port.output.PostPortalRepository;
import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.post.dto.UpsertRecruitmentPostRequest;
import com.hus.mim_backend.domain.auth.model.AccountStatus;
import com.hus.mim_backend.domain.auth.model.Email;
import com.hus.mim_backend.domain.auth.model.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostPortalServiceTest {

    @Test
    void createPostShouldAutoApproveCompanyPostAndSkipPendingNotification() {
        PostPortalRepository repository = mock(PostPortalRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        PendingContentNotificationPort pendingContentNotificationPort = mock(PendingContentNotificationPort.class);
        PostPortalService service = new PostPortalService(repository, userRepository, pendingContentNotificationPort);

        String email = "company-" + UUID.randomUUID() + "@example.com";
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UpsertRecruitmentPostRequest request = newRequest("COMPANY_RECRUITING_JOB");
        PublicPostResponse createdPost = postResponse(postId, userId, "APPROVED");

        when(repository.findUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(userRepository.findById(userId)).thenReturn(Optional.of(approvedUser(userId, email)));
        when(repository.findPrimaryRole(userId)).thenReturn(Optional.of("COMPANY"));
        when(repository.createPost(eq(userId), any(UpsertRecruitmentPostRequest.class), isNull(), isNull(), eq("APPROVED")))
                .thenReturn(postId);
        when(repository.findPostByIdForAuthor(postId, userId)).thenReturn(Optional.of(createdPost));

        PublicPostResponse response = service.createPost(email, request);

        assertEquals("APPROVED", response.getApprovalStatus());
        verify(repository).createPost(eq(userId), any(UpsertRecruitmentPostRequest.class), isNull(), isNull(), eq("APPROVED"));
        verify(pendingContentNotificationPort, never()).notifyNewPendingContent(any(), any(), any(), any());
    }

    @Test
    void updatePostShouldKeepApprovedStatusForAlreadyApprovedPost() {
        PostPortalRepository repository = mock(PostPortalRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        PendingContentNotificationPort pendingContentNotificationPort = mock(PendingContentNotificationPort.class);
        PostPortalService service = new PostPortalService(repository, userRepository, pendingContentNotificationPort);

        String email = "student-" + UUID.randomUUID() + "@example.com";
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UpsertRecruitmentPostRequest request = newRequest("STUDENT_SEEKING_JOB");
        PublicPostResponse existingPost = postResponse(postId, userId, "APPROVED");
        PublicPostResponse updatedPost = postResponse(postId, userId, "APPROVED");

        when(repository.findUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(userRepository.findById(userId)).thenReturn(Optional.of(approvedUser(userId, email)));
        when(repository.findPrimaryRole(userId)).thenReturn(Optional.of("STUDENT"));
        when(repository.findPostByIdForAuthor(postId, userId)).thenReturn(Optional.of(existingPost), Optional.of(updatedPost));
        when(repository.updatePostByAuthor(
                eq(postId),
                eq(userId),
                any(UpsertRecruitmentPostRequest.class),
                isNull(),
                isNull(),
                eq("APPROVED"))).thenReturn(true);

        PublicPostResponse response = service.updatePost(email, postId, request);

        assertEquals("APPROVED", response.getApprovalStatus());
        verify(repository).updatePostByAuthor(
                eq(postId),
                eq(userId),
                any(UpsertRecruitmentPostRequest.class),
                isNull(),
                isNull(),
                eq("APPROVED"));
    }

    private static UpsertRecruitmentPostRequest newRequest(String postType) {
        UpsertRecruitmentPostRequest request = new UpsertRecruitmentPostRequest();
        request.setTitle("Recruitment post");
        request.setDescription("Recruitment description");
        request.setPostType(postType);
        request.setJobType("FULL_TIME");
        request.setStatus("OPEN");
        return request;
    }

    private static User approvedUser(UUID userId, String email) {
        return User.builder()
                .id(userId)
                .email(new Email(email))
                .status(AccountStatus.APPROVED)
                .build();
    }

    private static PublicPostResponse postResponse(UUID postId, UUID authorId, String approvalStatus) {
        PublicPostResponse response = new PublicPostResponse();
        response.setId(postId);
        response.setAuthorId(authorId);
        response.setTitle("Recruitment post");
        response.setApprovalStatus(approvalStatus);
        return response;
    }
}
