# E2E: Import Pipeline + Reconciliation + Training Generation

End-to-end guide for testing the full flow from extractor outbox to platform import, reconciliation, canonical training plans, and structured student detail.

## Prerequisites

- PostgreSQL running (or Docker Compose up)
- carolai-extractor running on `:8082`
- carolai-platform running on `:8080`
- carolai-platform-web running on `:3000` (optional, for UI verification)
- Extractor database already populated with migrated data (see `migration-ops.md`)

## Step 1 — Populate Export Outbox

Trigger the outbox population in the extractor. This transforms all extractor entities into canonical JSON payloads.

```bash
# Populate all entity types
curl -X POST http://localhost:8082/internal/exports/populate | jq

# Or populate a specific type
curl -X POST "http://localhost:8082/internal/exports/populate?type=STUDENT" | jq
curl -X POST "http://localhost:8082/internal/exports/populate?type=EXERCISE" | jq
```

Expected response:
```json
[
  { "entityType": "STUDENT", "total": 562, "inserted": 562, "updated": 0, "unchanged": 0 },
  { "entityType": "EXERCISE", "total": 420, "inserted": 420, "updated": 0, "unchanged": 0 },
  { "entityType": "TRAINING_HISTORY", "total": 1204, "inserted": 1204, "updated": 0, "unchanged": 0 },
  { "entityType": "PHYSICAL_ASSESSMENT", "total": 89, "inserted": 89, "updated": 0, "unchanged": 0 },
  { "entityType": "OBJECTIVE", "total": 340, "inserted": 340, "updated": 0, "unchanged": 0 }
]
```

## Step 2 — Verify Outbox Stats

```bash
curl http://localhost:8082/internal/exports/stats | jq
```

Check that `totalPending` matches expected counts.

## Step 3 — Run Import from Platform

### Via UI (recommended)

1. Open `http://localhost:3000/app/imports`
2. Click "Executar Importacao"
3. Watch the auto-refresh (3s interval) while status is RUNNING
4. After completion, check summary cards: Total, Criados, Ignorados, Falhas, Revisao

### Via API

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: <TENANT_UUID>" \
  -d '{"email":"admin@studio.com","password":"password","tenantId":"<TENANT_UUID>"}' \
  | jq -r '.tokens.accessToken')

# Run import
curl -X POST http://localhost:8080/api/v1/imports/run \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: <TENANT_UUID>" | jq
```

Expected response:
```json
{
  "id": "uuid",
  "status": "COMPLETED",
  "total": 2615,
  "imported": 2600,
  "skipped": 10,
  "failed": 0,
  "needsReview": 5,
  "startedAt": "...",
  "finishedAt": "..."
}
```

### What happens during import

| Entity Type | Import Strategy | Result |
|-------------|----------------|--------|
| STUDENT | Multi-criteria reconciliation (email > phone > name+birthDate) | CREATED_NEW, MATCHED_EXISTING, or NEEDS_REVIEW |
| EXERCISE | Skip-existing by name (never overwrite Borg/RPE) | Created or skipped |
| TRAINING_HISTORY | Canonical TrainingPlan/Session/Exercise entities | Full structured plan |
| PHYSICAL_ASSESSMENT | Archive (raw JSON) | ArchiveRecord |
| OBJECTIVE | Archive (raw JSON) | ArchiveRecord |

## Step 4 — Verify Students in Platform

```bash
curl http://localhost:8080/api/v1/students?size=5 \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: <TENANT_UUID>" | jq '.content | length'
```

Should show imported students.

## Step 5 — Verify Exercises (real, not seeded)

```bash
curl "http://localhost:8080/api/v1/exercises?size=5" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: <TENANT_UUID>" | jq '.content[] | {name, borgMin, borgMax}'
```

Imported exercises should have null Borg/RPE (only exercises configured in the platform UI have intensity data).

## Step 6 — Verify Training Plans (canonical)

```bash
STUDENT_ID="<student-uuid>"

curl "http://localhost:8080/api/v1/students/$STUDENT_ID/trainings?size=5" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: <TENANT_UUID>" | jq '.content[] | {goal, level, sessions: (.sessions | length)}'
```

Imported training plans have:
- `goal`: CONDICIONAMENTO (default for imports)
- `level`: INTERMEDIARIO (default)
- `coachId`: null (imported, not generated)
- Sessions with exercises (sets=0, reps="0", restSeconds=0 for missing data)

## Step 7 — Verify Archive (assessments + objectives only)

```bash
# Physical assessments
curl "http://localhost:8080/api/v1/students/$STUDENT_ID/archive?category=PHYSICAL_ASSESSMENT&size=10" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: <TENANT_UUID>" | jq '.content | length'

# Objectives
curl "http://localhost:8080/api/v1/students/$STUDENT_ID/archive?category=OBJECTIVE&size=10" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: <TENANT_UUID>" | jq '.content | length'
```

Note: TRAINING_HISTORY is no longer archived — it goes to canonical TrainingPlan entities (Step 6).

## Step 8 — Check Reconciliation Issues

```bash
# List open issues
curl "http://localhost:8080/api/v1/reconciliation/issues?status=OPEN" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: <TENANT_UUID>" | jq '.content | length'

# Get count
curl "http://localhost:8080/api/v1/reconciliation/issues/count" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: <TENANT_UUID>" | jq
```

### Via UI

1. Open `http://localhost:3000/app/reconciliation`
2. Filter by status (default: OPEN)
3. Click an issue to see incoming data and candidate students

## Step 9 — Resolve a Reconciliation Issue

```bash
ISSUE_ID="<issue-uuid>"

# Option A: Use existing student
curl -X POST "http://localhost:8080/api/v1/reconciliation/issues/$ISSUE_ID/resolve" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: <TENANT_UUID>" \
  -H "Content-Type: application/json" \
  -d '{"action":"USE_EXISTING","chosenStudentId":"<student-uuid>"}' | jq

# Option B: Create new student
curl -X POST "http://localhost:8080/api/v1/reconciliation/issues/$ISSUE_ID/resolve" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: <TENANT_UUID>" \
  -H "Content-Type: application/json" \
  -d '{"action":"CREATE_NEW"}' | jq
```

### Via UI

1. Open an issue at `http://localhost:3000/app/reconciliation/<id>`
2. Select a candidate student and click "Usar existente" OR click "Criar novo"
3. Verify success alert and issue status changes to RESOLVED

## Step 10 — Verify Student Detail (structured tabs)

### Via UI

1. Open `http://localhost:3000/app/students/<id>`
2. Check tabs:
   - **Dados** — student info (name, email, phone, birth date)
   - **Planos de Treino** — expandable training plans with sessions/exercises table
   - **Avaliacoes** — structured physical assessment cards with key-value pairs
   - **Objetivos** — objective badges (selected=green, unselected=gray)
   - **Conta** — account management (if user has STUDENT_ACCOUNT_MANAGE permission)

## Step 11 — Verify Outbox Acks

```bash
curl http://localhost:8082/internal/exports/stats | jq
```

`totalPending` should be 0 (except for NEEDS_REVIEW items which are NOT acked).
`totalAcked` should match the import totals minus needsReview.

## Step 12 — Re-run Population (Idempotency)

```bash
curl -X POST http://localhost:8082/internal/exports/populate | jq
```

All items should show `"unchanged"` counts since no data changed.

## Step 13 — Generate Training with RPE

```bash
curl -X POST http://localhost:8080/api/v1/trainings/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: <TENANT_UUID>" \
  -H "Content-Type: application/json" \
  -d '{"studentId":"<STUDENT_ID>","goal":"HIPERTROFIA","level":"INTERMEDIARIO","daysPerWeek":4,"sessionDurationMinutes":60}' | jq
```

Verify the generated plan includes exercises with RPE/Borg intensity data.

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| Import returns 0 items | Outbox not populated | Run step 1 first |
| Students created but no archive | Student emails don't match | Check extractor emails match platform |
| Import FAILED status | ExtractorClient connection error | Verify extractor is running on correct port |
| Ack fails after import | Extractor down during ack phase | Re-run import, already-acked items will be skipped |
| Training plans empty | TRAINING_HISTORY items still ACKED from previous import | Reset status: `UPDATE export_outbox SET status='PENDING', platform_id=NULL WHERE entity_type='TRAINING_HISTORY'` |
| Exercises have no Borg/RPE | Expected: imported exercises don't have intensity | Configure Borg/RPE in platform UI for specific exercises |
| needsReview > 0 | Ambiguous student matches (multiple name matches) | Resolve via reconciliation UI |
| Re-import skips exercises | Exercises with same name already exist | Expected behavior: skip-existing prevents overwriting Borg/RPE |
