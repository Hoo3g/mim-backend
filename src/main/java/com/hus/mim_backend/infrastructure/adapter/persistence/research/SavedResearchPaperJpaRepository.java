package com.hus.mim_backend.infrastructure.adapter.persistence.research;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SavedResearchPaperJpaRepository extends JpaRepository<SavedResearchPaperEntity, SavedResearchPaperId> {
    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM research_papers
                WHERE id = :paperId
                  AND COALESCE(approval_status, 'PENDING') = 'APPROVED'
            )
            """, nativeQuery = true)
    boolean existsApprovedPaper(@Param("paperId") UUID paperId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO saved_research_papers (user_id, paper_id, created_at)
            VALUES (:userId, :paperId, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id, paper_id) DO NOTHING
            """, nativeQuery = true)
    void insertIgnoreConflict(@Param("userId") UUID userId, @Param("paperId") UUID paperId);

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM saved_research_papers
            WHERE user_id = :userId
              AND paper_id = :paperId
            """, nativeQuery = true)
    int deleteByUserIdAndPaperId(@Param("userId") UUID userId, @Param("paperId") UUID paperId);

    @Query(value = """
            SELECT sr.paper_id AS paperId,
                   rp.title AS title,
                   COALESCE(rp.research_area, 'Chưa phân loại') AS researchArea,
                   COALESCE(rp.category, 'STUDENT') AS category,
                   rp.publication_year AS publicationYear,
                   sr.created_at AS savedAt
            FROM saved_research_papers sr
            JOIN research_papers rp ON rp.id = sr.paper_id
            WHERE sr.user_id = :userId
              AND COALESCE(rp.approval_status, 'PENDING') = 'APPROVED'
            ORDER BY sr.created_at DESC
            """, nativeQuery = true)
    List<ResearchBookmarkProjection> findBookmarksByUserId(@Param("userId") UUID userId);
}
