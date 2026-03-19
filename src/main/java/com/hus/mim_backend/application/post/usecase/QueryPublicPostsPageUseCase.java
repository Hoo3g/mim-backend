package com.hus.mim_backend.application.post.usecase;

import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.shared.PagedResult;

import java.util.List;

public interface QueryPublicPostsPageUseCase {
    PagedResult<PublicPostResponse> getPostsPage(String keyword,
                                                 String type,
                                                 List<String> specializations,
                                                 int page,
                                                 int size);
}
