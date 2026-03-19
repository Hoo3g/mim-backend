package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.shared.PagedResult;

import java.util.List;

/**
 * Output port for paged public recruitment posts.
 */
public interface PublicPostPageRepository {
    PagedResult<PublicPostResponse> findApprovedPostsPage(String normalizedKeyword,
                                                          String normalizedType,
                                                          List<String> specializationCandidates,
                                                          int page,
                                                          int size);
}
