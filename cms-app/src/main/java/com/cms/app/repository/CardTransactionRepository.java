package com.cms.app.repository;

import com.cms.app.entity.CardTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CardTransactionRepository extends JpaRepository<CardTransaction, Long> {

    @Query("""
            SELECT t FROM CardTransaction t
            WHERE (:relationshipNum IS NULL OR t.relationshipNum = :relationshipNum)
              AND (:pan IS NULL OR t.pan = :pan)
              AND (:channelCode IS NULL OR t.channelCode = :channelCode)
              AND t.createdOn >= :fromOn
              AND t.createdOn < :toOnExclusive
            ORDER BY t.createdOn DESC
            """)
    Page<CardTransaction> search(
            @Param("relationshipNum") String relationshipNum,
            @Param("pan") String pan,
            @Param("channelCode") String channelCode,
            @Param("fromOn") LocalDateTime fromOn,
            @Param("toOnExclusive") LocalDateTime toOnExclusive,
            Pageable pageable);

    @Query("""
            SELECT t FROM CardTransaction t
            WHERE (:relationshipNum IS NULL OR t.relationshipNum = :relationshipNum)
              AND (:pan IS NULL OR t.pan = :pan)
              AND (:channelCode IS NULL OR t.channelCode = :channelCode)
              AND t.createdOn >= :fromOn
              AND t.createdOn < :toOnExclusive
            ORDER BY t.createdOn DESC
            """)
    List<CardTransaction> searchList(
            @Param("relationshipNum") String relationshipNum,
            @Param("pan") String pan,
            @Param("channelCode") String channelCode,
            @Param("fromOn") LocalDateTime fromOn,
            @Param("toOnExclusive") LocalDateTime toOnExclusive);
}
