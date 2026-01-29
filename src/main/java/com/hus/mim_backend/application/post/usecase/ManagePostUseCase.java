package com.hus.mim_backend.application.post.usecase;

import com.hus.mim_backend.application.post.dto.CreatePostRequest;
import com.hus.mim_backend.application.post.dto.PostResponse;
import com.hus.mim_backend.application.post.dto.UpdatePostRequest;
import java.util.List;
import java.util.UUID;

/**
 * Input port for managing job/recruitment posts
 */
public interface ManagePostUseCase {
    PostResponse createPost(UUID authorId, CreatePostRequest request);

    PostResponse updatePost(UUID postId, UpdatePostRequest request);

    void deletePost(UUID postId);

    PostResponse getPost(UUID postId);

    List<PostResponse> searchPosts(String keyword, List<String> tags);

    List<PostResponse> getMyPosts(UUID authorId);
}
