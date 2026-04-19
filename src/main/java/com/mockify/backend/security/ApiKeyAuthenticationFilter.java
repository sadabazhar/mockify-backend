package com.mockify.backend.security;

import com.mockify.backend.config.ApiKeyConfig;
import com.mockify.backend.model.ApiKey;
import com.mockify.backend.model.ApiKeyPermission;
import com.mockify.backend.repository.ApiKeyPermissionRepository;
import com.mockify.backend.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Filter to authenticate requests using API keys.
 *
 * <p>Runs after the JWT filter. If a JWT is already present the filter skips
 * immediately, so JWT always takes precedence.</p>
 *
 * <p>On a successful HMAC match the filter eagerly loads all
 * {@link ApiKeyPermission} rows for the key and stores them inside the
 * {@link ApiKeyAuthenticationToken}.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyPermissionRepository apiKeyPermissionRepository;
    private final ApiKeyCryptoService cryptoService;
    private final ApiKeyConfig apiKeyConfig;

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String API_KEY_PREFIX = "ApiKey ";

    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "/api/auth/register",
            "/api/auth/register/verify",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/forgot-password",
            "/api/auth/reset-password"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        // Respect the global kill switch
        if (!apiKeyConfig.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip if already authenticated (JWT took precedence)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract API key from headers
        String apiKey = extractApiKey(request);

        if (!StringUtils.hasText(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Validate key format
            if (!cryptoService.isValidKeyFormat(apiKey)) {
                log.warn("Invalid API key format from IP: {}", request.getRemoteAddr());
                filterChain.doFilter(request, response);
                return;
            }

            Optional<ApiKey> validatedKey = authenticateByKeyHash(apiKey);

            if (validatedKey.isPresent()) {
                ApiKey key = validatedKey.get();

                if (!key.isValid()) {
                    log.warn("API key failed validity check: keyId={}", key.getId());
                    filterChain.doFilter(request, response);
                    return;
                }

                // Load permissions eagerly so MockifyPermissionEvaluator can use them
                // without extra DB calls during @PreAuthorize evaluation.
                List<ApiKeyPermission> permissions =
                        apiKeyPermissionRepository.findByApiKeyId(key.getId());

                ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(
                        key.getId(),
                        key.getCreatedBy().getId(),
                        key.getOrganization().getId(),
                        key.getProject() != null ? key.getProject().getId() : null,
                        permissions,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_KEY")),
                        key.getRateLimitPerMinute()
                );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Update last used timestamp asynchronously (don't block request)
                updateLastUsedAsync(key);

                log.debug("API key authenticated: keyId={}, org={}, permissions={}",
                        key.getId(), key.getOrganization().getId(), permissions.size());

            } else {
                log.warn("API key authentication failed from IP: {}", request.getRemoteAddr());
            }

        } catch (Exception ex) {
            log.error("API key authentication error", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract API key from request headers
     * Supports both X-API-Key header and Authorization: ApiKey <key> format
     */
    private String extractApiKey(HttpServletRequest request) {
        // Try X-API-Key header first
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (StringUtils.hasText(apiKey)) {
            return apiKey.trim();
        }

        // Try Authorization header with "ApiKey" scheme
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(API_KEY_PREFIX)) {
            return authHeader.substring(API_KEY_PREFIX.length()).trim();
        }

        return null;
    }

    private Optional<ApiKey> authenticateByKeyHash(String apiKey) {
        try {
            String keyPrefix = cryptoService.extractKeyPrefix(apiKey);
            List<ApiKey> candidates = apiKeyRepository.findByKeyPrefixAndActive(keyPrefix, LocalDateTime.now());

            if (candidates.isEmpty()) {
                log.debug("No active API keys found with prefix: {}", keyPrefix);
                return Optional.empty();
            }

            for (ApiKey candidate : candidates) {
                String orgSecret = cryptoService.generateOrgSecret(
                        candidate.getOrganization().getId().toString(),
                        apiKeyConfig.getSecret()
                );
                if (cryptoService.verifyApiKey(apiKey, candidate.getKeyHash(), orgSecret)) {
                    return Optional.of(candidate);
                }
            }

            log.debug("No HMAC match found among {} candidate key(s)", candidates.size());
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error during key hash authentication", e);
            return Optional.empty();
        }
    }

    /**
     * TODO: In production, use @Async or a message queue.
     * For now, simple synchronous update — acceptable for low traffic.
     */
    private void updateLastUsedAsync(ApiKey key) {
        try {
            key.markAsUsed();
            apiKeyRepository.save(key);
        } catch (Exception e) {
            log.warn("Failed to update API key last-used timestamp: keyId={}", key.getId(), e);
        }
    }

    /**
     * Skip API key authentication for:
     *  - Public auth endpoints
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }
}