package com.hireconnect.auth.service;

import com.hireconnect.auth.dto.LoginRequest;
import com.hireconnect.auth.dto.LoginResponse;
import com.hireconnect.auth.dto.RegisterRequest;
import com.hireconnect.auth.dto.TokenValidationResponse;
import com.hireconnect.auth.entity.AuthProvider;
import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.repository.AuthRepository;
import com.hireconnect.auth.security.JwtUtil;
import com.hireconnect.auth.service.AuthService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

	private final AuthRepository authRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;

	public AuthServiceImpl(AuthRepository authRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
		this.authRepository = authRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtUtil = jwtUtil;
	}

	@Override
	public String register(RegisterRequest request) {

		// Step 1: check if email already exists
		if (authRepository.existsByEmail(request.getEmail())) {
			throw new RuntimeException("Email already registered");
		}

		// Step 2: create entity
		UserCredential user = new UserCredential();
		user.setEmail(request.getEmail());

		// Step 3: encode password
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

		user.setRole(request.getRole());
		user.setProvider(AuthProvider.LOCAL);
		user.setCreatedAt(LocalDateTime.now());

		// Step 4: save
		authRepository.save(user);

		return "User registered successfully";
	}

//    returns string instead of token on LOGIN 
//    @Override
//    public String login(LoginRequest request)
//    {
//    	UserCredential user=authRepository.findByEmail(request.getEmail())
//    			.orElseThrow(()-> new RuntimeException("user not found"));
//    	if(!passwordEncoder.matches(request.getPassword(),user.getPasswordHash()))
//    	{
//    		throw new RuntimeException("invalid credentials");
//    	}
//    	return "Login successful for user: "+user.getEmail() ;
//    	
//    }
	@Override
	public LoginResponse login(LoginRequest request) {

		UserCredential user = authRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new RuntimeException("User not found"));

		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			throw new RuntimeException("Invalid email or passwords");
		}

		String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getUserId());

		return new LoginResponse(token, user.getEmail(), user.getRole().name());
	}

	// ✅ LOGOUT (state less → just client-side)
	@Override
	public String logout(String token) {
		return "Logout successful (client should discard token)";
	}

	// ✅ VALIDATE TOKEN
	@Override
	public TokenValidationResponse validateToken(String token) {
		try {
			String email = jwtUtil.extractEmail(token);
			return new TokenValidationResponse(true, email);
		} catch (Exception e) {
			return new TokenValidationResponse(false, null);
		}
	}

	// ✅ REFRESH TOKEN
	@Override
	public LoginResponse refreshToken(String token) {

		try {
			String email = jwtUtil.extractEmail(token);
			String role = jwtUtil.extractRole(token);
			UserCredential user = authRepository.findByEmail(email)
					.orElseThrow(() -> new RuntimeException("User not found"));

			String newToken = jwtUtil.generateToken(email, role, user.getUserId());

			return new LoginResponse(newToken, email, role);

		} catch (Exception e) {
			throw new RuntimeException("Invalid token");
		}

	}
}
