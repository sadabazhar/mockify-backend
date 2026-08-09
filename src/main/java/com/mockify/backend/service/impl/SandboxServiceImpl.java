package com.mockify.backend.service.impl;

import com.mockify.backend.common.enums.UserRole;
import com.mockify.backend.dto.response.auth.AuthResponse;
import com.mockify.backend.dto.response.auth.AuthResult;
import com.mockify.backend.dto.response.sandbox.*;
import com.mockify.backend.exception.DuplicateResourceException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.exception.SandboxExpiredException;
import com.mockify.backend.mapper.UserMapper;
import com.mockify.backend.model.*;
import com.mockify.backend.repository.*;
import com.mockify.backend.sandbox.PendingConversion;
import com.mockify.backend.sandbox.SandboxSession;
import com.mockify.backend.sandbox.SandboxSeedDataProvider;
import com.mockify.backend.security.ApiKeyCryptoService;
import com.mockify.backend.security.CookieUtil;
import com.mockify.backend.security.JwtTokenProvider;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.MailService;
import com.mockify.backend.service.SandboxService;
import com.mockify.backend.service.SlugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SandboxServiceImpl implements SandboxService {

    // ── Repositories ──────────────────────────────────────────────────────────
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final MockSchemaRepository mockSchemaRepository;
    private final MockRecordRepository mockRecordRepository;
    private final SchemaTemplateRepository schemaTemplateRepository;

    // ── Services & utilities ──────────────────────────────────────────────────
    private final SlugService slugService;
    private final EndpointService endpointService;
    private final MailService mailService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyCryptoService cryptoService;
    private final UserMapper userMapper;
    private final SandboxSeedDataProvider seedDataProvider;

    // ── Redis ─────────────────────────────────────────────────────────────────
    private final RedisTemplate<String, Object> redisTemplate;

    // ── Config ────────────────────────────────────────────────────────────────
    @Value("${app.api-key.secret}")
    private String globalSecret;

    @Value("${app.verification.email.frontend-url}")
    private String emailVerificationBaseUrl;

    // ── Constants ─────────────────────────────────────────────────────────────
    static final String SESSION_KEY_PREFIX    = "sandbox:session:";
    static final String CONVERSION_KEY_PREFIX = "sandbox:convert:";
    static final String SANDBOX_PROVIDER      = "sandbox";
    static final String DEFAULT_ORG_NAME      = "My Sandbox";
    static final String DEFAULT_PROJECT_NAME  = "Demo API";
    static final Duration ROLLING_TTL         = Duration.ofHours(4);
    static final Duration HARD_CAP            = Duration.ofHours(24);
    static final Duration CONVERSION_TTL      = Duration.ofMinutes(15);

    // =========================================================================
    // CREATE
    // =========================================================================

    @Override
    @Transactional
    public SandboxCreationResult createSandbox(String clientIp) {
        log.info("Creating sandbox for clientIp={}", clientIp);

        // 1. Provision the workspace hierarchy
        User guest       = createGuestUser();
        Organization org = createSandboxOrganization(guest);
        Project project  = createDemoProject(org);

        // 2. Seed schemas and records
        List<SandboxSchemaInfo> schemaInfos =
                seedDefaultData(project, org.getExpiresAt());

        // 3. Generate sandbox token and hash for DB fallback
        String rawToken   = UUID.randomUUID().toString();
        String tokenHash  = cryptoService.hashApiKey(rawToken, globalSecret);

        guest.setSandboxTokenHash(tokenHash);
        guest.setSandboxCreatedAt(LocalDateTime.now());
        userRepository.save(guest);

        // 4. Store session in Redis
        SandboxSession session = SandboxSession.builder()
                .userId(guest.getId())
                .organizationId(org.getId())
                .createdAtEpochMs(System.currentTimeMillis())
                .hardExpiresAtEpochMs(System.currentTimeMillis() + HARD_CAP.toMillis())
                .build();

        storeSession(rawToken, session, HARD_CAP);

        // 5. Issue guest JWT
        String jwt = jwtTokenProvider.generateAccessToken(guest.getId(), UserRole.GUEST);

        // 6. Build response
        ResponseCookie cookie = cookieUtil.createSandboxTokenCookie(rawToken);

        String baseMockUrl = "/api/mock/" + org.getSlug() + "/" + project.getSlug();

        SandboxWorkspace workspace = SandboxWorkspace.builder()
                .orgSlug(org.getSlug())
                .projectSlug(project.getSlug())
                .schemas(schemaInfos)
                .baseMockUrl(baseMockUrl)
                .build();

        log.info("Sandbox created: userId={}, orgId={}, schemas={}",
                guest.getId(), org.getId(), schemaInfos.size());

        return SandboxCreationResult.builder()
                .accessToken(jwt)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .sandboxCookie(cookie)
                .workspace(workspace)
                .build();
    }

    // =========================================================================
    // RESUME
    // =========================================================================

    @Override
    public SandboxResumeResult resumeSandbox(String sandboxToken) {
        SandboxSession session = requireValidSession(sandboxToken);

        // Roll the TTL forward (capped at hard expiry)
        rollTtl(sandboxToken, session);

        // Issue a fresh JWT for the guest user
        String jwt = jwtTokenProvider.generateAccessToken(
                session.getUserId(), UserRole.GUEST);

        log.info("Sandbox session resumed: userId={}", session.getUserId());

        return new SandboxResumeResult(
                jwt,
                jwtTokenProvider.getAccessTokenExpiration()
        );
    }

    // =========================================================================
    // CONVERSION — initiate
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public void initiateSandboxConversion(
            String sandboxToken, String email, String password) {

        SandboxSession session = requireValidSession(sandboxToken);

        // Guard: email must be available
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "An account with this email already exists.");
        }

        // Store pending conversion (email not yet verified)
        String encodedPassword = passwordEncoder.encode(password);

        PendingConversion pending = PendingConversion.builder()
                .userId(session.getUserId())
                .email(email)
                .encodedPassword(encodedPassword)
                .sandboxToken(sandboxToken)
                .build();

        // Unique token for the verification link
        String conversionToken = UUID.randomUUID().toString();
        String redisKey = CONVERSION_KEY_PREFIX + conversionToken;

        redisTemplate.opsForValue().set(redisKey, pending, CONVERSION_TTL);

        // Send verification email using the existing mail service
        String verifyLink = emailVerificationBaseUrl
                + "/sandbox/verify?token=" + conversionToken;

        mailService.sendEmailVerificationMail(email, verifyLink);

        log.info("Sandbox conversion initiated: userId={}, email={}",
                session.getUserId(), email);
    }

    // =========================================================================
    // CONVERSION — complete
    // =========================================================================

    @Override
    @Transactional
    public AuthResult completeSandboxConversion(String verificationToken) {
        // 1. Retrieve and consume the pending conversion
        String redisKey = CONVERSION_KEY_PREFIX + verificationToken;
        Object raw = redisTemplate.opsForValue().get(redisKey);

        if (raw == null) {
            throw new ResourceNotFoundException(
                    "Conversion link is invalid or has expired. " +
                            "Please request a new conversion link.");
        }

        PendingConversion pending = (PendingConversion) raw;

        // Consume immediately — single-use token
        redisTemplate.delete(redisKey);

        // 2. Load the guest user and their sandbox org
        User guest = userRepository.findById(pending.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Guest user not found. The sandbox may have already expired."));

        Organization org = organizationRepository
                .findByOwnerIdAndIsSandboxTrue(guest.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sandbox organization not found."));

        // 3. Promote guest user to real account (atomic with org promotion)
        guest.setEmail(pending.getEmail());
        guest.setPassword(pending.getEncodedPassword());
        guest.setRole(UserRole.USER);
        guest.setEmailVerified(true);
        guest.setProviderName("local");
        guest.setSandboxTokenHash(null);
        guest.setSandboxCreatedAt(null);
        userRepository.save(guest);

        // 4. Promote sandbox org to permanent org
        org.setSandbox(false);
        org.setExpiresAt(null);
        organizationRepository.save(org);

        // 5. Invalidate sandbox session in Redis
        redisTemplate.delete(SESSION_KEY_PREFIX + pending.getSandboxToken());

        log.info("Sandbox conversion completed: userId={}, email={}",
                guest.getId(), pending.getEmail());

        // 6. Build and return a full auth session
        return buildAuthResult(guest);
    }

    // =========================================================================
    // SESSION HELPERS (used by SandboxRequestInterceptor)
    // =========================================================================

    @Override
    public Optional<SandboxSession> getSession(String sandboxToken) {
        if (sandboxToken == null || sandboxToken.isBlank()) {
            return Optional.empty();
        }
        try {
            Object raw = redisTemplate.opsForValue()
                    .get(SESSION_KEY_PREFIX + sandboxToken);
            return Optional.ofNullable((SandboxSession) raw);
        } catch (Exception e) {
            log.warn("Redis error reading sandbox session, attempting DB fallback", e);
            return getSessionFromDb(sandboxToken);
        }
    }

    @Override
    public void rollTtl(String sandboxToken, SandboxSession session) {
        long now         = System.currentTimeMillis();
        long hardExpiry  = session.getHardExpiresAtEpochMs();
        long remaining   = hardExpiry - now;

        if (remaining <= 0) {
            // Already past hard cap — do not roll
            return;
        }

        // Roll forward by ROLLING_TTL but never past hard cap
        Duration newTtl = Duration.ofMillis(
                Math.min(ROLLING_TTL.toMillis(), remaining));

        redisTemplate.expire(SESSION_KEY_PREFIX + sandboxToken, newTtl);
    }

    // =========================================================================
    // Private helpers — workspace provisioning
    // =========================================================================

    private User createGuestUser() {
        User guest = new User();
        guest.setName("Guest User");
        guest.setEmail(null);
        guest.setRole(UserRole.GUEST);
        guest.setEmailVerified(false);
        guest.setProviderName(SANDBOX_PROVIDER);
        // Generate a unique username so the users_username_unique constraint holds
        guest.setUsername("sandbox_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        return userRepository.save(guest);
    }

    private Organization createSandboxOrganization(User owner) {
        String baseSlug = slugService.generateSlug(DEFAULT_ORG_NAME);
        String slug = organizationRepository.existsBySlug(baseSlug)
                ? slugService.generateUniqueSlug(baseSlug)
                : baseSlug;

        Organization org = new Organization();
        org.setName(DEFAULT_ORG_NAME);
        org.setSlug(slug);
        org.setOwner(owner);
        org.setSandbox(true);
        org.setExpiresAt(LocalDateTime.now().plus(HARD_CAP));

        Organization saved = organizationRepository.save(org);
        endpointService.createEndpoint(saved);
        return saved;
    }

    private Project createDemoProject(Organization org) {
        String slug = slugService.generateSlug(DEFAULT_PROJECT_NAME);

        Project project = new Project();
        project.setName(DEFAULT_PROJECT_NAME);
        project.setSlug(slug);
        project.setOrganization(org);

        Project saved = projectRepository.save(project);
        endpointService.createEndpoint(saved);
        return saved;
    }

    private List<SandboxSchemaInfo> seedDefaultData(
            Project project, LocalDateTime expiresAt) {

        List<SandboxSchemaInfo> result = new ArrayList<>();

        for (String templateSlug : SandboxSeedDataProvider.DEFAULT_TEMPLATE_SLUGS) {
            try {
                Optional<SchemaTemplate> templateOpt =
                        schemaTemplateRepository.findBySlugAndSystemTemplateTrue(templateSlug);

                if (templateOpt.isEmpty()) {
                    log.warn("System template '{}' not found in DB — skipping seed", templateSlug);
                    continue;
                }

                SchemaTemplate template = templateOpt.get();
                MockSchema schema = createSchemaFromTemplate(template, project);
                int seeded = seedRecords(schema, templateSlug, expiresAt);

                result.add(SandboxSchemaInfo.builder()
                        .schemaId(schema.getId())
                        .name(schema.getName())
                        .slug(schema.getSlug())
                        .apiUrl("/api/mock/" + project.getOrganization().getSlug()
                                + "/" + project.getSlug() + "/" + schema.getSlug() + "/records")
                        .seedRecordCount(seeded)
                        .build());

            } catch (Exception e) {
                // Seeding is best-effort. Log and continue.
                // A partially-seeded sandbox is better than a failed creation.
                log.error("Failed to seed template '{}': {}", templateSlug, e.getMessage(), e);
            }
        }

        return result;
    }

    private MockSchema createSchemaFromTemplate(SchemaTemplate template, Project project) {
        String baseSlug = slugService.generateSlug(template.getName());
        String slug = mockSchemaRepository.existsBySlugAndProjectId(baseSlug, project.getId())
                ? slugService.generateUniqueSlug(baseSlug)
                : baseSlug;

        MockSchema schema = new MockSchema();
        schema.setName(template.getName());
        schema.setSlug(slug);
        schema.setSchemaJson(new HashMap<>(template.getSchemaJson()));
        schema.setProject(project);

        MockSchema saved = mockSchemaRepository.save(schema);
        endpointService.createEndpoint(saved);
        return saved;
    }

    private int seedRecords(MockSchema schema, String templateSlug, LocalDateTime expiresAt) {
        List<Map<String, Object>> seedData = seedDataProvider.getSeedRecords(templateSlug);
        int count = 0;

        for (Map<String, Object> data : seedData) {
            MockRecord record = new MockRecord();
            record.setMockSchema(schema);
            record.setData(new HashMap<>(data));
            record.setCreatedAt(LocalDateTime.now());
            // Records expire with the sandbox org
            record.setExpiresAt(expiresAt);
            mockRecordRepository.save(record);
            count++;
        }

        return count;
    }

    // =========================================================================
    // Private helpers — session management
    // =========================================================================

    private SandboxSession requireValidSession(String sandboxToken) {
        SandboxSession session = getSession(sandboxToken)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sandbox session not found. It may have already expired."));

        if (session.isHardExpired()) {
            throw new SandboxExpiredException();
        }

        return session;
    }

    private void storeSession(String rawToken, SandboxSession session, Duration ttl) {
        redisTemplate.opsForValue()
                .set(SESSION_KEY_PREFIX + rawToken, session, ttl);
    }

    /**
     * DB fallback: reconstruct a minimal session from the user's stored token hash.
     * Used when Redis is temporarily unavailable.
     */
    private Optional<SandboxSession> getSessionFromDb(String rawToken) {
        try {
            String tokenHash = cryptoService.hashApiKey(rawToken, globalSecret);
            return userRepository.findBySandboxTokenHash(tokenHash)
                    .map(user -> {
                        long createdMs = user.getSandboxCreatedAt() != null
                                ? user.getSandboxCreatedAt()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                                : System.currentTimeMillis() - HARD_CAP.toMillis();

                        // Find the sandbox org
                        List<Organization> orgs =
                                organizationRepository.findByIsSandboxTrueAndExpiresAtBefore(
                                        LocalDateTime.now().plusYears(10));
                        UUID orgId = orgs.stream()
                                .filter(o -> o.getOwner().getId().equals(user.getId()))
                                .map(Organization::getId)
                                .findFirst()
                                .orElse(null);

                        return SandboxSession.builder()
                                .userId(user.getId())
                                .organizationId(orgId)
                                .createdAtEpochMs(createdMs)
                                .hardExpiresAtEpochMs(createdMs + HARD_CAP.toMillis())
                                .build();
                    });
        } catch (Exception e) {
            log.error("DB fallback for sandbox session also failed", e);
            return Optional.empty();
        }
    }

    // =========================================================================
    // Private helpers — auth session builder
    // =========================================================================

    /**
     * Builds a full AuthResult for a newly-converted user.
     * Duplicates the logic in AuthServiceImpl.login() intentionally —
     * extracting a shared method would require adding a public API
     * to AuthService that couples it to SandboxService's needs.
     * TODO: Refactor into a shared TokenIssuanceService in a future cleanup PR.
     */
    private AuthResult buildAuthResult(User user) {
        String accessToken  = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getRole());

        ResponseCookie refreshCookie = cookieUtil.createRefreshToken(refreshToken);

        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .user(userMapper.toResponse(user))
                .build();

        return new AuthResult(response, refreshCookie);
    }
}