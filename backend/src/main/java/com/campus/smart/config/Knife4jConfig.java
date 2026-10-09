package com.campus.smart.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j OpenAPI 3 配置类
 * 基于 springdoc-openapi，替代旧版 Swagger
 *
 * @author smart-campus
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智慧校园 API 文档")
                        .version("1.0.0")
                        .description("智慧校园微信小程序后端接口文档，包含用户端和管理端所有接口")
                        .contact(new Contact()
                                .name("智慧校园团队")
                                .email("support@smartcampus.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
