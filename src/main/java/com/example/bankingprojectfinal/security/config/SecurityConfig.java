package com.example.bankingprojectfinal.security.config;

import com.example.bankingprojectfinal.Exception.handler.CustomAccessDeniedHandler;
import com.example.bankingprojectfinal.Exception.handler.CustomAuthenticationEntryPointHandler;
import com.example.bankingprojectfinal.security.jwt.JwtAuthenticationFilter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.scheduling.annotation.EnableScheduling;


@Configuration
@EnableScheduling
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {

	JwtAuthenticationFilter jwtAuthenticationFilter;
	UserDetailsService userDetailsService;
	CustomAccessDeniedHandler customAccessDeniedHandler;
	CustomAuthenticationEntryPointHandler customAuthenticationEntryPointHandler;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)) // For form login sessions
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
								"/api/v1/auth/**", "/login", "/home",
								"/css/**", "/js/**", "/images/**"
						).permitAll()
						.requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("USER", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
						.anyRequest().authenticated()
				)
				.exceptionHandling(ex -> ex
						.accessDeniedHandler(customAccessDeniedHandler)
						.authenticationEntryPoint(customAuthenticationEntryPointHandler)
				)
				.formLogin(form -> form
						.loginPage("/login")
						.defaultSuccessUrl("/home", true)
						.permitAll()
				)
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/login?logout")
						.permitAll()
				)
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}
}
