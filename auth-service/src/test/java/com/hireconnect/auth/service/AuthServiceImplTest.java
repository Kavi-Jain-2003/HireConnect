package com.hireconnect.auth.service;

// ════════════════════════════════════════════════════════════════
//  AuthServiceImpl — Unit Tests
//  Framework : JUnit 5 + Mockito
//  Kya test kar rahe hain:
//    1.  register()      — successful registration
//    2.  register()      — duplicate email
//    3.  register()      — admin role blocked
//    4.  login()         — successful login
//    5.  login()         — user not found
//    6.  login()         — wrong password
//    7.  login()         — suspended user
//    8.  logout()        — successful logout
//    9.  logout()        — null token
//    10. validateToken() — valid token
//    11. validateToken() — invalid token
// ════════════════════════════════════════════════════════════════

import com.hireconnect.auth.dto.LoginRequest;
import com.hireconnect.auth.dto.LoginResponse;
import com.hireconnect.auth.dto.RegisterRequest;
import com.hireconnect.auth.dto.TokenValidationResponse;
import com.hireconnect.auth.entity.AuthProvider;
import com.hireconnect.auth.entity.Role;
import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.repository.AuthRepository;
import com.hireconnect.auth.security.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    // ── Mocks ─────────────────────────────────────────────────────
    @Mock
    private AuthRepository authRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlackListService blacklistService;

    // ── Real class jo test karni hai ──────────────────────────────
    @InjectMocks
    private AuthServiceImpl authService;

    // ── Test Data ─────────────────────────────────────────────────
    private UserCredential sampleUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Sample user — jaise DB mein hota hai
        sampleUser = new UserCredential();
        sampleUser.setEmail("candidate1@gmail.com");
        sampleUser.setPasswordHash("$2a$hashedpassword");
        sampleUser.setRole(Role.CANDIDATE);
        sampleUser.setProvider(AuthProvider.LOCAL);
        sampleUser.setCreatedAt(LocalDateTime.now());

        // Register request — frontend se aata hai
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@gmail.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole(Role.CANDIDATE);

        // Login request
        loginRequest = new LoginRequest();
        loginRequest.setEmail("candidate1@gmail.com");
        loginRequest.setPassword("password123");
    }

    // ════════════════════════════════════════════════════════════
    // TEST 1 — register() — Successful
    // Kya test: Naya user successfully register ho jaaye
    // Expected: "User registered successfully"
    // ════════════════════════════════════════════════════════════
    @Test
    void register_WhenNewEmail_ShouldRegisterSuccessfully() {
        // ARRANGE
        // Email exist nahi karta
        when(authRepository.existsByEmail("newuser@gmail.com")).thenReturn(false);
        // Password encode ho jaaye
        when(passwordEncoder.encode("password123")).thenReturn("$2a$hashedpassword");
        // Save successful
        when(authRepository.save(any(UserCredential.class))).thenReturn(sampleUser);

        // ACT
        String result = authService.register(registerRequest);

        // ASSERT
        assertEquals("User registered successfully", result);
        verify(authRepository, times(1)).save(any(UserCredential.class));
        verify(passwordEncoder, times(1)).encode("password123");
    }

    // ════════════════════════════════════════════════════════════
    // TEST 2 — register() — Duplicate Email
    // Kya test: Same email dobara register nahi ho sakti
    // Expected: RuntimeException — "Email already registered"
    // ════════════════════════════════════════════════════════════
    @Test
    void register_WhenEmailAlreadyExists_ShouldThrowException() {
        // ARRANGE — email pehle se exist karta hai
        when(authRepository.existsByEmail("newuser@gmail.com")).thenReturn(true);

        // ACT + ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals("Email already registered", exception.getMessage());
        // Save NAHI hona chahiye
        verify(authRepository, never()).save(any(UserCredential.class));
    }

    // ════════════════════════════════════════════════════════════
    // TEST 3 — register() — Admin Role Blocked
    // Kya test: ADMIN role se register nahi kar sakte
    // Expected: RuntimeException — "Cannot register as admin"
    // ════════════════════════════════════════════════════════════
    @Test
    void register_WhenAdminRole_ShouldThrowException() {
        // ARRANGE — admin role set karo
        registerRequest.setRole(Role.ADMIN);
        when(authRepository.existsByEmail("newuser@gmail.com")).thenReturn(false);

        // ACT + ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals("Cannot register as admin", exception.getMessage());
        verify(authRepository, never()).save(any(UserCredential.class));
    }

    // ════════════════════════════════════════════════════════════
    // TEST 4 — login() — Successful
    // Kya test: Sahi email + password se login ho jaaye
    // Expected: LoginResponse with token, email, role
    // ════════════════════════════════════════════════════════════
    @Test
    void login_WhenValidCredentials_ShouldReturnLoginResponse() {
        // ARRANGE
        when(authRepository.findByEmail("candidate1@gmail.com"))
                .thenReturn(Optional.of(sampleUser));

        // Password match karo
        when(passwordEncoder.matches("password123", "$2a$hashedpassword"))
                .thenReturn(true);

        // JWT token generate karo
        when(jwtUtil.generateToken(anyString(), anyString(), any()))
                .thenReturn("eyJhbGciOiJIUzI1NiJ9.sample.token");

        // ACT
        LoginResponse response = authService.login(loginRequest);

        // ASSERT
        assertNotNull(response);
        assertEquals("candidate1@gmail.com", response.getEmail());
        assertEquals("CANDIDATE", response.getRole());
        assertNotNull(response.getToken()); // token null nahi hona chahiye
    }

    // ════════════════════════════════════════════════════════════
    // TEST 5 — login() — User Not Found
    // Kya test: Wrong email pe login fail ho
    // Expected: RuntimeException — "User not found"
    // ════════════════════════════════════════════════════════════
    @Test
    void login_WhenUserNotFound_ShouldThrowException() {
        // ARRANGE — email DB mein nahi hai
        when(authRepository.findByEmail("wrong@gmail.com"))
                .thenReturn(Optional.empty());

        loginRequest.setEmail("wrong@gmail.com");

        // ACT + ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals("User not found", exception.getMessage());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 6 — login() — Wrong Password
    // Kya test: Galat password pe login fail ho
    // Expected: RuntimeException — "Invalid email or password"
    // ════════════════════════════════════════════════════════════
    @Test
    void login_WhenWrongPassword_ShouldThrowException() {
        // ARRANGE
        when(authRepository.findByEmail("candidate1@gmail.com"))
                .thenReturn(Optional.of(sampleUser));

        // Password match NAHI karta
        when(passwordEncoder.matches("wrongpassword", "$2a$hashedpassword"))
                .thenReturn(false);

        loginRequest.setPassword("wrongpassword");

        // ACT + ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals("Invalid email or password", exception.getMessage());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 7 — login() — Suspended User
    // Kya test: Suspended user login nahi kar sakta
    // Expected: RuntimeException — "Your account has been suspended"
    // ════════════════════════════════════════════════════════════
    @Test
    void login_WhenUserSuspended_ShouldThrowException() {
        // ARRANGE — user ko suspend karo
        sampleUser.setSuspended(true);

        when(authRepository.findByEmail("candidate1@gmail.com"))
                .thenReturn(Optional.of(sampleUser));

        when(passwordEncoder.matches("password123", "$2a$hashedpassword"))
                .thenReturn(true);

        // ACT + ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });

        assertTrue(exception.getMessage().contains("suspended"));
    }

    // ════════════════════════════════════════════════════════════
    // TEST 8 — logout() — Successful
    // Kya test: Valid token pe logout successful ho
    // Expected: "Logout successful" + token blacklist mein jaaye
    // ════════════════════════════════════════════════════════════
    @Test
    void logout_WhenValidToken_ShouldBlacklistAndReturnSuccess() {
        // ARRANGE
        String token = "eyJhbGciOiJIUzI1NiJ9.valid.token";

        // Token ki expiry future mein hai
        Date futureDate = new Date(System.currentTimeMillis() + 3600000); // 1 hour
        when(jwtUtil.extractExpiration(token)).thenReturn(futureDate);

        // ACT
        String result = authService.logout(token);

        // ASSERT
        assertEquals("Logout successful", result);
        // Blacklist mein token add hua
        verify(blacklistService, times(1)).blacklist(eq(token), any());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 9 — logout() — Null Token
    // Kya test: Null token pe bhi logout successful ho
    // Expected: "Logout successful" (gracefully handle)
    // ════════════════════════════════════════════════════════════
    @Test
    void logout_WhenNullToken_ShouldReturnSuccess() {
        // ACT
        String result = authService.logout(null);

        // ASSERT
        assertEquals("Logout successful", result);
        // Blacklist call NAHI hona chahiye null token pe
        verify(blacklistService, never()).blacklist(any(), any());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 10 — validateToken() — Valid Token
    // Kya test: Valid token pe email extract ho
    // Expected: valid=true, email milni chahiye
    // ════════════════════════════════════════════════════════════
    @Test
    void validateToken_WhenValidToken_ShouldReturnTrueWithEmail() {
        // ARRANGE
        String token = "eyJhbGciOiJIUzI1NiJ9.valid.token";
        when(jwtUtil.extractEmail(token)).thenReturn("candidate1@gmail.com");

        // ACT
        TokenValidationResponse response = authService.validateToken(token);

        // ASSERT
        assertTrue(response.isValid());
        assertEquals("candidate1@gmail.com", response.getEmail());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 11 — validateToken() — Invalid Token
    // Kya test: Invalid/expired token pe valid=false aaye
    // Expected: valid=false, email=null
    // ════════════════════════════════════════════════════════════
    @Test
    void validateToken_WhenInvalidToken_ShouldReturnFalse() {
        // ARRANGE — exception throw ho jab invalid token aaye
        when(jwtUtil.extractEmail("invalid.token"))
                .thenThrow(new RuntimeException("Invalid JWT"));

        // ACT
        TokenValidationResponse response = authService.validateToken("invalid.token");

        // ASSERT
        assertFalse(response.isValid());
        assertNull(response.getEmail());
    }
}
