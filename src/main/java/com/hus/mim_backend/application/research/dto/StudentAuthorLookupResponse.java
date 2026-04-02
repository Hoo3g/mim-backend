package com.hus.mim_backend.application.research.dto;

import java.util.UUID;

/**
 * Lightweight student lookup item for research co-author search.
 */
public record StudentAuthorLookupResponse(
        UUID userId,
        String studentId,
        String fullName
) {
}
