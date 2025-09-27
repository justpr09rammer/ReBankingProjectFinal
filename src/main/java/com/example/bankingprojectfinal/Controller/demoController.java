package com.example.bankingprojectfinal.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/demo")
public class demoController {

//    @GetMapping
//    public ResponseEntity<String> getUserInfo(HttpServletRequest request) {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//
//        // Get JWT token from Authorization header
//        String authHeader = request.getHeader("Authorization");
//        String jwtToken = null;
//
//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            jwtToken = authHeader.substring(7);
//        }
//
//        StringBuilder response = new StringBuilder();
//        response.append("=== User Information ===\n");
//
//        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
//            response.append("Username: ").append(auth.getName()).append("\n");
//            response.append("User ID: ").append(auth.getName()).append("\n"); // Assuming username is the user ID
//            response.append("Authorities: ").append(
//                    auth.getAuthorities().stream()
//                            .map(grantedAuthority -> grantedAuthority.getAuthority())
//                            .collect(Collectors.joining(", "))
//            ).append("\n");
//            response.append("Authenticated: ").append(auth.isAuthenticated()).append("\n");
//            response.append("Principal: ").append(auth.getPrincipal().getClass().getSimpleName()).append("\n");
//        } else {
//            response.append("Status: Not authenticated\n");
//            response.append("Principal: anonymousUser\n");
//        }
//
//        response.append("\n=== JWT Token ===\n");
//        if (jwtToken != null) {
//            response.append("Token: ").append(jwtToken).append("\n");
//            response.append("Token Length: ").append(jwtToken.length()).append(" characters\n");
//
//            // You can add JWT token parsing here if you want to extract claims
//            // For example: String userId = getUserIdFromToken(jwtToken);
//        } else {
//            response.append("No JWT token found in Authorization header\n");
//        }
//
//        return ResponseEntity.ok(response.toString());
//    }
    @GetMapping
    public String getHello(){
        return "Hello";
    }
}