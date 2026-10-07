# Dental Clinic Project Context

This document is the working map for future feature work. It describes what the project does today, how the pieces connect, and the places that need special attention before extending the app.

## High-Level Shape

The repository contains a dental clinic management app with:

- `dental_clinic_backend`: Spring Boot 4.1, Java 21, PostgreSQL, JPA, Flyway.
- `dental-clinic-frontend`: Angular 20 standalone-components app with SSR configuration.
- `docker-compose.yml`: local PostgreSQL 16 service.
- `database/`: older standalone SQL schema files.
- `graphify-out/`, backend `target/`, frontend `dist/`, `node_modules/`: generated/build artifacts, not primary source.

Primary domain areas already present:

- Login with two roles: `doctor` and `secretaire`.
- Patient records.
- Appointment scheduling.
- Doctor treatment management: treatment plans, procedures by tooth, history/timeline, photos, prescriptions, procedure catalog.
- Expenses and doctor financial insight.
- Several dashboard/material/payment/message views still backed by mock frontend data.

## Local Runtime

Codex workflow rule:

- Do not start frontend or backend dev servers unless the user explicitly asks for running servers. Implement, verify with builds/tests when possible, report results, and leave the servers stopped.

Database:

```bash
docker compose up -d postgres
```

Backend:

```bash
cd dental_clinic_backend
./mvnw spring-boot:run
```

Frontend:

```bash
cd dental-clinic-frontend
npm start
```

Default ports:

- PostgreSQL: `localhost:5432`
- Spring API: `localhost:8080`
- Angular dev server: usually `localhost:4200`

Frontend API proxy:

- `dental-clinic-frontend/proxy.conf.json` forwards `/api` to `http://localhost:8080`.

Database config:

- `application.properties` defaults to `jdbc:postgresql://localhost:5432/wajih`.
- `.env` contains `DB_NAME=wajih`, `DB_USER=postgres`, `DB_PASSWORD=123456`.

## Backend Architecture

Entry point:

- `com.example.demo.DemoApplication`

Layers:

- `controller`: REST endpoints and request/response classes.
- `service`: patient service plus service interfaces.
- `service.impl`: appointment, expense, and treatment business logic.
- `entity`: JPA entities and enums.
- `repository`: Spring Data repositories.
- `dto`: API DTO records for appointments, expenses, treatments, procedures, history, photos, prescriptions, catalog, doctor insights.
- `mapper`: maps entities to DTO responses for expense and treatment flows.
- `exception` and `controller.GlobalExceptionHandler`: centralized API errors.

Important backend conventions:

- JPA schema generation is disabled: `spring.jpa.hibernate.ddl-auto=none`.
- Flyway owns schema evolution from `src/main/resources/db/migration`.
- Controllers use explicit `@CrossOrigin` localhost lists, but Angular also has a proxy for dev.
- Most APIs return raw DTO objects/lists, not a wrapped `ApiResponse`.

## Backend APIs

Auth:

- `POST /api/login`
- Body: `{ "username": "...", "password": "..." }`
- Login is backed by the `login` database table.
- V9 seeds the existing users `doctor` and `secretaire` with password `drwajih`, and the schema supports many doctors and many secretaries.
- Returns `{ userId, username, fullName, role, message }`.
- `GET /api/users` returns active users for internal messaging.

Patients:

- `GET /api/patients`
- `GET /api/patients/{id}`
- `POST /api/patients`
- `PUT /api/patients/{id}`
- `DELETE /api/patients/{id}`

Patient fields include patient number, names, gender, birth date, address, phone, email, blood type, allergies, selected/catalog or custom treatment, expected and paid amounts, CNAM details, visit dates, next appointment, unpaid balance, notes, and last modified timestamp. The backend owns predefined treatment prices, derives unpaid balance, rejects overpayment, and records `lastVisit` on every create/update.

Appointments:

- `GET /api/appointments?q=&date=&status=`
- `GET /api/appointments/{id}`
- `POST /api/appointments`
- `PUT /api/appointments/{id}`
- `PATCH /api/appointments/{id}/cancel`

Appointment requests require `patientId`, `date`, `heure`, `durationMinutes`, and `providerName`. Optional fields include `treatmentId`, priority, status, and notes. If `treatmentId` is provided, the backend checks it belongs to the chosen patient.

Treatments:

- `GET /api/patients/{patientId}/treatments`
- `POST /api/patients/{patientId}/treatments`
- `GET /api/treatments/{id}`
- `PUT /api/treatments/{id}`
- `DELETE /api/treatments/{id}`

Treatment child resources:

- `GET /api/treatments/{id}/procedures`
- `POST /api/treatments/{id}/procedures`
- `PUT /api/procedures/{id}`
- `DELETE /api/procedures/{id}`
- `GET /api/treatments/{id}/timeline`
- `POST /api/treatments/{id}/history`
- `GET /api/treatments/{id}/photos`
- `POST /api/treatments/{id}/photos`
- `DELETE /api/photos/{id}`
- `GET /api/treatments/{id}/prescriptions`
- `POST /api/treatments/{id}/prescriptions`
- `PUT /api/prescriptions/{id}`
- `DELETE /api/prescriptions/{id}`
- `GET /api/procedure-catalog?q=`

Messages:

- `GET /api/users`
- `GET /api/messages/conversations?userId=`
- `POST /api/messages/conversations`
- `GET /api/messages/conversations/{conversationId}?userId=`
- `POST /api/messages/conversations/{conversationId}`
- `POST /api/messages/conversations/{conversationId}/read?userId=`
- `GET /api/messages/unread-count?userId=`

Messaging behavior:

- Conversations, participants, messages, and read state are stored in PostgreSQL.
- The messages UI is user-first, not conversation-first: it shows all active clinic users except the logged-in user, with no connected/online state and no conversation search.
- Doctors and secretaries can choose any active clinic user and send a direct message, including doctor-to-doctor, secretary-to-secretary, and doctor-to-secretary messages.
- Direct two-person conversations are reused by the backend when they already exist, so choosing the same person continues the same saved thread.
- Opening a conversation marks it read for the current user.
- Doctor and secretary layout notification badges load unread conversation counts from the messaging API.

Treatment behavior:

- Creating a treatment adds a system history item.
- Adding a completed procedure adds a system history item.
- Adding a photo or prescription adds a system history item.
- Adding/updating/deleting procedures recalculates clinical duration from the procedure list.
- Procedure progress is calculated from completed procedure count divided by total procedure count. Updating procedure status updates the treatment progress and moves the treatment to `IN_PROGRESS` or `COMPLETED` when appropriate.
- The expected bill and paid amount are controlled by the treatment form so the doctor's proposed bill is not overwritten by procedure edits.
- Doctor-created treatment plans sync the patient/client summary when they are the registration plan, and the first doctor-created plan becomes the patient registration plan if no registration plan exists yet.
- Procedure catalog can fill default procedure cost and duration.

Expenses:

- `GET /api/expenses?q=&status=`
- `POST /api/expenses`
- `PUT /api/expenses/{id}`
- `DELETE /api/expenses/{id}`
- `GET /api/doctor/insights/financial`

Current backend expense schema is a fixed recurring-bill tracker:

- `recordedAt`
- `category`: `WATER_BILL`, `ELECTRICITY_BILL`, `INTERNET_BILL`, `PATENTE_BILL`, `UNEXPECTED_MAINTENANCE`
- `billingPeriodMonths`
- `amount`
- `status`: `PENDING`, `PAID`, `NOT_RECEIVED`
- `description`

Financial insight calculates gross revenue and collected revenue from all treatments, monthly expense totals from the current calendar month, pending monthly expense totals, net income, unexpected maintenance count, stat cards, and category totals.

## Database Migrations

Migration files:

- `V1__create_patient_table.sql`
- `V2__create_treatment_tables.sql`
- `V3__create_expense_table.sql`
- `V4__create_appointment_table.sql`
- `V5__sync_patient_expense_and_login.sql`
- `V6__add_patient_treatment_billing_and_visit_tracking.sql`
- `V7__link_patient_registration_treatment_and_whole_mouth_procedures.sql`
- `V8__add_tooth_descriptions_to_treatment_procedures.sql`
- `V9__db_users_and_internal_messaging.sql`
- `V10__add_message_notifications.sql`

Important V5 note:

- V5 documents that the dev database was changed directly and the migration reconciles Flyway history with that live schema.
- V5 alters `patient`, drops and recreates `expense`, and creates `login`.
- `application.properties` sets `spring.flyway.baseline-version=5` with a description saying the existing dev DB already matches V5.

Important V6 note:

- Seeds the full patient-form treatment catalog with database-owned default prices.
- Adds `patient.selected_treatment_id`, `expected_amount`, and `paid_amount` with payment constraints.
- Changes appointment foreign keys so deleting a patient cascades appointments and safely clears treatment references.
- The Maven build uses `spring-boot-starter-flyway`; Spring Boot 4 requires this starter for Flyway auto-configuration.

Important V7 note:

- Applied successfully to the local `wajih` database; Flyway schema history is at version 7.
- Links the treatment chosen in the patient form to a real registration treatment plan and backfills existing patients.
- Adds `treatment_procedure.all_teeth` for one whole-mouth procedure without creating 32 duplicate rows.
- The patient registration plan synchronizes description, expected bill, paid amount, and remaining amount between the doctor treatment workspace and client cards.
- Patients retain one-to-many treatment plans; `registration_treatment_id` identifies only the plan created from registration.
- Procedure changes update clinical duration and tooth assignment but do not overwrite the doctor-controlled proposed bill or progress percentage.

Important V8 note:

- Applied successfully to the local `wajih` database; Flyway schema history is at version 8.
- Adds `treatment_procedure.tooth_description`, exposed through `ProcedureDto` and Angular `TreatmentProcedure`.
- The treatment page now stages selected teeth/procedure rows in the form. The doctor clicks `Save selected teeth` to keep selected tooth numbers and per-tooth descriptions in the form, then `Save all treatment data` persists the treatment plan plus all staged procedure rows.
- Prescriptions keep the visible labels `Medicine`, `Dosage`, `Duration`, and `Instructions`, but the UI now supports multiple medicines before saving. Each medicine becomes a persisted prescription row when the full treatment form is saved.
- Saved medicines load immediately when a patient treatment plan is opened. Photos are displayed with contained image sizing and an unavailable-image fallback for broken URLs.

Important V9 note:

- Applied successfully to the local `wajih` database; Flyway schema history is at version 9.
- Upgrades `login` into a real user table with `id`, `username`, `full_name`, `role`, `password`, active state, and timestamps.
- Adds `conversation`, `conversation_participant`, and `chat_message`.
- Existing login credentials still work: `doctor` / `drwajih` and `secretaire` / `drwajih`.
- Passwords are still stored in the legacy plaintext `password` column; replacing this with hashed passwords is an important future security cleanup.

Important V10 note:

- Applied successfully to the local `wajih` database; Flyway schema history is at version 10.
- Adds `message_notification`, one row per destination user for each sent chat message.
- Sending a message persists the `chat_message` row and unread notification rows for every recipient except the sender.
- Opening/reading a conversation marks destination notification rows as read.
- Doctor and secretary shells poll the messaging API every 5 seconds for unread notifications; the messages page polls every 4 seconds to refresh visible threads.

Schema cautions:

- Patient entity/request fields are aligned with V5 date, size, gender, and blood-type constraints as of V6 feature work.
- `appointment` Java class is lowercase, which works but is nonstandard and should be handled carefully when refactoring.

## Frontend Architecture

Angular app characteristics:

- Angular 20 standalone components.
- Uses `provideZonelessChangeDetection`.
- Uses `provideHttpClient(withFetch())`.
- Routes are lazy-loaded standalone components.
- SSR files exist: `main.server.ts`, `server.ts`, `app.routes.server.ts`.

Top-level routes:

- `/login`
- `/doctor/...`
- `/secretaire/...`
- Unknown routes redirect to `/secretaire/home`.

Role layouts:

- `src/app/doctor/doctor.ts`, `doctor.html`, `doctor.css`
- `src/app/secretaire/secretaire.ts`, `secretaire.html`, `secretaire.css`

Shared role behavior:

- Both layouts use the same secretary-era navigation components and mock data for notifications/search.
- Doctor routes reuse many secretary pages for clients, appointments, payments, expenses, calendar, messages, materials, and dashboard.

Frontend API services:

- `login/auth.service.ts`: `POST /api/login`, stores role, user id, username, and full name in `localStorage`.
- `secretaire/services/patient.service.ts`: patients CRUD except delete.
- `secretaire/services/appointment.service.ts`: appointments CRUD/cancel/search.
- `doctor/services/treatment.service.ts`: treatments, procedures, history, photos, prescriptions, catalog.
- `shared/services/expense.service.ts`: expenses CRUD and doctor financial insight.
- `shared/services/message.service.ts`: users, conversations, messages, send, mark read, and unread count.

## Frontend Feature Map

Mostly API-backed:

- Login.
- Secretary clients/patients page.
- Secretary/doctor appointments page.
- Doctor treatment management page.
- Doctor/secretary messages page.
- Doctor stats partially load from `/api/doctor/insights/financial`.

Mostly mock-data-backed:

- Home/dashboard summaries.
- Materials.
- Payments.
- Some doctor dashboard/search/sidebar content.

Fallback behavior:

- Several pages fall back to mock data if the backend is unavailable.
- Treatment management has a local mock seed if the API fails.
- Expenses page falls back to `clinicExpenses` mock data.

## Known Contract Mismatches To Fix Before Building On Them

Expense frontend/backend mismatch:

- Frontend `shared/models/expense.models.ts` still models the old ledger-style V3 expense table: `expenseDate`, `dueDate`, `label`, `supplier`, `invoiceNumber`, `owner`, `sourceRole`, `unexpected`, `taxDeductible`, etc.
- Backend V5 expects the simplified recurring-bill schema: `category`, `billingPeriodMonths`, `amount`, `status`, `description`.
- Frontend `ExpensesComponent` posts old fields and old status/category values, so create/update will not match the current backend DTO.
- Frontend `FinancialInsight` includes `scheduledExpenses`, but backend `FinancialInsight` does not return it.

Patient frontend/backend/schema mismatch:

- The client form converts optional date fields to omitted values before sending and uses date-only `nextAppointment`, matching V5.
- Phone is still required by the API/UI even though V5 allows null at database level.
- Gender is required and blood type uses the database-supported value list.

Auth/session limitations:

- There are no route guards enforcing role access yet.
- Auth is DB-backed, but frontend session state is still localStorage-only.
- Backend has no password hashing, tokens, or server-side sessions yet.

Testing limitations:

- Backend has only the default `DemoApplicationTests`.
- Frontend has generated specs, but no meaningful workflow tests visible.

## Guidance For Upcoming Features

When adding backend features:

- Start from migration/entity/DTO consistency. This project has drift between V5 and Java/Angular contracts.
- Add Flyway migrations for any schema change; do not rely on Hibernate DDL.
- Keep controller paths under `/api`.
- Prefer DTO records for new request/response contracts.
- Add service-layer validation when a child object must belong to a parent, like appointment/treatment patient matching.
- Update Angular TypeScript models in the same change as backend DTOs.

When adding frontend features:

- Use existing standalone component style.
- Keep API access in services, not directly in components.
- Preserve SSR/browser checks before using `localStorage`, `window`, or browser-only APIs.
- Check whether a page is API-backed or mock-backed before extending it.
- For doctor/secretary shared pages, confirm whether behavior should differ by role before changing the shared implementation.

Good first cleanup before large features:

- Align expense frontend models/components with V5 backend DTOs.
- Decide whether patient-level billing should eventually be consolidated into the richer treatment/payment tables; V6 intentionally stores a patient registry snapshot for the add/edit workflow.
- Replace plaintext login passwords with hashed passwords and add user-management screens.
- Add route guards for doctor/secretary areas.
- Add basic integration tests for patients, appointments, treatments, and expenses.

## Useful Files

Backend:

- `dental_clinic_backend/pom.xml`
- `dental_clinic_backend/src/main/resources/application.properties`
- `dental_clinic_backend/src/main/resources/db/migration/`
- `dental_clinic_backend/src/main/java/com/example/demo/controller/`
- `dental_clinic_backend/src/main/java/com/example/demo/service/`
- `dental_clinic_backend/src/main/java/com/example/demo/service/impl/`
- `dental_clinic_backend/src/main/java/com/example/demo/entity/`
- `dental_clinic_backend/src/main/java/com/example/demo/dto/`
- `dental_clinic_backend/src/main/java/com/example/demo/mapper/`
- `dental_clinic_backend/src/main/java/com/example/demo/repository/`

Frontend:

- `dental-clinic-frontend/package.json`
- `dental-clinic-frontend/angular.json`
- `dental-clinic-frontend/proxy.conf.json`
- `dental-clinic-frontend/src/app/app.routes.ts`
- `dental-clinic-frontend/src/app/login/`
- `dental-clinic-frontend/src/app/secretaire/`
- `dental-clinic-frontend/src/app/doctor/`
- `dental-clinic-frontend/src/app/shared/`

## Multi-Cabinet Architecture

The app serves many cabinets (clinics). A cabinet is a closed group of doctors and secretaries; the single platform `admin` (no cabinet) manages all cabinets and their members.

- **Schema (`V20__create_cabinets.sql`)**: table `cabinet`; `login.cabinet_id` (required for doctor/secretaire, NULL for admin, enforced by a check constraint); `cabinet_id NOT NULL` on `patient`, `appointment`, `expense`, `material_inventory`, `procedure_catalog`, `treatment`, `treatment_procedure`, `treatment_history`, `treatment_photo`, `prescription`, `staff_action`, `conversation`. Existing data was moved into the cabinet `MAIN`. `patient_number` and procedure `code` are unique per cabinet. Composite foreign keys (`(id, cabinet_id)`) stop rows from different cabinets being linked even by buggy code. `tooth` and `treatment_type` stay global reference data.
- **Isolation**: entities of those tables extend `CabinetOwned` (Hibernate `@TenantId`). `MessagingJwtFilter` stores the caller's cabinet in `CabinetContext` (cleared after the request); `CabinetTenantResolver` hands it to Hibernate, which adds it to every query/lookup and fills it on insert. Without a cabinet the resolver answers `0`, so tenant data is invisible and unwritable (fail closed). Native SQL bypasses this: `ExpenseRepository.restoreDeleted` sets `cabinet_id` explicitly and `CabinetAdminService` uses `JdbcTemplate` for cross-cabinet statistics.
- **People**: `LoginUser.cabinetId`; doctor/secretary lookups (assigned doctor, appointment provider, treatment doctor, working hours) must be in the caller's cabinet; messaging is only allowed inside one cabinet; `/api/users` lists only the caller's cabinet; realtime notifiers target only the cabinet's users; presence is published on `/topic/presence/{cabinetId}` and a client can subscribe only to its own. Users of a disabled cabinet cannot log in and existing tokens stop working.
- **Admin API**: `/api/admin/cabinets` (list with stats, create — optionally copying a catalog —, update/disable, delete only when empty) and `/api/admin/users?cabinetId=` plus `cabinetId` on create/update. A user that already owns cabinet data cannot be moved to another cabinet. New doctors get default working hours.
- **Within a cabinet** the existing rules are unchanged (a doctor sees the patients assigned to them, a secretary sees all of the cabinet's patients).

### Cabinet manager and invitation links (`V21`)

- **Role `manager`**: exactly one active manager per cabinet (partial unique index `uq_login_active_cabinet_manager`). The admin must provide the manager when creating a cabinet (`POST /api/admin/cabinets` with `manager`), and can replace it (`PUT /api/admin/cabinets/{id}/manager`; the previous manager is deactivated). The manager works in `/manager` and can only create/edit/deactivate the **doctors and secretaries of their own cabinet** (`/api/manager/users`; the cabinet comes from the login). They cannot create managers/admins, see patient data or touch other cabinets.
- **Invitation links**: accounts are created without a known password (random unusable one). `InvitationService` issues a one-time link (`/invite/{token}`, 72 h, only the SHA-256 hash is stored, a new link invalidates older ones). The public page `/invite/:token` (`GET /api/invitations/{token}`, `POST .../accept`) lets the person choose their password. The same mechanism covers forgotten passwords: admin or manager just generates a new link (`POST .../invitation`).
