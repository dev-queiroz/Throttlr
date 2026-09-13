# Throttlr

Sistema de rate limiting distribuido e quota em Java 21, Spring Boot 3, Redis Cluster, Kafka, Micrometer/Prometheus e Resilience4j.

## Arquitetura

O Throttlr avalia cada requisicao HTTP com politicas hierarquicas nesta ordem:

1. User Override
2. Organization Limit
3. Plan Limit
4. Global Limit

A decisao atomica de consumo acontece no Redis via Lua, com uma chamada por requisicao. Requisicoes aceitas publicam um evento assíncrono no Kafka para quota tracking e billing em tempo real.

Algoritmos implementados:

- Token Bucket: burst controlado com refill continuo.
- Sliding Window Counter: janela ponderada entre bucket atual e anterior.
- Leaky Bucket: suavizacao de trafego por taxa de vazamento.

Fallback:

- `FAIL_OPEN`: libera trafego quando Redis esta indisponivel.
- `FAIL_CLOSED`: bloqueia trafego quando Redis esta indisponivel.

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

## Teste rapido

```bash
curl -i \
  -H "X-Tenant-Id: demo" \
  -H "X-Organization-Id: acme" \
  -H "X-Plan-Id: free" \
  -H "X-User-Id: vip-user" \
  http://localhost:8080/api/demo
```

Headers aceitos:

- `X-Tenant-Id`
- `X-Organization-Id`
- `X-Plan-Id`
- `X-User-Id`
- `X-Request-Cost`

Headers de resposta:

- `X-RateLimit-Limit`
- `X-RateLimit-Remaining`
- `X-RateLimit-Policy`
- `X-RateLimit-Soft-Limit`
- `X-RateLimit-Fallback`
- `Retry-After`

## Observabilidade

Metricas principais:

- `redis.rate.limit.latency`: histograma com p95/p99.
- `redis.rate.limit.failures`: falhas de execucao Redis/Lua.
- `rate.limit.rejected`: respostas HTTP 429.
- `rate.limit.soft_limited`: requisicoes aceitas acima do soft limit.
- `rate.limit.fallback`: decisoes tomadas por fallback.
- `usage.events.published`: eventos enviados ao Kafka.
- `usage.events.failed`: falhas de publicacao no Kafka.

## Desenvolvimento

O projeto usa commits convencionais e branches curtas:

- `feat/*` para funcionalidades.
- `fix/*` para correcoes.
- `docs/*` para documentacao.

Neste workspace, os merges foram feitos localmente com commits de merge para representar o fluxo de PR sem escrever direto na `main` depois da fundacao inicial.
