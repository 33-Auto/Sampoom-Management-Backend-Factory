package com.sampoom.factory.common.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

//    @Value("${jwt.access.header}")
//    private String accessTokenHeader;
//
//    @Value("${jwt.refresh.header}")
//    private String refreshTokenHeader;

    public OpenAPI openAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:8080");

        return new OpenAPI()
                .info(new Info()
                        .title("삼삼오토")
                        .description("삼삼오토 REST API Document")
                        .version("1.0.0"))
                .addServersItem(server);
    }

//    @Bean
//    public OpenAPI openAPI() {
//        SecurityScheme accessTokenScheme = new SecurityScheme()
//                .type(SecurityScheme.Type.APIKEY)   // 여기 중요!
//                .in(SecurityScheme.In.HEADER)
//                .name(accessTokenHeader); // 일반적으로 "Authorization"
//
//        SecurityRequirement accessTokenRequirement = new SecurityRequirement()
//                .addList(accessTokenHeader);
//
//        SecurityScheme refreshTokenScheme = new SecurityScheme()
//                .type(SecurityScheme.Type.APIKEY)
//                .in(SecurityScheme.In.HEADER)
//                .name(refreshTokenHeader); // 예: "Refresh"
//
//        SecurityRequirement refreshTokenRequirement = new SecurityRequirement()
//                .addList(refreshTokenHeader);
//
//        Server server = new Server();
//        server.setUrl("http://localhost:8080");
//
//
//        return new OpenAPI()
//                .info(new Info()
//                        .title("삼삼오토")
//                        .description("삼삼오토 REST API Document")
//                        .version("1.0.0"))
//                .components(new Components()
//                        .addSecuritySchemes(accessTokenHeader, accessTokenScheme)
//                        .addSecuritySchemes(refreshTokenHeader, refreshTokenScheme))
//                .addServersItem(server)
//                .addSecurityItem(accessTokenRequirement)
//                .addSecurityItem(refreshTokenRequirement);
//    }
}
