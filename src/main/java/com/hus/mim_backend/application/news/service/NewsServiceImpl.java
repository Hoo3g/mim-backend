package com.hus.mim_backend.application.news.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hus.mim_backend.application.news.dto.CreateNewsRequest;
import com.hus.mim_backend.application.news.dto.NewsResponse;
import com.hus.mim_backend.application.news.dto.NewsScheduleEntryDto;
import com.hus.mim_backend.application.news.dto.NewsScheduleImportPreviewResponse;
import com.hus.mim_backend.application.news.dto.UpdateNewsRequest;
import com.hus.mim_backend.application.news.usecase.ManageNewsUseCase;
import com.hus.mim_backend.application.port.output.NewsRepository;
import com.hus.mim_backend.application.research.dto.PaperResponse;
import com.hus.mim_backend.application.research.usecase.ManageResearchPortalUseCase;
import com.hus.mim_backend.domain.news.model.News;
import com.hus.mim_backend.domain.news.model.NewsScheduleEntry;
import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.shared.constants.CacheNames;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service orchestrating News management use cases.
 */
public class NewsServiceImpl implements ManageNewsUseCase {
    private static final int MAX_TITLE_LENGTH = 512;
    private static final int DEFAULT_SUMMARY_LENGTH = 220;
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String CONTENT_TYPE_STANDARD = "STANDARD";
    private static final String CONTENT_TYPE_RESEARCH_SCHEDULE = "RESEARCH_SCHEDULE";
    private static final String UNKNOWN_REPORT_TIME = "Chưa cập nhật thời gian báo cáo";
    private static final String UNKNOWN_REPORT_ROOM = "Chưa cập nhật phòng báo cáo";
    private static final String UNKNOWN_REPORT_FORMAT = "Chưa cập nhật hình thức báo cáo";
    private static final Pattern GOOGLE_SHEET_ID_PATTERN = Pattern.compile("/spreadsheets/d/([^/]+)");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Set<String> TIME_HEADER_KEYS = Set.of(
            "thoigianbaocao", "thoigian", "khunggio", "giobaocao", "thoigiantrinhbay",
            "thoigianseminar", "time", "reporttime");
    private static final Set<String> ROOM_HEADER_KEYS = Set.of(
            "phongbaocao", "phong", "sophong", "maphong", "diadiem", "phonghop",
            "phongtrinhbay", "room", "location");
    private static final Set<String> REPORT_FORMAT_HEADER_KEYS = Set.of(
            "hinhthucbaocao", "dangkyhinhthucbaocao", "dangkyhinhthuc", "hinhthuc",
            "hinhthuctrinhbay", "tieuban", "tieu ban", "presentationformat", "reportformat")
            .stream()
            .map((value) -> value.toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    private static final Set<String> TITLE_HEADER_KEYS = Set.of(
            "tendetai", "tenbai", "tenbainghiencuu", "tenbaocao", "tenbaitrinhbay",
            "tendetaibangtiengviet", "tendetatibangtiengviet", "tendetaitiengviet",
            "tendetaibangtienganh", "tendetatibangtienganh", "tendetaitienganh",
            "deTai", "topic", "title", "seminar", "tenseminar").stream()
            .map((value) -> value.toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private final NewsRepository newsRepository;
    private final ManageResearchPortalUseCase manageResearchPortalUseCase;
    private final HttpClient httpClient;

    public NewsServiceImpl(NewsRepository newsRepository, ManageResearchPortalUseCase manageResearchPortalUseCase) {
        this.newsRepository = newsRepository;
        this.manageResearchPortalUseCase = manageResearchPortalUseCase;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLIC_NEWS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_NEWS_DETAILS, allEntries = true)
    })
    public NewsResponse createNews(UUID authorId, CreateNewsRequest request) {
        if (request == null) {
            throw new DomainException("Request body is required");
        }

        String contentType = normalizeContentType(request.getContentType(), CONTENT_TYPE_STANDARD);
        List<NewsScheduleEntry> scheduleEntries = normalizeScheduleEntries(
                request.getScheduleEntries(),
                contentType,
                List.of());

        News news = new News();
        news.setId(UUID.randomUUID());
        news.setTitle(normalizeTitle(request.getTitle()));
        news.setContentType(contentType);
        news.setContent(normalizeContent(request.getContent(), contentType, scheduleEntries));
        news.setSummary(normalizeSummary(request.getSummary(), news.getContent(), contentType, scheduleEntries));
        news.setImageUrl(normalizeOptionalText(request.getImageUrl()));
        news.setStatus(normalizeStatus(request.getStatus(), STATUS_PUBLISHED));
        news.setPinned(Boolean.TRUE.equals(request.getPinned()));
        news.setAuthorId(authorId);
        news.setImportSourceUrl(contentType.equals(CONTENT_TYPE_RESEARCH_SCHEDULE)
                ? normalizeOptionalText(request.getImportSourceUrl())
                : null);
        news.setScheduleEntries(scheduleEntries);
        news.setImportedAt(scheduleEntries.isEmpty() ? null : LocalDateTime.now());
        news.setCreatedAt(LocalDateTime.now());
        news.setUpdatedAt(LocalDateTime.now());

        return toResponse(newsRepository.save(news));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLIC_NEWS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_NEWS_DETAILS, allEntries = true)
    })
    public Optional<NewsResponse> updateNews(UUID newsId, UpdateNewsRequest request) {
        if (newsId == null) {
            throw new DomainException("newsId is required");
        }
        if (request == null) {
            throw new DomainException("Request body is required");
        }

        Optional<News> current = newsRepository.findById(newsId);
        if (current.isEmpty()) {
            return Optional.empty();
        }

        News news = current.get();
        String contentType = normalizeContentType(request.getContentType(), news.getContentType());
        List<NewsScheduleEntry> scheduleEntries = normalizeScheduleEntries(
                request.getScheduleEntries(),
                contentType,
                news.getScheduleEntries());

        news.setTitle(normalizeTitle(request.getTitle()));
        news.setContentType(contentType);
        news.setContent(normalizeContent(request.getContent(), contentType, scheduleEntries));
        news.setSummary(normalizeSummary(request.getSummary(), news.getContent(), contentType, scheduleEntries));
        news.setImageUrl(normalizeOptionalText(request.getImageUrl()));
        news.setStatus(normalizeStatus(request.getStatus(), news.getStatus()));
        news.setPinned(request.getPinned() == null ? news.isPinned() : request.getPinned());
        news.setImportSourceUrl(contentType.equals(CONTENT_TYPE_RESEARCH_SCHEDULE)
                ? normalizeOptionalText(request.getImportSourceUrl())
                : null);
        news.setScheduleEntries(scheduleEntries);
        news.setImportedAt(scheduleEntries.isEmpty() ? null : LocalDateTime.now());
        news.setUpdatedAt(LocalDateTime.now());

        return Optional.of(toResponse(newsRepository.save(news)));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PUBLIC_NEWS,
            key = "T(com.hus.mim_backend.shared.constants.CacheKeys).singleton()",
            sync = true)
    public List<NewsResponse> getPublicNews() {
        return newsRepository.findPublishedOrderByPinnedAndCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<NewsResponse> getAdminNews() {
        return newsRepository.findAllOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public Optional<NewsResponse> getPublicNewsDetails(UUID newsId) {
        if (newsId == null) {
            throw new DomainException("newsId is required");
        }

        return newsRepository.findById(newsId)
                .filter(news -> STATUS_PUBLISHED.equalsIgnoreCase(news.getStatus()))
                .map(this::toResponse);
    }

    @Override
    public Optional<NewsResponse> getAdminNewsDetails(UUID newsId) {
        if (newsId == null) {
            throw new DomainException("newsId is required");
        }

        return newsRepository.findById(newsId).map(this::toResponse);
    }

    @Override
    public NewsScheduleImportPreviewResponse importResearchSchedule(MultipartFile file, String sourceUrl) {
        ImportedScheduleSource source = resolveImportedScheduleSource(file, sourceUrl);
        List<ImportedScheduleRow> rows = parseImportedScheduleRows(source);
        if (rows.isEmpty()) {
            throw new DomainException("Không tìm thấy dòng lịch báo cáo hợp lệ trong nguồn đã nhập.");
        }

        Map<String, PaperResponse> approvedPaperByTitle = buildApprovedPaperIndex();
        List<NewsScheduleEntryDto> entries = new ArrayList<>();
        int matchedEntries = 0;

        for (ImportedScheduleRow row : rows) {
            NewsScheduleEntryDto entry = new NewsScheduleEntryDto();
            entry.setReportTime(row.reportTime());
            entry.setReportRoom(row.reportRoom());
            entry.setReportFormat(row.reportFormat());
            entry.setPaperTitle(row.paperTitle());
            entry.setDisplayOrder(row.displayOrder());

            PaperResponse matchedPaper = approvedPaperByTitle.get(normalizeLookupKey(row.paperTitle()));
            if (matchedPaper != null && matchedPaper.getId() != null) {
                entry.setPaperId(matchedPaper.getId());
                matchedEntries++;
            }

            entries.add(entry);
        }

        NewsScheduleImportPreviewResponse response = new NewsScheduleImportPreviewResponse();
        response.setSourceUrl(source.rawSourceUrl());
        response.setEntries(entries);
        response.setTotalEntries(entries.size());
        response.setMatchedEntries(matchedEntries);
        response.setUnmatchedEntries(Math.max(entries.size() - matchedEntries, 0));
        return response;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLIC_NEWS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLIC_NEWS_DETAILS, allEntries = true)
    })
    public boolean deleteNews(UUID newsId) {
        if (newsId == null) {
            throw new DomainException("newsId is required");
        }
        return newsRepository.deleteById(newsId) > 0;
    }

    private NewsResponse toResponse(News news) {
        NewsResponse response = new NewsResponse();
        response.setId(news.getId());
        response.setTitle(news.getTitle());
        response.setContent(news.getContent());
        response.setSummary(news.getSummary());
        response.setImageUrl(news.getImageUrl());
        response.setStatus(news.getStatus());
        response.setContentType(normalizeContentType(news.getContentType(), CONTENT_TYPE_STANDARD));
        response.setPinned(news.isPinned());
        response.setAuthorId(news.getAuthorId());
        response.setImportSourceUrl(news.getImportSourceUrl());
        response.setScheduleEntries(toScheduleEntryDtos(news.getScheduleEntries()));
        response.setImportedAt(news.getImportedAt());
        response.setCreatedAt(news.getCreatedAt());
        response.setUpdatedAt(news.getUpdatedAt());
        return response;
    }

    private List<NewsScheduleEntryDto> toScheduleEntryDtos(List<NewsScheduleEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .map((entry) -> {
                    ScheduleDisplayFields displayFields = normalizeScheduleDisplayFields(
                            entry.getReportRoom(),
                            entry.getReportFormat());
                    NewsScheduleEntryDto dto = new NewsScheduleEntryDto();
                    dto.setReportTime(entry.getReportTime());
                    dto.setReportRoom(displayFields.reportRoom());
                    dto.setReportFormat(displayFields.reportFormat());
                    dto.setPaperTitle(entry.getPaperTitle());
                    dto.setPaperId(entry.getPaperId());
                    dto.setDisplayOrder(entry.getDisplayOrder());
                    return dto;
                })
                .toList();
    }

    private String normalizeTitle(String value) {
        if (!StringUtils.hasText(value)) {
            throw new DomainException("News title is required");
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > MAX_TITLE_LENGTH) {
            throw new DomainException("News title exceeds 512 characters");
        }
        return normalized;
    }

    private String normalizeContent(String value, String contentType, List<NewsScheduleEntry> scheduleEntries) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        if (CONTENT_TYPE_RESEARCH_SCHEDULE.equals(contentType)) {
            if (scheduleEntries == null || scheduleEntries.isEmpty()) {
                throw new DomainException("Schedule entries are required for research schedule news.");
            }
            return buildScheduleFallbackContent(scheduleEntries);
        }
        throw new DomainException("News content is required");
    }

    private String normalizeSummary(String value,
            String fallbackContent,
            String contentType,
            List<NewsScheduleEntry> scheduleEntries) {
        String normalized = normalizeOptionalText(value);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        if (CONTENT_TYPE_RESEARCH_SCHEDULE.equals(contentType) && scheduleEntries != null && !scheduleEntries.isEmpty()) {
            return buildScheduleSummary(scheduleEntries);
        }
        return generateSummary(fallbackContent);
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeStatus(String value, String fallbackStatus) {
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallbackStatus;
        if (!STATUS_DRAFT.equals(normalized) && !STATUS_PUBLISHED.equals(normalized)) {
            throw new DomainException("Unsupported news status. Use DRAFT or PUBLISHED.");
        }
        return normalized;
    }

    private String normalizeContentType(String value, String fallbackContentType) {
        String fallback = StringUtils.hasText(fallbackContentType) ? fallbackContentType : CONTENT_TYPE_STANDARD;
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
        if (!CONTENT_TYPE_STANDARD.equals(normalized) && !CONTENT_TYPE_RESEARCH_SCHEDULE.equals(normalized)) {
            throw new DomainException("Unsupported news contentType. Use STANDARD or RESEARCH_SCHEDULE.");
        }
        return normalized;
    }

    private List<NewsScheduleEntry> normalizeScheduleEntries(List<NewsScheduleEntryDto> requestEntries,
            String contentType,
            List<NewsScheduleEntry> fallbackEntries) {
        if (!CONTENT_TYPE_RESEARCH_SCHEDULE.equals(contentType)) {
            return List.of();
        }

        List<NewsScheduleEntryDto> sourceEntries = requestEntries == null ? toScheduleEntryDtos(fallbackEntries) : requestEntries;
        if (sourceEntries == null || sourceEntries.isEmpty()) {
            throw new DomainException("Schedule entries are required for research schedule news.");
        }

        List<NewsScheduleEntry> normalizedEntries = new ArrayList<>();
        int displayOrder = 1;
        for (NewsScheduleEntryDto entryDto : sourceEntries) {
            if (entryDto == null) {
                continue;
            }

            String reportTime = normalizeRequiredScheduleField(entryDto.getReportTime(), "reportTime");
            ScheduleDisplayFields displayFields = normalizeScheduleDisplayFields(
                    entryDto.getReportRoom(),
                    entryDto.getReportFormat());
            String reportRoom = displayFields.reportRoom();
            String reportFormat = displayFields.reportFormat();
            String paperTitle = normalizeRequiredScheduleField(entryDto.getPaperTitle(), "paperTitle");

            NewsScheduleEntry entry = new NewsScheduleEntry();
            entry.setReportTime(reportTime);
            entry.setReportRoom(reportRoom);
            entry.setReportFormat(reportFormat);
            entry.setPaperTitle(paperTitle);
            entry.setPaperId(entryDto.getPaperId());
            entry.setDisplayOrder(entryDto.getDisplayOrder() == null ? displayOrder : entryDto.getDisplayOrder());
            normalizedEntries.add(entry);
            displayOrder++;
        }

        if (normalizedEntries.isEmpty()) {
            throw new DomainException("Schedule entries are required for research schedule news.");
        }
        return normalizedEntries;
    }

    private String normalizeRequiredScheduleField(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new DomainException(fieldName + " is required in schedule entries");
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String buildScheduleFallbackContent(List<NewsScheduleEntry> scheduleEntries) {
        return "Lịch báo cáo nghiên cứu được hiển thị theo phòng, hình thức báo cáo và thời gian trình bày. "
                + "Danh sách hiện có " + scheduleEntries.size() + " bài trình bày.";
    }

    private String buildScheduleSummary(List<NewsScheduleEntry> scheduleEntries) {
        long roomCount = scheduleEntries.stream()
                .map((entry) -> normalizeScheduleDisplayFields(entry.getReportRoom(), entry.getReportFormat()).reportRoom())
                .filter(StringUtils::hasText)
                .distinct()
                .count();
        long reportFormatCount = scheduleEntries.stream()
                .map((entry) -> normalizeScheduleDisplayFields(entry.getReportRoom(), entry.getReportFormat()).reportFormat())
                .filter(StringUtils::hasText)
                .distinct()
                .count();
        long timeCount = scheduleEntries.stream()
                .map(NewsScheduleEntry::getReportTime)
                .filter(StringUtils::hasText)
                .distinct()
                .count();
        return "Lịch báo cáo gồm " + scheduleEntries.size()
                + " bài trình bày tại " + roomCount
                + " phòng, " + reportFormatCount
                + " hình thức báo cáo theo " + timeCount + " khung thời gian.";
    }

    private String generateSummary(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }

        String plain = content
                .replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (plain.length() <= DEFAULT_SUMMARY_LENGTH) {
            return plain;
        }
        return plain.substring(0, DEFAULT_SUMMARY_LENGTH).trim() + "...";
    }

    private ImportedScheduleSource resolveImportedScheduleSource(MultipartFile file, String sourceUrl) {
        boolean hasFile = file != null && !file.isEmpty();
        boolean hasUrl = StringUtils.hasText(sourceUrl);

        if (!hasFile && !hasUrl) {
            throw new DomainException("Excel file or sourceUrl is required");
        }

        if (hasFile) {
            try {
                return new ImportedScheduleSource(
                        file.getOriginalFilename(),
                        normalizeOptionalText(file.getContentType()),
                        null,
                        file.getBytes());
            } catch (IOException ex) {
                throw new DomainException("Không thể đọc file lịch báo cáo đã tải lên.");
            }
        }

        String normalizedUrl = sourceUrl.trim();
        String downloadUrl = resolveScheduleDownloadUrl(normalizedUrl);
        HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "MIM Platform Schedule Import")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                throw new DomainException("Không thể tải dữ liệu từ URL bảng tính đã nhập.");
            }

            return new ImportedScheduleSource(
                    extractRemoteSourceName(downloadUrl),
                    response.headers().firstValue("Content-Type").orElse(null),
                    normalizedUrl,
                    response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DomainException("Tác vụ tải bảng tính bị gián đoạn.");
        } catch (IOException ex) {
            throw new DomainException("Không thể tải dữ liệu từ URL bảng tính đã nhập.");
        }
    }

    private String resolveScheduleDownloadUrl(String sourceUrl) {
        URI uri = URI.create(sourceUrl);
        String host = Optional.ofNullable(uri.getHost()).orElse("").toLowerCase(Locale.ROOT);
        if (!host.contains("docs.google.com")) {
            return sourceUrl;
        }

        Matcher matcher = GOOGLE_SHEET_ID_PATTERN.matcher(Optional.ofNullable(uri.getPath()).orElse(""));
        if (!matcher.find()) {
            return sourceUrl;
        }

        String sheetId = matcher.group(1);
        String gid = extractQueryValue(uri.getRawQuery(), "gid");
        if (!StringUtils.hasText(gid) && StringUtils.hasText(uri.getFragment())) {
            gid = extractQueryValue(uri.getFragment(), "gid");
        }
        if (!StringUtils.hasText(gid)) {
            gid = "0";
        }

        return "https://docs.google.com/spreadsheets/d/" + sheetId
                + "/export?format=csv&gid=" + URLEncoder.encode(gid, StandardCharsets.UTF_8);
    }

    private String extractQueryValue(String rawQuery, String key) {
        if (!StringUtils.hasText(rawQuery) || !StringUtils.hasText(key)) {
            return null;
        }
        for (String pair : rawQuery.split("&")) {
            int separatorIndex = pair.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }
            String currentKey = pair.substring(0, separatorIndex).trim();
            if (!key.equalsIgnoreCase(currentKey)) {
                continue;
            }
            return pair.substring(separatorIndex + 1).trim();
        }
        return null;
    }

    private String extractRemoteSourceName(String sourceUrl) {
        try {
            String path = Optional.ofNullable(URI.create(sourceUrl).getPath()).orElse("");
            int slashIndex = path.lastIndexOf('/');
            String candidate = slashIndex >= 0 ? path.substring(slashIndex + 1) : path;
            return StringUtils.hasText(candidate) ? candidate : "research-schedule.csv";
        } catch (IllegalArgumentException ex) {
            return "research-schedule.csv";
        }
    }

    private List<ImportedScheduleRow> parseImportedScheduleRows(ImportedScheduleSource source) {
        List<List<String>> rows = looksLikeWorkbook(source) ? parseWorkbookRows(source.bytes()) : parseCsvRows(source.bytes());
        if (rows.isEmpty()) {
            return List.of();
        }

        ParsedHeader parsedHeader = findParsedHeader(rows);
        List<ImportedScheduleRow> importedRows = new ArrayList<>();
        int displayOrder = 1;
        for (int rowIndex = parsedHeader.headerRowIndex() + 1; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            String reportTime = parsedHeader.timeIndex() == null
                    ? UNKNOWN_REPORT_TIME
                    : extractColumnValue(row, parsedHeader.timeIndex());
            String reportRoom = parsedHeader.roomIndex() == null
                    ? UNKNOWN_REPORT_ROOM
                    : extractColumnValue(row, parsedHeader.roomIndex());
            String reportFormat = parsedHeader.reportFormatIndex() == null
                    ? UNKNOWN_REPORT_FORMAT
                    : extractColumnValue(row, parsedHeader.reportFormatIndex());
            String paperTitle = extractColumnValue(row, parsedHeader.titleIndex());

            if (!StringUtils.hasText(reportTime)
                    && !StringUtils.hasText(reportRoom)
                    && !StringUtils.hasText(reportFormat)
                    && !StringUtils.hasText(paperTitle)) {
                continue;
            }
            if (!StringUtils.hasText(paperTitle)) {
                continue;
            }

            ScheduleDisplayFields displayFields = normalizeScheduleDisplayFields(
                    normalizeScheduleCellValue(reportRoom, UNKNOWN_REPORT_ROOM),
                    normalizeScheduleCellValue(reportFormat, UNKNOWN_REPORT_FORMAT));

            importedRows.add(new ImportedScheduleRow(
                    normalizeScheduleCellValue(reportTime, UNKNOWN_REPORT_TIME),
                    displayFields.reportRoom(),
                    displayFields.reportFormat(),
                    paperTitle.trim().replaceAll("\\s+", " "),
                    displayOrder++));
        }
        return importedRows;
    }

    private boolean looksLikeWorkbook(ImportedScheduleSource source) {
        String sourceName = Optional.ofNullable(source.sourceName()).orElse("").toLowerCase(Locale.ROOT);
        String contentType = Optional.ofNullable(source.contentType()).orElse("").toLowerCase(Locale.ROOT);
        byte[] bytes = source.bytes();
        return sourceName.endsWith(".xlsx")
                || sourceName.endsWith(".xls")
                || contentType.contains("spreadsheet")
                || contentType.contains("excel")
                || hasZipHeader(bytes)
                || hasOle2Header(bytes);
    }

    private boolean hasZipHeader(byte[] bytes) {
        return bytes != null && bytes.length >= 4
                && bytes[0] == 'P'
                && bytes[1] == 'K'
                && bytes[2] == 3
                && bytes[3] == 4;
    }

    private boolean hasOle2Header(byte[] bytes) {
        return bytes != null && bytes.length >= 8
                && (bytes[0] & 0xFF) == 0xD0
                && (bytes[1] & 0xFF) == 0xCF
                && (bytes[2] & 0xFF) == 0x11
                && (bytes[3] & 0xFF) == 0xE0;
    }

    private List<List<String>> parseWorkbookRows(byte[] bytes) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() == 0) {
                return List.of();
            }

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("vi-VN"));
            List<List<String>> rows = new ArrayList<>();

            for (Row row : sheet) {
                int lastCellNum = Math.max(row.getLastCellNum(), (short) 0);
                List<String> values = new ArrayList<>();
                for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
                    values.add(formatter.formatCellValue(row.getCell(cellIndex)).trim());
                }
                if (values.stream().noneMatch(StringUtils::hasText)) {
                    continue;
                }
                rows.add(values);
            }
            return rows;
        } catch (IOException ex) {
            throw new DomainException("Không thể đọc file Excel đã tải lên.");
        }
    }

    private List<List<String>> parseCsvRows(byte[] bytes) {
        String raw = new String(bytes, StandardCharsets.UTF_8);
        if (!raw.isEmpty() && raw.charAt(0) == '\uFEFF') {
            raw = raw.substring(1);
        }

        List<List<String>> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder currentCell = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < raw.length(); index++) {
            char current = raw.charAt(index);
            if (inQuotes) {
                if (current == '"') {
                    boolean escapedQuote = index + 1 < raw.length() && raw.charAt(index + 1) == '"';
                    if (escapedQuote) {
                        currentCell.append('"');
                        index++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    currentCell.append(current);
                }
                continue;
            }

            if (current == '"') {
                inQuotes = true;
                continue;
            }
            if (current == ',') {
                currentRow.add(currentCell.toString().trim());
                currentCell.setLength(0);
                continue;
            }
            if (current == '\r' || current == '\n') {
                if (current == '\r' && index + 1 < raw.length() && raw.charAt(index + 1) == '\n') {
                    index++;
                }
                currentRow.add(currentCell.toString().trim());
                currentCell.setLength(0);
                if (currentRow.stream().anyMatch(StringUtils::hasText)) {
                    rows.add(new ArrayList<>(currentRow));
                }
                currentRow.clear();
                continue;
            }
            currentCell.append(current);
        }

        currentRow.add(currentCell.toString().trim());
        if (currentRow.stream().anyMatch(StringUtils::hasText)) {
            rows.add(currentRow);
        }
        return rows;
    }

    private ParsedHeader findParsedHeader(List<List<String>> rows) {
        int scanLimit = Math.min(rows.size(), 10);
        List<String> missingHeaderGroups = List.of();
        for (int rowIndex = 0; rowIndex < scanLimit; rowIndex++) {
            ParsedHeader parsedHeader = resolveHeader(rows.get(rowIndex), rowIndex);
            if (parsedHeader != null) {
                return parsedHeader;
            }
            missingHeaderGroups = detectMissingHeaderGroups(rows.get(rowIndex));
        }
        if (missingHeaderGroups.isEmpty()) {
            throw new DomainException("Không tìm thấy dòng tiêu đề hợp lệ trong file import.");
        }
        throw new DomainException("Không tìm thấy các cột: " + String.join(", ", missingHeaderGroups) + ".");
    }

    private ParsedHeader resolveHeader(List<String> row, int rowIndex) {
        Integer timeIndex = null;
        Integer roomIndex = null;
        Integer reportFormatIndex = null;
        Integer titleIndex = null;

        for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
            String normalizedHeader = normalizeHeaderKey(row.get(columnIndex));
            if (!StringUtils.hasText(normalizedHeader)) {
                continue;
            }
            if (timeIndex == null && TIME_HEADER_KEYS.contains(normalizedHeader)) {
                timeIndex = columnIndex;
            } else if (roomIndex == null && ROOM_HEADER_KEYS.contains(normalizedHeader)) {
                roomIndex = columnIndex;
            } else if (reportFormatIndex == null && REPORT_FORMAT_HEADER_KEYS.contains(normalizedHeader)) {
                reportFormatIndex = columnIndex;
            } else if (titleIndex == null && TITLE_HEADER_KEYS.contains(normalizedHeader)) {
                titleIndex = columnIndex;
            }
        }

        if (titleIndex == null) {
            return null;
        }
        return new ParsedHeader(rowIndex, timeIndex, roomIndex, reportFormatIndex, titleIndex);
    }

    private List<String> detectMissingHeaderGroups(List<String> row) {
        boolean hasTime = false;
        boolean hasRoom = false;
        boolean hasTitle = false;

        for (String cellValue : row) {
            String normalizedHeader = normalizeHeaderKey(cellValue);
            if (!StringUtils.hasText(normalizedHeader)) {
                continue;
            }
            hasTime = hasTime || TIME_HEADER_KEYS.contains(normalizedHeader);
            hasRoom = hasRoom || ROOM_HEADER_KEYS.contains(normalizedHeader);
            hasTitle = hasTitle || TITLE_HEADER_KEYS.contains(normalizedHeader);
        }

        List<String> missing = new ArrayList<>();
        if (!hasTitle) {
            missing.add("tên đề tài");
        }
        if (!hasTime) {
            missing.add("thời gian báo cáo");
        }
        if (!hasRoom) {
            missing.add("phòng báo cáo");
        }
        return missing;
    }

    private String extractColumnValue(List<String> row, int index) {
        if (index < 0 || index >= row.size()) {
            return "";
        }
        return Optional.ofNullable(row.get(index)).orElse("");
    }

    private String normalizeScheduleCellValue(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private ScheduleDisplayFields normalizeScheduleDisplayFields(String reportRoom, String reportFormat) {
        String normalizedRoom = normalizeOptionalScheduleField(reportRoom);
        String normalizedFormat = normalizeOptionalScheduleField(reportFormat);

        if (!StringUtils.hasText(normalizedFormat) && looksLikeReportFormat(normalizedRoom)) {
            normalizedFormat = normalizedRoom;
            normalizedRoom = null;
        }

        return new ScheduleDisplayFields(
                StringUtils.hasText(normalizedRoom) ? normalizedRoom : UNKNOWN_REPORT_ROOM,
                StringUtils.hasText(normalizedFormat) ? normalizedFormat : UNKNOWN_REPORT_FORMAT);
    }

    private String normalizeOptionalScheduleField(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private boolean looksLikeReportFormat(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalizedValue = normalizeLookupKey(value);
        return normalizedValue.contains("tieu ban")
                || normalizedValue.contains("poster")
                || normalizedValue.contains("bao cao tai");
    }

    private Map<String, PaperResponse> buildApprovedPaperIndex() {
        Map<String, PaperResponse> approvedPaperByTitle = new LinkedHashMap<>();
        for (Object rawPaper : manageResearchPortalUseCase.getAllApprovedPapers()) {
            PaperResponse paper = coercePaperResponse(rawPaper);
            if (paper == null || !StringUtils.hasText(paper.getTitle())) {
                continue;
            }
            approvedPaperByTitle.putIfAbsent(normalizeLookupKey(paper.getTitle()), paper);
        }
        return approvedPaperByTitle;
    }

    private PaperResponse coercePaperResponse(Object rawPaper) {
        if (rawPaper instanceof PaperResponse paperResponse) {
            return paperResponse;
        }
        if (rawPaper instanceof Map<?, ?> map) {
            return OBJECT_MAPPER.convertValue(map, PaperResponse.class);
        }
        return null;
    }

    private String normalizeHeaderKey(String value) {
        return normalizeLookupKey(value).replace(" ", "");
    }

    private String normalizeLookupKey(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }

    private record ImportedScheduleSource(String sourceName, String contentType, String rawSourceUrl, byte[] bytes) {
    }

    private record ImportedScheduleRow(String reportTime,
            String reportRoom,
            String reportFormat,
            String paperTitle,
            int displayOrder) {
    }

    private record ParsedHeader(int headerRowIndex,
            Integer timeIndex,
            Integer roomIndex,
            Integer reportFormatIndex,
            int titleIndex) {
    }

    private record ScheduleDisplayFields(String reportRoom, String reportFormat) {
    }
}
