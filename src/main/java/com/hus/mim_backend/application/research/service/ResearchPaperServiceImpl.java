package com.hus.mim_backend.application.research.service;

import com.hus.mim_backend.application.port.output.ResearchPaperRepository;
import com.hus.mim_backend.application.research.dto.CreatePaperRequest;
import com.hus.mim_backend.application.research.dto.PaperResponse;
import com.hus.mim_backend.application.research.usecase.ManageResearchPaperUseCase;
import com.hus.mim_backend.domain.research.model.PaperAuthor;
import com.hus.mim_backend.domain.research.model.ResearchPaper;
import com.hus.mim_backend.domain.shared.DomainException;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service orchestrating Research Paper use cases
 */
public class ResearchPaperServiceImpl implements ManageResearchPaperUseCase {

    private final ResearchPaperRepository paperRepository;

    public ResearchPaperServiceImpl(ResearchPaperRepository paperRepository) {
        this.paperRepository = paperRepository;
    }

    @Override
    public PaperResponse uploadPaper(CreatePaperRequest request) {
        if (request == null) {
            throw new DomainException("request is required");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new DomainException("title is required");
        }
        if (request.getAbstractText() == null || request.getAbstractText().isBlank()) {
            throw new DomainException("abstractText is required");
        }
        if (request.getPdfUrl() == null || request.getPdfUrl().isBlank()) {
            throw new DomainException("pdfUrl is required");
        }

        List<PaperAuthor> authors = buildAuthors(request.getStudentAuthorIds(), request.getLecturerAuthorIds());
        if (authors.isEmpty()) {
            throw new DomainException("At least one author is required");
        }

        ResearchPaper paper = ResearchPaper.builder()
                .id(UUID.randomUUID())
                .title(request.getTitle().trim())
                .abstractText(request.getAbstractText().trim())
                .pdfUrl(request.getPdfUrl().trim())
                .publicationYear(request.getPublicationYear() == null ? Year.now().getValue() : request.getPublicationYear())
                .category(resolveCategory(authors))
                .authors(authors)
                .viewCount(0)
                .downloadCount(0)
                .citationCount(0)
                .approvalStatus(ResearchPaper.ApprovalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ResearchPaper saved = paperRepository.save(paper);
        return toResponse(saved);
    }

    @Override
    public PaperResponse getPaper(UUID paperId) {
        if (paperId == null) {
            throw new DomainException("paperId is required");
        }
        ResearchPaper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new DomainException("Paper not found"));
        return toResponse(paper);
    }

    @Override
    public List<PaperResponse> searchPapers(String keyword) {
        List<ResearchPaper> papers;
        if (keyword == null || keyword.isBlank()) {
            papers = paperRepository.findByApprovalStatus(ResearchPaper.ApprovalStatus.APPROVED.name());
        } else {
            papers = paperRepository.searchByTitle(keyword.trim());
        }
        return papers.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void trackView(UUID paperId) {
        if (paperId == null) {
            throw new DomainException("paperId is required");
        }
        ensurePaperExists(paperId);
        paperRepository.incrementViewCount(paperId);
    }

    @Override
    public void trackDownload(UUID paperId) {
        if (paperId == null) {
            throw new DomainException("paperId is required");
        }
        ensurePaperExists(paperId);
        paperRepository.incrementDownloadCount(paperId);
    }

    private void ensurePaperExists(UUID paperId) {
        if (paperRepository.findById(paperId).isEmpty()) {
            throw new DomainException("Paper not found");
        }
    }

    private List<PaperAuthor> buildAuthors(List<UUID> studentAuthorIds, List<UUID> lecturerAuthorIds) {
        List<PaperAuthor> authors = new ArrayList<>();
        int order = 1;

        for (UUID studentId : uniqueIds(studentAuthorIds)) {
            authors.add(PaperAuthor.builder()
                    .id(UUID.randomUUID())
                    .studentId(studentId)
                    .isMainAuthor(order == 1)
                    .authorOrder(order++)
                    .build());
        }

        for (UUID lecturerId : uniqueIds(lecturerAuthorIds)) {
            authors.add(PaperAuthor.builder()
                    .id(UUID.randomUUID())
                    .lecturerId(lecturerId)
                    .isMainAuthor(order == 1)
                    .authorOrder(order++)
                    .build());
        }

        return authors;
    }

    private List<UUID> uniqueIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<UUID> unique = ids.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return unique.isEmpty() ? List.of() : new ArrayList<>(unique);
    }

    private ResearchPaper.PaperCategory resolveCategory(List<PaperAuthor> authors) {
        boolean hasStudentAuthor = authors.stream().anyMatch(author -> author.getStudentId() != null);
        boolean hasLecturerAuthor = authors.stream().anyMatch(author -> author.getLecturerId() != null);
        if (hasLecturerAuthor && !hasStudentAuthor) {
            return ResearchPaper.PaperCategory.LECTURER;
        }
        return ResearchPaper.PaperCategory.STUDENT;
    }

    private PaperResponse toResponse(ResearchPaper paper) {
        PaperResponse response = new PaperResponse();
        response.setId(paper.getId());
        response.setTitle(paper.getTitle());
        response.setAbstract(paper.getAbstractText());
        response.setPdfUrl(paper.getPdfUrl());
        response.setPublicationYear(paper.getPublicationYear());
        response.setJournalConference(paper.getJournalConference());
        response.setResearchArea(paper.getResearchArea());
        response.setCategory(paper.getCategory() == null ? null : paper.getCategory().name());
        response.setViewCount(paper.getViewCount());
        response.setDownloadCount(paper.getDownloadCount());
        response.setBookmarkCount(0);
        response.setApprovalStatus(paper.getApprovalStatus() == null ? null : paper.getApprovalStatus().name());
        response.setModerationComment(paper.getModerationComment());
        response.setCreatedAt(paper.getCreatedAt());
        response.setUpdatedAt(paper.getUpdatedAt());
        response.setAuthors(mapAuthors(paper.getAuthors()));
        return response;
    }

    private List<PaperResponse.PaperAuthorResponse> mapAuthors(List<PaperAuthor> authors) {
        if (authors == null || authors.isEmpty()) {
            return List.of();
        }

        return authors.stream()
                .map((author) -> {
                    PaperResponse.PaperAuthorResponse dto = new PaperResponse.PaperAuthorResponse();
                    UUID authorId = author.getAuthorId();
                    dto.setStudentId(authorId == null ? null : authorId.toString());
                    dto.setName(null);
                    dto.setMainAuthor(author.isMainAuthor());
                    dto.setAuthorOrder(author.getAuthorOrder());
                    return dto;
                })
                .toList();
    }
}
