package com.cms.dal.repository;

import com.cms.dal.entity.CmsApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CmsApiKeyRepository extends JpaRepository<CmsApiKey, Long> {

    List<CmsApiKey> findAllByOrderByCreatedAtDesc();

    Optional<CmsApiKey> findFirstByStatusOrderByCreatedAtDesc(String status);

    Optional<CmsApiKey> findByKeyHashAndStatus(String keyHash, String status);

    @Modifying
    @Query("UPDATE CmsApiKey k SET k.status = 'REVOKED', k.revokedBy = :revokedBy, k.revokedAt = :revokedAt WHERE k.status = 'ACTIVE'")
    int revokeAllActive(@Param("revokedBy") String revokedBy, @Param("revokedAt") LocalDateTime revokedAt);
}
