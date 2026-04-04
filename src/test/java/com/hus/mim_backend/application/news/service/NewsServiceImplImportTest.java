package com.hus.mim_backend.application.news.service;

import com.hus.mim_backend.application.news.dto.NewsScheduleImportPreviewResponse;
import com.hus.mim_backend.application.port.output.NewsRepository;
import com.hus.mim_backend.application.research.usecase.ManageResearchPortalUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NewsServiceImplImportTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void importsScheduleEvenWhenApprovedPapersComeBackAsCachedMaps() {
        NewsRepository newsRepository = mock(NewsRepository.class);
        ManageResearchPortalUseCase researchPortalUseCase = mock(ManageResearchPortalUseCase.class);
        when(researchPortalUseCase.getAllApprovedPapers()).thenReturn((List) List.of(
                Map.of(
                        "id", UUID.randomUUID().toString(),
                        "title", "Đề tài A"
                )
        ));

        NewsServiceImpl service = new NewsServiceImpl(newsRepository, researchPortalUseCase);
        String csv = String.join("\n",
                "Tên đề tài bằng tiếng Việt,Thời gian báo cáo,Phòng báo cáo",
                "Đề tài A,08:00 - 08:20,Phòng A101",
                "Đề tài B,08:20 - 08:40,Phòng A101");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "research-report-schedule-template.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        NewsScheduleImportPreviewResponse preview = service.importResearchSchedule(file, null);

        assertNotNull(preview);
        assertEquals(2, preview.getTotalEntries());
        assertEquals(1, preview.getMatchedEntries());
        assertFalse(preview.getEntries().isEmpty());
    }
}
