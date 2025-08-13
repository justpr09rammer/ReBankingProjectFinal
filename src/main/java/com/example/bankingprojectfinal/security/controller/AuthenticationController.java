package com.example.bankingprojectfinal.security.controller;

import com.example.bankingprojectfinal.Exception.IncorrectPasswordException;
import com.example.bankingprojectfinal.Exception.UserHasBeenDisabledException;
import com.example.bankingprojectfinal.Exception.UserNotFoundException;
import com.example.bankingprojectfinal.Model.Entity.UserEntity;
import com.example.bankingprojectfinal.Model.Enums.UserStatus;
import com.example.bankingprojectfinal.Repository.UserRepository;
import com.example.bankingprojectfinal.security.dto.AuthenticationResponse;
import com.example.bankingprojectfinal.security.jwt.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

record LoginRequest(String username, String password) {}

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
	JwtUtil jwtUtil;
	UserRepository userRepository;
	PasswordEncoder passwordEncoder;

	@PostMapping("/login")
	public ResponseEntity<AuthenticationResponse> login(@RequestBody LoginRequest request) {
		UserEntity userEntity = userRepository.findById(request.username()).orElseThrow(UserNotFoundException::new);
		if (userEntity.getStatus().equals(UserStatus.DISABLED)) throw new UserHasBeenDisabledException("user has been disabled");
		if (!passwordEncoder.matches(request.password(), userEntity.getPassword())) throw new IncorrectPasswordException();

		String token = jwtUtil.generateToken(request.username());

		return ResponseEntity.ok(new AuthenticationResponse(token));
	}
}
