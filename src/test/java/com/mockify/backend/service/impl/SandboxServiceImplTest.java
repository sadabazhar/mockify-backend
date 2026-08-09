package com.mockify.backend.service.impl;

import com.mockify.backend.common.enums.UserRole;
import com.mockify.backend.dto.response.sandbox.SandboxCreationResult;
import com.mockify.backend.dto.response.sandbox.SandboxResumeResult;
import com.mockify.backend.exception.DuplicateResourceException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.exception.SandboxExpiredException;
import com.mockify.backend.mapper.UserMapper;
import com.mockify.backend.model.*;
import com.mockify.backend.repository.*;
import com.mockify.backend.sandbox.PendingConversion;
import com.mockify.backend.sandbox.SandboxSeedDataProvider;
import com.mockify.backend.sandbox.SandboxSession;
import com.mockify.backend.security.ApiKeyCryptoService;
import com.mockify.backend.security.CookieUtil;
import com.mockify.backend.security.JwtTokenProvider;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.MailService;
import com.mockify.backend.service.SlugService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SandboxServiceImpl")
class SandboxServiceImplTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────
    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private MockSchemaRepository mockSchemaRepository;
    @Mock private MockRecordRepository mockRecordRepository;
    @Mock private SchemaTemplateRepository schemaTemplateRepository;
    @Mock private SlugService slugService;
    @Mock private EndpointService endpointService;
    @Mock private MailService mailService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private CookieUtil cookieUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApiKeyCryptoService cryptoService;
    @Mock private UserMapper userMapper;
    @Mock private SandboxSeedDataProvider seedDataProvider;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOps;

    @InjectMocks
    private SandboxServiceImpl sandboxService;

    // ── Fixtures ──────────────────────────────────────────────────────────────
    private User guestUser;
    private Organization sandboxOrg;
    private Project demoProject;
    private SandboxSession activeSession;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sandboxService, "globalSecret", "test-secret-min-32-chars-long!!!!");
        ReflectionTestUtils.setField(sandboxService, "emailVerificationBaseUrl", "http://localhost:3000/verify-email");

        guestUser = new User();
        guestUser.setId(UUID.randomUUID());
        guestUser.setRole(UserRole.GUEST);
        guestUser.setProviderName("sandbox");
        guestUser.setUsername("sandbox_testuser");

        sandboxOrg = new Organization();
        sandboxOrg.setId(UUID.randomUUID());
        sandboxOrg.setName("My Sandbox");
        sandboxOrg.setSlug("my-sandbox");
        sandboxOrg.setSandbox(true);
        sandboxOrg.setExpiresAt(LocalDateTime.now().plusHours(24));
        sandboxOrg.setOwner(guestUser);

        demoProject = new Project();
        demoProject.setId(UUID.randomUUID());
        demoProject.setName("Demo API");
        demoProject.setSlug("demo-api");
        demoProject.setOrganization(sandboxOrg);

        activeSession = SandboxSession.builder()
                .userId(guestUser.getId())
                .organizationId(sandboxOrg.getId())
                .createdAtEpochMs(System.currentTimeMillis())
                .hardExpiresAtEpochMs(System.currentTimeMillis() + Duration.ofHours(24).toMillis())
                .build();
    }

    // =========================================================================
    // createSandbox
    // =========================================================================

    @Nested
    @DisplayName("createSandbox()")
    class CreateSandbox {

        @BeforeEach
        void setUpCreateSandboxMocks() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(slugService.generateSlug("My Sandbox")).thenReturn("my-sandbox");
            when(slugService.generateSlug("Demo API")).thenReturn("demo-api");
            when(organizationRepository.existsBySlug("my-sandbox")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(guestUser);
            when(organizationRepository.save(any(Organization.class))).thenReturn(sandboxOrg);
            when(projectRepository.save(any(Project.class))).thenReturn(demoProject);
            when(cryptoService.hashApiKey(anyString(), anyString())).thenReturn("hashed-token");
            when(jwtTokenProvider.generateAccessToken(any(), eq(UserRole.GUEST)))
                    .thenReturn("guest.jwt.token");
            when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);
            when(cookieUtil.createSandboxTokenCookie(anyString()))
                    .thenReturn(ResponseCookie.from("sandbox_token", "token").build());
            // No templates found — seeding skipped (tested separately)
            when(schemaTemplateRepository.findBySlugAndSystemTemplateTrue(anyString()))
                    .thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("Returns a valid SandboxCreationResult with JWT and cookie")
        void createSandbox_returnsValidResult() {
            SandboxCreationResult result = sandboxService.createSandbox("127.0.0.1");

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("guest.jwt.token");
            assertThat(result.getExpiresIn()).isEqualTo(3600000L);
            assertThat(result.getSandboxCookie()).isNotNull();
            assertThat(result.getWorkspace()).isNotNull();
        }

        @Test
        @DisplayName("Creates a guest user with GUEST role and null email")
        void createSandbox_createsGuestUser() {
            sandboxService.createSandbox("127.0.0.1");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            // First save is the initial guest user creation
            verify(userRepository, atLeast(1)).save(userCaptor.capture());

            User savedUser = userCaptor.getAllValues().get(0);
            assertThat(savedUser.getRole()).isEqualTo(UserRole.GUEST);
            assertThat(savedUser.getEmail()).isNull();
            assertThat(savedUser.getProviderName()).isEqualTo("sandbox");
        }

        @Test
        @DisplayName("Creates sandbox organization with is_sandbox=true and expires_at set")
        void createSandbox_createsSandboxOrg() {
            sandboxService.createSandbox("127.0.0.1");

            ArgumentCaptor<Organization> orgCaptor =
                    ArgumentCaptor.forClass(Organization.class);
            verify(organizationRepository).save(orgCaptor.capture());

            Organization org = orgCaptor.getValue();
            assertThat(org.isSandbox()).isTrue();
            assertThat(org.getExpiresAt()).isNotNull();
            assertThat(org.getExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("Stores session in Redis with hard cap TTL")
        void createSandbox_storesSessionInRedis() {
            sandboxService.createSandbox("127.0.0.1");

            verify(valueOps).set(
                    startsWith(SandboxServiceImpl.SESSION_KEY_PREFIX),
                    any(SandboxSession.class),
                    eq(SandboxServiceImpl.HARD_CAP)
            );
        }

        @Test
        @DisplayName("Creates endpoint for org and project")
        void createSandbox_createsEndpoints() {
            sandboxService.createSandbox("127.0.0.1");

            verify(endpointService).createEndpoint(any(Organization.class));
            verify(endpointService).createEndpoint(any(Project.class));
        }

        @Test
        @DisplayName("Workspace contains correct slugs")
        void createSandbox_workspaceHasCorrectSlugs() {
            SandboxCreationResult result = sandboxService.createSandbox("127.0.0.1");

            assertThat(result.getWorkspace().getOrgSlug()).isEqualTo("my-sandbox");
            assertThat(result.getWorkspace().getProjectSlug()).isEqualTo("demo-api");
        }

        @Test
        @DisplayName("Handles slug collision by generating unique slug")
        void createSandbox_handlesSlugCollision() {
            when(organizationRepository.existsBySlug("my-sandbox")).thenReturn(true);
            when(slugService.generateUniqueSlug("my-sandbox")).thenReturn("my-sandbox-abc12345");

            sandboxService.createSandbox("127.0.0.1");

            verify(slugService).generateUniqueSlug("my-sandbox");
        }
    }

    // =========================================================================
    // resumeSandbox
    // =========================================================================

    @Nested
    @DisplayName("resumeSandbox()")
    class ResumeSandbox {
        @BeforeEach
        void setUpRedis() {
            // resumeSandbox() → requireValidSession() → getSession() → opsForValue().get()
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        @DisplayName("Returns new JWT for valid, non-expired token")
        void resumeSandbox_validToken_returnsNewJwt() {
            when(valueOps.get(SESSION_KEY("valid-token"))).thenReturn(activeSession);
            when(jwtTokenProvider.generateAccessToken(guestUser.getId(), UserRole.GUEST))
                    .thenReturn("new.guest.jwt");
            when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

            SandboxResumeResult result = sandboxService.resumeSandbox("valid-token");

            assertThat(result.getAccessToken()).isEqualTo("new.guest.jwt");
            assertThat(result.getExpiresIn()).isEqualTo(3600000L);
        }

        @Test
        @DisplayName("Throws SandboxExpiredException when hard cap is exceeded")
        void resumeSandbox_expiredSession_throws() {
            SandboxSession expiredSession = SandboxSession.builder()
                    .userId(guestUser.getId())
                    .createdAtEpochMs(System.currentTimeMillis() - 90_000_000L)
                    .hardExpiresAtEpochMs(System.currentTimeMillis() - 1_000L) // past
                    .build();

            when(valueOps.get(SESSION_KEY("expired-token"))).thenReturn(expiredSession);

            assertThatThrownBy(() -> sandboxService.resumeSandbox("expired-token"))
                    .isInstanceOf(SandboxExpiredException.class);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when token is unknown")
        void resumeSandbox_unknownToken_throws() {
            when(valueOps.get(anyString())).thenReturn(null);

            assertThatThrownBy(() -> sandboxService.resumeSandbox("unknown-token"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Rolls TTL on valid resume")
        void resumeSandbox_rollsTtl() {
            when(valueOps.get(SESSION_KEY("valid-token"))).thenReturn(activeSession);
            when(jwtTokenProvider.generateAccessToken(any(), any())).thenReturn("jwt");
            when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

            sandboxService.resumeSandbox("valid-token");

            verify(redisTemplate).expire(eq(SESSION_KEY("valid-token")), any());
        }
    }

    // =========================================================================
    // initiateSandboxConversion
    // =========================================================================

    @Nested
    @DisplayName("initiateSandboxConversion()")
    class InitiateConversion {
        @BeforeEach
        void setUpRedis() {
            // initiateSandboxConversion() calls getSession() AND stores PendingConversion
            // — both paths call opsForValue()
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        @DisplayName("Sends verification email and stores PendingConversion in Redis")
        void initiateConversion_validData_sendsEmailAndStoresInRedis() {
            when(valueOps.get(SESSION_KEY("valid-token"))).thenReturn(activeSession);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

            sandboxService.initiateSandboxConversion("valid-token", "new@example.com", "password123");

            verify(valueOps).set(
                    startsWith(SandboxServiceImpl.CONVERSION_KEY_PREFIX),
                    any(PendingConversion.class),
                    eq(SandboxServiceImpl.CONVERSION_TTL)
            );
            verify(mailService).sendEmailVerificationMail(eq("new@example.com"), anyString());
        }

        @Test
        @DisplayName("Throws DuplicateResourceException when email is already registered")
        void initiateConversion_emailTaken_throws() {
            when(valueOps.get(SESSION_KEY("valid-token"))).thenReturn(activeSession);
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertThatThrownBy(() ->
                    sandboxService.initiateSandboxConversion(
                            "valid-token", "taken@example.com", "pass"))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Throws SandboxExpiredException for expired session")
        void initiateConversion_expiredSession_throws() {
            SandboxSession expired = SandboxSession.builder()
                    .userId(guestUser.getId())
                    .hardExpiresAtEpochMs(System.currentTimeMillis() - 1000)
                    .build();

            when(valueOps.get(SESSION_KEY("expired"))).thenReturn(expired);

            assertThatThrownBy(() ->
                    sandboxService.initiateSandboxConversion(
                            "expired", "test@example.com", "pass"))
                    .isInstanceOf(SandboxExpiredException.class);
        }

        @Test
        @DisplayName("Never sends email if any pre-condition fails")
        void initiateConversion_failedPrecondition_doesNotSendEmail() {
            when(valueOps.get(anyString())).thenReturn(null); // token not found

            assertThatThrownBy(() ->
                    sandboxService.initiateSandboxConversion(
                            "bad", "test@example.com", "pass"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(mailService);
        }
    }

    // =========================================================================
    // getSession / rollTtl
    // =========================================================================

    @Nested
    @DisplayName("getSession() and rollTtl()")
    class SessionManagement {

        @Test
        @DisplayName("getSession returns empty Optional for null token")
        void getSession_nullToken_returnsEmpty() {
            assertThat(sandboxService.getSession(null)).isEmpty();
            assertThat(sandboxService.getSession("")).isEmpty();
        }

        @Test
        @DisplayName("getSession returns present Optional for known token")
        void getSession_knownToken_returnsSession() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(SESSION_KEY("known"))).thenReturn(activeSession);

            Optional<SandboxSession> result = sandboxService.getSession("known");

            assertThat(result).isPresent();
            assertThat(result.get().getUserId()).isEqualTo(guestUser.getId());
        }

        @Test
        @DisplayName("rollTtl does not extend beyond hard cap")
        void rollTtl_doesNotExceedHardCap() {
            // Session expires in 30 seconds (less than ROLLING_TTL of 4 hours)
            SandboxSession almostExpired = SandboxSession.builder()
                    .userId(guestUser.getId())
                    .hardExpiresAtEpochMs(System.currentTimeMillis() + 30_000)
                    .build();

            sandboxService.rollTtl("some-token", almostExpired);

            // Should expire in ~30s, not 4 hours
            ArgumentCaptor<java.time.Duration> durationCaptor =
                    ArgumentCaptor.forClass(java.time.Duration.class);
            verify(redisTemplate).expire(anyString(), durationCaptor.capture());

            assertThat(durationCaptor.getValue().getSeconds()).isLessThanOrEqualTo(30);
        }

        @Test
        @DisplayName("rollTtl skips extension for hard-expired session")
        void rollTtl_alreadyExpired_doesNothing() {
            SandboxSession expired = SandboxSession.builder()
                    .hardExpiresAtEpochMs(System.currentTimeMillis() - 1000)
                    .build();

            sandboxService.rollTtl("token", expired);

            verifyNoInteractions(redisTemplate);
        }
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private String SESSION_KEY(String token) {
        return SandboxServiceImpl.SESSION_KEY_PREFIX + token;
    }
}