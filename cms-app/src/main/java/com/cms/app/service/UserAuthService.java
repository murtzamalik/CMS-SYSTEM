package com.cms.app.service;

import com.cms.app.entity.AppUser;
import com.cms.app.repository.AppUserRepository;
import com.cms.app.response.LoginResponse;
import com.cms.app.security.AppJwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class UserAuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppJwtService jwtService;

    public UserAuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            AppJwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Optional<LoginResponse> login(String username, String password, String relationshipNum) {
        if (username == null || username.isBlank() || password == null) {
            return Optional.empty();
        }
        Optional<AppUser> opt = appUserRepository.findByLoginIdIgnoreCase(username.trim());
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        AppUser user = opt.get();
        if (user.getWhenDeleted() != null) {
            return Optional.empty();
        }
        if (user.getIsActive() == null || user.getIsActive().compareTo(BigDecimal.ONE) != 0) {
            return Optional.empty();
        }
        String stored = user.getPassword();
        if (stored == null || stored.isBlank() || !passwordEncoder.matches(password, stored)) {
            return Optional.empty();
        }

        List<String> groupIds = appUserRepository.findGroupIdsByLoginId(user.getLoginId());
        String boundRelationship = relationshipNum != null && !relationshipNum.isBlank()
                ? relationshipNum.trim()
                : null;
        String token = jwtService.generateToken(user.getLoginId(), groupIds, boundRelationship);

        LoginResponse body = new LoginResponse();
        body.setLoginId(user.getLoginId());
        body.setFullName(user.getFullName() != null ? user.getFullName() : user.getLoginId());
        body.setGroupIds(groupIds);
        body.setToken(token);
        body.setRelationshipNum(boundRelationship);
        return Optional.of(body);
    }
}
