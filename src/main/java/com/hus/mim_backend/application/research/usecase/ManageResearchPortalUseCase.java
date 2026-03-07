package com.hus.mim_backend.application.research.usecase;

import com.hus.mim_backend.application.research.dto.PaperResponse;
import com.hus.mim_backend.application.research.dto.UpsertPaperRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for research portal APIs.
 */
public interface ManageResearchPortalUseCase {
    List<PaperResponse> getAllApprovedPapers();

    List<PaperResponse> getMyPapers(String currentUserEmail);

    Optional<PaperResponse> getApprovedPaperById(UUID paperId);

    PaperResponse createPaper(String currentUserEmail, UpsertPaperRequest request);

    UpdatePaperResult updatePaper(String currentUserEmail, UUID paperId, UpsertPaperRequest request);

    enum UpdatePaperResultType {
        SUCCESS,
        FORBIDDEN,
        NOT_FOUND
    }

    class UpdatePaperResult {
        private final UpdatePaperResultType type;
        private final PaperResponse paper;

        private UpdatePaperResult(UpdatePaperResultType type, PaperResponse paper) {
            this.type = type;
            this.paper = paper;
        }

        public static UpdatePaperResult success(PaperResponse paper) {
            return new UpdatePaperResult(UpdatePaperResultType.SUCCESS, paper);
        }

        public static UpdatePaperResult forbidden() {
            return new UpdatePaperResult(UpdatePaperResultType.FORBIDDEN, null);
        }

        public static UpdatePaperResult notFound() {
            return new UpdatePaperResult(UpdatePaperResultType.NOT_FOUND, null);
        }

        public UpdatePaperResultType getType() {
            return type;
        }

        public PaperResponse getPaper() {
            return paper;
        }
    }
}
