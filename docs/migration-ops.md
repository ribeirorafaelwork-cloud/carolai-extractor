# Migration Operations Guide

## Pre-requisitos

1. **carolai-platform** rodando (porta 8081)
2. Tenant provisionado via `POST /api/v1/admin/tenants`
3. Usuario admin com permissoes `STUDENT_WRITE`, `EXERCISE_WRITE` criado no tenant
4. **carolai-extractor** com dados extraidos do Firestore (tabela `customer` populada)

## Configuracao

Edite `application.properties` ou use variaveis de ambiente:

```properties
carolai.platform.url=${PLATFORM_URL:http://localhost:8081}
carolai.platform.email=${PLATFORM_EMAIL:admin@example.com}
carolai.platform.password=${PLATFORM_PASSWORD:Admin123!}
carolai.platform.tenant-id=${PLATFORM_TENANT_ID:<UUID-do-tenant>}
```

## Endpoints

### Executar Migracao

```
POST /migration/run?type={TYPE}&dryRun={true|false}&limit={N}&resume={true|false}
```

**Parametros:**

| Param   | Default | Descricao |
|---------|---------|-----------|
| type    | (obrigatorio) | `STUDENT` ou `EXERCISE` |
| dryRun  | false   | Se true, apenas loga sem enviar para API |
| limit   | 0       | Limite de registros (0 = sem limite) |
| resume  | true    | Pula entidades ja migradas (idempotencia) |

### Historico de Migracoes

```
GET /migration/runs
```

Retorna lista de todas as execucoes ordenadas por data (mais recentes primeiro).

## Fluxo Operacional

### 1. Dry-run (validacao)

```bash
curl -X POST "http://localhost:8080/migration/run?type=STUDENT&dryRun=true"
```

Revise os logs. Nenhum dado e enviado ao platform.

### 2. Migracao real de Students

```bash
# Primeiro, migrar poucos para validar
curl -X POST "http://localhost:8080/migration/run?type=STUDENT&limit=5"

# Se OK, migrar todos
curl -X POST "http://localhost:8080/migration/run?type=STUDENT"
```

### 3. Seed de Exercicios

```bash
curl -X POST "http://localhost:8080/migration/run?type=EXERCISE"
```

### 4. Verificacao pos-migracao

```bash
# Verificar historico
curl http://localhost:8080/migration/runs | jq .

# Verificar students no platform
curl -H "Authorization: Bearer <JWT>" \
     -H "X-Tenant-Id: <TENANT_ID>" \
     http://localhost:8081/api/v1/students | jq .

# Verificar exercicios no platform
curl -H "Authorization: Bearer <JWT>" \
     -H "X-Tenant-Id: <TENANT_ID>" \
     http://localhost:8081/api/v1/exercises | jq .
```

## Re-execucao Segura

A migracao e idempotente via `migration_id_map`:
- Students com mesmo `externalRef` e `payloadHash` iguais sao skipados
- O flag `resume=true` (default) garante que re-execucoes nao criam duplicatas

## Troubleshooting

| Problema | Solucao |
|----------|---------|
| 401 na autenticacao | Verificar email/password e se o usuario tem as permissoes corretas |
| Tenant nao encontrado | Verificar `carolai.platform.tenant-id` e se o tenant esta provisionado |
| Email duplicado no platform | O platform exige email unico por tenant. Customers sem email recebem placeholder |
| Flyway migration nao roda | Verificar se `spring.flyway.enabled=true` e `spring.flyway.baseline-on-migrate=true` |
