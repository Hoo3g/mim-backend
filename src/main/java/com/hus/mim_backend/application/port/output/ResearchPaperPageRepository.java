package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.research.dto.PaperResponse;
import com.hus.mim_backend.application.shared.PagedResult;

import java.util.List;

/**
 * Output port for paged public research paper queries.
 */
public interface ResearchPaperPageRepository {
    PagedResult<PaperResponse> findApprovedPapersPage(String normalizedKeyword,
                                                      String normalizedCategory,
                                                      String normalizedPaperType,
                                                      List<String> normalizedResearchAreas,
                                                      Integer publicationYear,
                                                      String metricSort,
                                                      int page,
                                                      int size);
}
