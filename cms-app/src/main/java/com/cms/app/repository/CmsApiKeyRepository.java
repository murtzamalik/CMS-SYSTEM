package com.cms.app.repository;

import com.cms.app.entity.CmsApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CmsApiKeyRepository extends JpaRepository<CmsApiKey, Long> {

    Optional<CmsApiKey> findByKeyHashAndStatus(String keyHash, String status);

    boolean existsByStatus(String status);
}
