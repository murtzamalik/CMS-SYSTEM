package com.cms.app.repository;

import com.cms.app.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByLoginIdIgnoreCase(String loginId);

    @Query(value = "SELECT GROUP_ID FROM USM_USER_GROUP WHERE UPPER(LOGIN_ID) = UPPER(:loginId)", nativeQuery = true)
    List<String> findGroupIdsByLoginId(@Param("loginId") String loginId);
}
