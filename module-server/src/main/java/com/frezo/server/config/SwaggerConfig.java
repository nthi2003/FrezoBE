package com.frezo.server.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${frezo.openapi.title:FTech API Documentation}")
    private String apiTitle;

    @Value("${frezo.openapi.description:Frezo Backend}")
    private String apiDescription;

    @Value("${frezo.openapi.version:1.0.0}")
    private String apiVersion;

    @Value("${frezo.openapi.server-url:/}")
    private String serverUrl;

    @Value("${frezo.openapi.server-description:Local environment}")
    private String serverDescription;

    @Bean
    public OpenAPI openAPI() {
        OpenAPI openAPI = new OpenAPI();

        Info info = new Info();
        info.setTitle(apiTitle);
        info.setDescription(apiDescription);
        info.setVersion(apiVersion);

        License license = new License();
        license.setName("Frezo 1.0");
        license.setUrl("http://springdoc.org");
        info.setLicense(license);

        Server localServer = new Server();
        localServer.setUrl(serverUrl);
        localServer.setDescription(serverDescription);

        SecurityScheme securityScheme = new SecurityScheme();
        securityScheme.setName("Authorization");
        securityScheme.setType(SecurityScheme.Type.HTTP);
        securityScheme.setScheme("bearer");
        securityScheme.setBearerFormat("JWT");
        securityScheme.setIn(SecurityScheme.In.HEADER);

        Components components = new Components();
        components.addSecuritySchemes("Token", securityScheme);

        SecurityRequirement securityRequirement = new SecurityRequirement();
        securityRequirement.addList("Token");

        openAPI.setInfo(info);
        openAPI.setServers(List.of(localServer));
        openAPI.setComponents(components);
        openAPI.setSecurity(List.of(securityRequirement));

        return openAPI;
    }
}