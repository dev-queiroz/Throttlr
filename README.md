# Throttlr

Sistema de rate limiting distribuido e quota em Java 21, Spring Boot 3, Redis Cluster, Kafka, Micrometer/Prometheus e Resilience4j.

## Execucao local

```bash
docker compose up -d
mvn spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui.html`
Prometheus: `http://localhost:9090`
