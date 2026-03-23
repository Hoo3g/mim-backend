package com.hus.mim_backend.application.post.service;

import com.hus.mim_backend.application.port.output.ApplicationPortalRepository;
import com.hus.mim_backend.domain.shared.DomainException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationPortalServiceTest {

    @Test
    void cancelApplicationShouldDeletePendingApplicationForStudent() {
        ApplicationPortalRepository repository = mock(ApplicationPortalRepository.class);
        ApplicationPortalService service = new ApplicationPortalService(repository);

        String email = "student-" + UUID.randomUUID() + "@example.com";
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();

        when(repository.findUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(repository.findPrimaryRole(userId)).thenReturn(Optional.of("STUDENT"));
        when(repository.deletePendingApplication(postId, userId)).thenReturn(true);

        boolean cancelled = service.cancelApplication(email, postId);

        assertTrue(cancelled);
        verify(repository).deletePendingApplication(postId, userId);
    }

    @Test
    void cancelApplicationShouldRejectNonStudentAccounts() {
        ApplicationPortalRepository repository = mock(ApplicationPortalRepository.class);
        ApplicationPortalService service = new ApplicationPortalService(repository);

        String email = "company-" + UUID.randomUUID() + "@example.com";
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();

        when(repository.findUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(repository.findPrimaryRole(userId)).thenReturn(Optional.of("COMPANY"));

        assertThrows(DomainException.class, () -> service.cancelApplication(email, postId));
    }

    @Test
    void deleteApplicationForMyCompanyPostShouldDeleteReceivedApplicationForCompany() {
        ApplicationPortalRepository repository = mock(ApplicationPortalRepository.class);
        ApplicationPortalService service = new ApplicationPortalService(repository);

        String email = "company-" + UUID.randomUUID() + "@example.com";
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        when(repository.findUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(repository.findPrimaryRole(userId)).thenReturn(Optional.of("COMPANY"));
        when(repository.deleteApplicationForCompany(applicationId, userId)).thenReturn(true);

        boolean deleted = service.deleteApplicationForMyCompanyPost(email, applicationId);

        assertTrue(deleted);
        verify(repository).deleteApplicationForCompany(applicationId, userId);
    }

    @Test
    void deleteApplicationForMyCompanyPostShouldRejectNonCompanyAccounts() {
        ApplicationPortalRepository repository = mock(ApplicationPortalRepository.class);
        ApplicationPortalService service = new ApplicationPortalService(repository);

        String email = "student-" + UUID.randomUUID() + "@example.com";
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        when(repository.findUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(repository.findPrimaryRole(userId)).thenReturn(Optional.of("STUDENT"));

        assertThrows(DomainException.class, () -> service.deleteApplicationForMyCompanyPost(email, applicationId));
    }
}
