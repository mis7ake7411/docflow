package com.docflow.infra.openapi;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiGroupsConfig {

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi folderApi() {
        return GroupedOpenApi.builder()
                .group("folder")
                .pathsToMatch("/api/folders/**")
                .build();
    }

    @Bean
    public GroupedOpenApi documentApi() {
        return GroupedOpenApi.builder()
                .group("document")
                .pathsToMatch("/api/documents/**")
                .build();
    }

    @Bean
    public GroupedOpenApi statsApi() {
        return GroupedOpenApi.builder()
                .group("stats")
                .pathsToMatch("/api/stats/**", "/api/users/me/recent-views")
                .build();
    }

    @Bean
    public GroupedOpenApi activityApi() {
        return GroupedOpenApi.builder()
                .group("activity")
                .pathsToMatch("/api/activities/**")
                .build();
    }
}
