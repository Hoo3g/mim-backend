package com.hus.mim_backend.application.post.service;

import com.hus.mim_backend.application.port.output.PublicPostRepository;
import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.post.usecase.QueryPublicPostsUseCase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for public post query use cases.
 */
public class PublicPostQueryServiceImpl implements QueryPublicPostsUseCase {
    private final PublicPostRepository repository;

    public PublicPostQueryServiceImpl(PublicPostRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PublicPostResponse> getPosts() {
        return repository.findAllApprovedPosts();
    }

    @Override
    public Optional<PublicPostResponse> getPostById(UUID postId) {
        return repository.findApprovedPostById(postId);
    }
}
