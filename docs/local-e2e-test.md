# Local End-to-End Test

Passo a passo para validar o fluxo completo de migracao + geracao de treino com RPE.

## Pre-requisitos

- Docker rodando (PostgreSQL para extractor e platform)
- Java 21
- Maven

## Passo 1 — Subir o Platform

```bash
cd carolai-platform
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

O platform sobe na porta 8081.

## Passo 2 — Provisionar Tenant

```bash
# Login como super admin
curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "super@carolai.com",
    "password": "SuperAdmin123!"
  }' | jq .

# Salve o accessToken retornado
export TOKEN="<accessToken>"

# Criar tenant
curl -s -X POST http://localhost:8081/api/v1/admin/tenants \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Studio Carol Test",
    "slug": "studio-carol-test",
    "type": "FRANCHISE_UNIT",
    "ownerEmail": "admin@studiocarol.com",
    "ownerPassword": "Admin123!",
    "ownerName": "Carol Admin"
  }' | jq .

# Salve o tenantId retornado
export TENANT_ID="<tenantId>"
```

## Passo 3 — Configurar Extractor

Edite `carolai-extractor/src/main/resources/application.properties`:

```properties
carolai.platform.url=http://localhost:8081
carolai.platform.email=admin@studiocarol.com
carolai.platform.password=Admin123!
carolai.platform.tenant-id=<TENANT_ID>
```

Suba o extractor:

```bash
cd carolai-extractor
mvn spring-boot:run
```

O extractor sobe na porta 8080.

## Passo 4 — Migrar Students

```bash
# Dry-run primeiro
curl -s -X POST "http://localhost:8080/migration/run?type=STUDENT&dryRun=true" | jq .
# Esperado: {"runType":"STUDENT","status":"COMPLETED","dryRun":true,"migrated":N,...}

# Migracao real
curl -s -X POST "http://localhost:8080/migration/run?type=STUDENT" | jq .
# Esperado: {"status":"COMPLETED","migrated":N,"skipped":0,"failed":0}
```

## Passo 5 — Seed de Exercicios

```bash
curl -s -X POST "http://localhost:8080/migration/run?type=EXERCISE" | jq .
# Esperado: {"runType":"EXERCISE","status":"COMPLETED","migrated":N}
```

## Passo 6 — Definir Intensidade RPE

Login no tenant e defina RPE para 3+ exercicios:

```bash
# Login como admin do tenant
curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@studiocarol.com",
    "password": "Admin123!",
    "tenantId": "'$TENANT_ID'"
  }' | jq .

export TENANT_TOKEN="<accessToken>"

# Listar exercicios
curl -s http://localhost:8081/api/v1/exercises \
  -H "Authorization: Bearer $TENANT_TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" | jq '.content[:5]'

# Definir RPE para 3 exercicios (substitua IDs reais)
for EXERCISE_ID in "<id1>" "<id2>" "<id3>"; do
  curl -s -X PATCH "http://localhost:8081/api/v1/exercises/$EXERCISE_ID/intensity" \
    -H "Authorization: Bearer $TENANT_TOKEN" \
    -H "X-Tenant-Id: $TENANT_ID" \
    -H "Content-Type: application/json" \
    -d '{
      "scaleType": "RPE_0_10",
      "target": 7,
      "min": 6,
      "max": 8,
      "notes": "Intensidade moderada-alta"
    }' | jq '{name, intensityTarget}'
done
```

## Passo 7 — Gerar Treino com RPE

```bash
# Use o ID de um student migrado
curl -s http://localhost:8081/api/v1/students \
  -H "Authorization: Bearer $TENANT_TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" | jq '.content[0].id'

export STUDENT_ID="<studentId>"

# Gerar treino com targetIntensity
curl -s -X POST http://localhost:8081/api/v1/trainings/generate \
  -H "Authorization: Bearer $TENANT_TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "'$STUDENT_ID'",
    "goal": "HIPERTROFIA",
    "level": "INTERMEDIARIO",
    "daysPerWeek": 4,
    "sessionDurationMinutes": 60,
    "equipment": ["HALTER", "BARRA", "MAQUINA"],
    "targetIntensity": 7
  }' | jq '.sessions[0].exercises[] | {exerciseName, intensityRpe}'
```

## Passo 8 — Verificar Resultado

O treino gerado deve:
- Conter exercicios do catalogo seeded
- Exercicios com `intensityRpe` definido devem aparecer ordenados por proximidade ao `targetIntensity=7`
- Exercicios sem RPE aparecem depois dos que tem RPE definido

## Checklist Final

- [ ] Students migrados visiveis em `GET /students`
- [ ] Exercicios seeded visiveis em `GET /exercises`
- [ ] Intensidade RPE definida em 3+ exercicios
- [ ] Treino gerado com `targetIntensity` respeita ordenacao RPE
- [ ] Re-execucao da migracao de students skipa duplicatas
- [ ] `mvn compile` + `mvn test` no platform: green
- [ ] `mvn compile` + `mvn test -Dtest=StudentMigrationMapperTest` no extractor: green
