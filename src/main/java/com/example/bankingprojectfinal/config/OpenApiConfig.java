package com.example.bankingprojectfinal.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
		info = @Info(title = "Banking Project API", version = "1.0", description = "API documentation for Banking Project"),
		servers = {
				@Server(url = "/", description = "Default Server")
		}
)
public class OpenApiConfig {
}