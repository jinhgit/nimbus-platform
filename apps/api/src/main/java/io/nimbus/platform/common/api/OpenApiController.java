package io.nimbus.platform.common.api;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Fixed OpenAPI 3 snapshot (docs/api/openapi.yaml 동기화본).
 * springdoc 없이 스펙 파일을 서빙한다.
 */
@RestController
public class OpenApiController {

    @GetMapping(value = {"/v3/api-docs", "/api/v1/openapi.yaml"}, produces = "application/yaml")
    public ResponseEntity<String> openApiYaml() throws Exception {
        ClassPathResource resource = new ClassPathResource("openapi.yaml");
        String body = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/yaml"))
                .body(body);
    }

    @GetMapping(value = "/api/v1/openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<OpenApiMeta> openApiMeta() {
        return ApiResponse.ok(new OpenApiMeta(
                "Nimbus Platform API",
                "0.1.0",
                "/v3/api-docs",
                "docs/api/openapi.yaml (repo) · classpath:openapi.yaml"
        ));
    }

    public record OpenApiMeta(String title, String version, String yamlPath, String source) {
    }
}
