# Migration Coverage Matrix

Overview of all entity types, their source in the extractor, canonical payload schema, and target in the platform.

## Entity Types

| Entity Type | Source Table (Extractor) | Outbox Mapper | Canonical Payload | Platform Target | Status |
|---|---|---|---|---|---|
| STUDENT | `customers` | `StudentOutboxMapper` | `{fullName, email, phone, birthDate, gender}` | `students` (UPSERT) | Active |
| TRAINING_HISTORY | `customers` → `training_plans` → `trainings` → `training_series` → `training_exercises` | `TrainingHistoryOutboxMapper` | `{studentEmail, planName, startDate, endDate, active, sessions[...]}` | `data_archive_records` (READ-ONLY) | Active |
| PHYSICAL_ASSESSMENT | `physical_assessments` | `PhysicalAssessmentOutboxMapper` | `{studentEmail, documentKey, assessmentDate, answeredDate, data:{...}}` | `data_archive_records` (READ-ONLY) | Active |
| OBJECTIVE | `customers` → `customer_objectives` | `ObjectiveOutboxMapper` | `{studentEmail, objectives[{code, name, selected}], snapshotAt}` | `data_archive_records` (READ-ONLY) | Active |

## Canonical vs Archive

| Category | Behavior | Match Strategy | Platform Table |
|---|---|---|---|
| **Canonical** (Student) | UPSERT — creates or updates real platform entities | Email match per tenant | `students` + `student_tenants` |
| **Archive** (Training, Assessment, Objective) | INSERT — read-only historical records | Student email lookup → `student_id` | `data_archive_records` |

## Payload Schemas

### STUDENT
```json
{
  "fullName": "Maria Silva",
  "email": "maria@example.com",
  "phone": null,
  "birthDate": "1990-05-15",
  "gender": "FEMALE"
}
```

### TRAINING_HISTORY
```json
{
  "studentEmail": "maria@example.com",
  "planName": "Plano Hipertrofia",
  "startDate": "2024-01-15",
  "endDate": "2024-04-15",
  "active": false,
  "sessions": [
    {
      "dayIndex": 0,
      "title": "Treino A - Peito e Triceps",
      "notes": null,
      "exercises": [
        {
          "name": "Supino Reto",
          "groupName": "Peito",
          "notes": "4x12",
          "videoUrl": null
        }
      ]
    }
  ]
}
```

### PHYSICAL_ASSESSMENT
```json
{
  "studentEmail": "maria@example.com",
  "documentKey": "assessment-2024-01",
  "assessmentDate": "2024-01-20T10:00:00",
  "answeredDate": "2024-01-22T14:30:00",
  "data": { "...raw assessment fields..." }
}
```

### OBJECTIVE
```json
{
  "studentEmail": "maria@example.com",
  "objectives": [
    { "code": "EMAGRECER", "name": "Emagrecimento", "selected": true },
    { "code": "HIPERTROFIA", "name": "Hipertrofia", "selected": false }
  ],
  "snapshotAt": "2024-01-15T08:00:00"
}
```

## Flow Diagram

```
Extractor DB → Mappers → export_outbox (PENDING)
                              ↓
Platform pulls via GET /internal/exports/pending
                              ↓
            ┌─── STUDENT → UPSERT into students table
            │
Import ─────┼─── TRAINING_HISTORY → INSERT into data_archive_records
            │
            ├─── PHYSICAL_ASSESSMENT → INSERT into data_archive_records
            │
            └─── OBJECTIVE → INSERT into data_archive_records
                              ↓
Platform acks via POST /internal/exports/{id}/ack
                              ↓
export_outbox status → ACKED
```

## Source Key Convention

| Entity Type | Source Key Format | Example |
|---|---|---|
| STUDENT | `{customerExternalRef}` | `cust-123` |
| TRAINING_HISTORY | `{customerExternalRef}:plan:{planExternalRef}` | `cust-123:plan:plan-456` |
| PHYSICAL_ASSESSMENT | `{customerId}:assessment:{documentKey}` | `42:assessment:doc-2024-01` |
| OBJECTIVE | `{customerExternalRef}:objectives` | `cust-123:objectives` |
