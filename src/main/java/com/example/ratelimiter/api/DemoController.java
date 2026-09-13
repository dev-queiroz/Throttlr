package com.example.ratelimiter.api;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/demo", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Demo")
public class DemoController {

    @GetMapping
    @Operation(
            summary = "Endpoint protegido pelo Throttlr",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Requisicao aceita",
                            headers = {
                                    @Header(name = "X-RateLimit-Limit"),
                                    @Header(name = "X-RateLimit-Remaining"),
                                    @Header(name = "X-RateLimit-Soft-Limit"),
                                    @Header(name = "X-RateLimit-Fallback")
                            }),
                    @ApiResponse(responseCode = "429", description = "Hard limit excedido",
                            content = @Content(schema = @Schema(implementation = Map.class)))
            }
    )
    public Map<String, Object> demo(
            @Parameter(description = "Tenant da requisicao")
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "demo") String tenantId,
            @Parameter(description = "Usuario final")
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId
    ) {
        return Map.of(
                "tenantId", tenantId,
                "userId", userId,
                "status", "accepted",
                "timestamp", Instant.now().toString()
        );
    }
}
