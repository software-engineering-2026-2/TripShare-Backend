package ufcg.software.engineering.trip.share.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(title = "TripShare API", version = "1.0"))
@SecurityScheme(name = "sessionBearer", type = SecuritySchemeType.HTTP, scheme = "bearer",
        description = "Token opaco retornado por POST /auth/login; não usar o ID token Firebase nas rotas protegidas.")
public class OpenApiConfig { }
