package com.firefly.idp.internaldb.service;

import com.firefly.idp.internaldb.config.InternalDbProperties;
import com.firefly.idp.internaldb.domain.RefreshToken;
import com.firefly.idp.internaldb.domain.Session;
import com.firefly.idp.internaldb.domain.User;
import com.firefly.idp.internaldb.repository.RefreshTokenRepository;
import com.firefly.idp.internaldb.repository.SessionRepository;
import com.firefly.idp.internaldb.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RoleService roleService;

    @Mock
    private InternalDbProperties properties;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private UUID testUserId;
    private InternalDbProperties.JwtConfig jwtProperties;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .mfaEnabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testUser.markAsNotNew();

        jwtProperties = new InternalDbProperties.JwtConfig();
        jwtProperties.setAccessTokenExpiration(900000L); // 15 minutes
        jwtProperties.setRefreshTokenExpiration(604800000L); // 7 days
        lenient().when(properties.getJwt()).thenReturn(jwtProperties);
    }

    @Test
    void shouldAuthenticateSuccessfully() {
        // Given
        String username = "testuser";
        String password = "password123";
        String accessToken = "access.token.here";
        String refreshToken = "refresh.token.here";

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(testUser));
        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(true);
        when(roleService.getUserRoles(testUserId)).thenReturn(Flux.just("USER", "ADMIN"));
        when(jwtTokenService.generateAccessToken(any(User.class), any())).thenReturn(accessToken);
        when(jwtTokenService.generateRefreshToken(any(User.class), any(UUID.class))).thenReturn(refreshToken);
        when(jwtTokenService.extractJti(accessToken)).thenReturn("access-jti");
        when(jwtTokenService.extractJti(refreshToken)).thenReturn("refresh-jti");
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

        // When & Then
        StepVerifier.create(authenticationService.authenticate(username, password))
                .assertNext(tokenResponse -> {
                    assertThat(tokenResponse.getAccessToken()).isEqualTo(accessToken);
                    assertThat(tokenResponse.getRefreshToken()).isEqualTo(refreshToken);
                    assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
                })
                .verifyComplete();

        verify(userRepository, atLeast(1)).findByUsername(username);
        verify(passwordEncoder).matches(password, testUser.getPasswordHash());
        verify(sessionRepository).save(any(Session.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldFailAuthenticationWithInvalidPassword() {
        // Given
        String username = "testuser";
        String password = "wrongpassword";

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(testUser));
        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(false);

        // When & Then
        StepVerifier.create(authenticationService.authenticate(username, password))
                .expectErrorMessage("Invalid username or password")
                .verify();

        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(password, testUser.getPasswordHash());
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldFailAuthenticationWhenUserNotFound() {
        // Given
        String username = "nonexistent";
        String password = "password123";

        when(userRepository.findByUsername(username)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(authenticationService.authenticate(username, password))
                .expectErrorMessage("Invalid username or password")
                .verify();

        verify(userRepository).findByUsername(username);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void shouldFailAuthenticationWhenUserDisabled() {
        // Given
        testUser.setEnabled(false);
        String username = "testuser";
        String password = "password123";

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(testUser));

        // When & Then
        StepVerifier.create(authenticationService.authenticate(username, password))
                .expectErrorMessage("User account is disabled")
                .verify();

        verify(userRepository).findByUsername(username);
    }

    @Test
    void shouldFailAuthenticationWhenUserLocked() {
        // Given
        testUser.setAccountNonLocked(false);
        String username = "testuser";
        String password = "password123";

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(testUser));

        // When & Then
        StepVerifier.create(authenticationService.authenticate(username, password))
                .expectErrorMessage("User account is locked")
                .verify();

        verify(userRepository).findByUsername(username);
    }

    @Test
    void shouldValidateAccessToken() {
        // Given
        String accessToken = "valid.access.token";
        String jti = "token-jti";
        Session session = Session.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .accessTokenJti(jti)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(jwtTokenService.validateToken(accessToken)).thenReturn(true);
        when(jwtTokenService.extractJti(accessToken)).thenReturn(jti);
        when(sessionRepository.findByAccessTokenJti(jti)).thenReturn(Mono.just(session));

        // When & Then
        StepVerifier.create(authenticationService.validateAccessToken(accessToken))
                .assertNext(isValid -> assertThat(isValid).isTrue())
                .verifyComplete();

        verify(jwtTokenService).validateToken(accessToken);
        verify(sessionRepository).findByAccessTokenJti(jti);
    }

    @Test
    void shouldRejectInvalidAccessToken() {
        // Given
        String accessToken = "invalid.access.token";

        when(jwtTokenService.validateToken(accessToken)).thenReturn(false);

        // When & Then
        StepVerifier.create(authenticationService.validateAccessToken(accessToken))
                .assertNext(isValid -> assertThat(isValid).isFalse())
                .verifyComplete();

        verify(jwtTokenService).validateToken(accessToken);
        verify(sessionRepository, never()).findByAccessTokenJti(anyString());
    }

    @Test
    void shouldLogoutUser() {
        // Given
        String accessToken = "access.token";
        String refreshToken = "refresh.token";
        String accessJti = "access-jti";
        String refreshJti = "refresh-jti";

        Session session = Session.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .accessTokenJti(accessJti)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .tokenJti(refreshJti)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(jwtTokenService.extractJti(accessToken)).thenReturn(accessJti);
        when(jwtTokenService.extractJti(refreshToken)).thenReturn(refreshJti);
        when(sessionRepository.findByAccessTokenJti(accessJti)).thenReturn(Mono.just(session));
        when(refreshTokenRepository.findByTokenJti(refreshJti)).thenReturn(Mono.just(refreshTokenEntity));
        when(sessionRepository.save(any(Session.class))).thenReturn(Mono.just(session));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(Mono.just(refreshTokenEntity));

        // When & Then
        StepVerifier.create(authenticationService.logout(accessToken, refreshToken))
                .verifyComplete();

        verify(sessionRepository).findByAccessTokenJti(accessJti);
        verify(refreshTokenRepository).findByTokenJti(refreshJti);
        verify(sessionRepository).save(any(Session.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
}
