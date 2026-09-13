# Throttlr

Sistema de rate limiting distribuido e quota em Java 21, Spring Boot 3, Redis Cluster, Kafka, Micrometer/Prometheus e Resilience4j.

## Execucao local

```bash
docker compose up -d
mvn spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui.html`
Prometheus: `http://localhost:9090`

Infra local:

- Redis Cluster: `localhost:7000`, `localhost:7001`, `localhost:7002`
- Kafka: `localhost:9092`
- Topico de billing: `api-usage-events`
