package com.cms.controller;

import com.cms.dto.response.ApiKeyResponse;
import com.cms.dto.response.ApiResponse;
import com.cms.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/api-keys")
@Tag(name = "API Keys", description = "Channel API keys for cms-app (mobile)")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','SUPER_ADMIN')")
    @Operation(summary = "List API keys (masked; includes revoked history)")
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(apiKeyService.listAll()));
    }

    @PostMapping("/rotate")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','SUPER_ADMIN')")
    @Operation(summary = "Generate new API key and revoke the previous active key")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> rotate() {
        String loginId = currentLoginId();
        ApiKeyResponse created = apiKeyService.rotate(loginId);
        return ResponseEntity.ok(ApiResponse.ok(
                "API key generated. Copy the secret now — it will not be shown again.",
                created));
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','SUPER_ADMIN')")
    @Operation(summary = "Revoke an API key without generating a replacement")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> revoke(@PathVariable Long id) {
        String loginId = currentLoginId();
        return ResponseEntity.ok(ApiResponse.ok("API key revoked", apiKeyService.revoke(id, loginId)));
    }

    private static String currentLoginId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName() != null ? auth.getName() : "system";
    }
}
