package com.hus.mim_backend.application.research.usecase;

import com.hus.mim_backend.application.research.dto.PaperResponse;
import com.hus.mim_backend.application.shared.PagedResult;

import java.util.List;

public interface QueryPublicResearchPapersPageUseCase {
    PagedResult<PaperResponse> getPapersPage(String keyword,
                                             String category,
                                             String paperType,
                                             List<String> researchAreas,
                                             Integer publicationYear,
                                             String metricSort,
                                             int page,
                                             int size);
}
