# Session handoff — 2026-09-09

## Session architecture update — 2026-09-10

- Angular now has one reactive `SessionService` containing the authenticated user, role, permissions, token, and authentication state.
- The cached browser user is a single `dental-clinic-session` JSON object; the JWT remains in the centralized `dental-clinic-token` key. Legacy fragmented user keys are removed.
- Application startup restores the cache, verifies it through authenticated `GET /api/auth/me`, and replaces it with current database values.
- `SessionLifecycleService` owns the sole 45-second `/api/auth/me` synchronization timer and the application-level STOMP lifecycle.
- Invalid or disabled sessions clear cache, notifications, and WebSocket subscriptions and return to `/login`.
- Doctor and secretary routes now have authenticated role guards. Login and empty role routes lead to the role-specific dashboard.
- WebSocket ownership was removed from layout and message components. Reconnect callbacks are guarded against stale clients, preventing old-user and duplicate subscriptions.
- Reusable signal-based toast notifications are mounted at the app root and receive private message and doctor staff-action events.
- `GET /api/dashboard/me` derives the user from `ClinicPrincipal`; Angular dashboard content and header are reactive and role-specific.
- V12 adds nullable `appointment.provider_user_id`, backfills matching active doctors, and indexes provider/date. Doctor dashboards now filter appointments by authenticated user ID, not a frontend ID or display-name guess.
- Message responses now include recipient ID/name and read state. Private delivery remains `/user/queue/messages`.
- Latest verification: Angular production plus SSR build passed; Spring test suite passed (16 tests); Flyway schema is at V12. The existing Google Fonts CSS budget warning remains.
- Multi-browser login switching and live message delivery still require manual browser verification; development servers were not started for this implementation.

## Secretary audit and safe undo update - 2026-09-10

- V13 expands `staff_action.action_type`, adds explicit `status` and `undoable` fields, migrates legacy action names, and adds an entity/time index.
- Patient, appointment, and expense writes now require an authenticated doctor or secretary at the backend controller boundary. The acting user always comes from `ClinicPrincipal`; Angular never supplies it.
- Patient deletion remains doctor-only and is rejected when `unpaidBalance > 0`. The project has no existing soft-delete convention, so the valid zero-balance path retains the existing hard delete.
- Secretary changes use explicit action types (`PATIENT_CREATED`, `PATIENT_UPDATED`, `PATIENT_BALANCE_UPDATED`, appointment and expense equivalents) with old/new JSON snapshots.
- Balance activity uses the secretary's database full name and a clear remaining-balance transition such as `Sawsen updated Ahmed's balance` / `300 DT -> 150 DT`.
- Staff activity is delivered after transaction commit over the existing STOMP client at `/user/queue/activity`; only authenticated doctors may subscribe.
- The doctor panel consumes the expanded audit DTO (`oldValue`, `newValue`, `timestamp`, `status`, `undoable`, `undoneBy`, `undoneAt`) and merges real-time create/undo events.
- Undo now pessimistically locks the audit row and business row, confirms the current business snapshot still equals the audited new state, restores the old state transactionally, and marks the audit `UNDONE` without deleting it.
- Stale undo returns `Cannot undo because this data was modified after the original action.`; repeat undo is rejected. Patient creation is recorded but deliberately not undoable because deleting it could remove later dependent clinical data.
- Latest verification: all 21 Spring tests pass; Angular production/SSR build passes. The existing Google Fonts CSS budget warning remains.

## Workspace

- Root: C:/Users/aram/OneDrive/Bureau/dentist web app
- Frontend: dental-clinic-frontend (Angular 20, zoneless, SSR)
- Backend: dental_clinic_backend (Spring Boot 4.1, Java 21, PostgreSQL, Flyway)
- Read PROJECT_CONTEXT.md for architecture. Its messaging descriptions predate this work.
- Do not start development servers unless the user explicitly requests it.
- Preserve existing changes; do not reset or delete working files.

## User requirements and implemented changes

- JWT identifies the sender; REST persists messages/history and authenticated STOMP delivers live events.
- All database users appear in Messages. Current user is labelled You and cannot select themselves.
- UserPresenceService tracks authenticated WebSocket sessions. Green circle indicates online; gray means offline.
- Multiple tabs count as separate sessions; disconnecting one does not mark the remaining sessions offline.
- Broker heartbeats detect dead connections. Client reconnects and refreshes users/conversations.
- Fixed secretary subscribing to doctor-only staff-actions channel, which interrupted messaging.
- Added Angular change detection updates for asynchronous messaging/notification callbacks.
- Added team search, online-first ordering, multiline composer, Enter/Shift+Enter, 4000-character limit.
- Preserved recipient drafts; protected against stale history responses and switching recipients during sends.
- Confirmed sends append without reloading the entire thread; failures preserve drafts.
- Serialized conversation creation with ordered user-row locks to prevent duplicate direct threads.
- Direct conversation titles use the other participant's name.
- Message notifications show previews/counts and link to the correct recipient using ?user=ID.
- Read notifications refresh after marking a visible conversation read. Hidden tabs do not automatically mark incoming messages read.
- Doctor-only patient deletion is enforced in API and UI; positive patient unpaid balance blocks deletion.
- Added V11 staff_action audit migration and doctor notification/undo panel.
- Secretary patient, appointment, and expense writes store before/after snapshots in the same transaction.
- Undo is doctor-only, newest pending action first. Deleted expense restoration preserves its original ID.

## Verification

- Latest backend run: 16 tests passed, including 3 presence tests (multiple sessions, repeated disconnect, unknown user).
- Latest Angular production build passed; existing Google Fonts CSS budget warning remains.
- Live two-browser messaging was NOT manually verified.
- Scheduler initially caused a Spring bean creation cycle; fixed with a static scheduler bean and qualified constructor injection; subsequent application-context test passed.

## Unresolved issue to investigate next

User reported Vite ws proxy ECONNREFUSED at 22:25.
Proxy forwards /api and /ws to http://localhost:8080; /ws has ws:true.
This indicates the proxy could not establish a TCP connection to its upstream.
Check whether Spring is running/listening on 8080 and inspect backend startup logs before changing code.
The diagnostic tool call was interrupted, so backend availability/address mismatch is NOT confirmed.
Backend logs: dental_clinic_backend/codex-spring.log and codex-spring.err.log.
Frontend logs: dental-clinic-frontend/codex-ng-4200.log and codex-ng-4200.err.log.

## Known limitations for future review

- Disabled database accounts are visible, but backend still rejects messaging to inactive accounts.
- Presence is in-memory for a single backend instance.
- Staff audit covers patient/appointment/expense mutations, not every possible interaction or chat.
- Staff undo does not yet protect against every later doctor edit or cross-resource dependency; do not claim universally safe exact restoration.
- Staff pending actions contribute to the notification badge until undone; separate persisted review/read status is not implemented.
- Existing mock payment/material/dashboard areas were not converted to backend persistence.

## GitHub handoff blocker

User asked to push changes to GitHub and stop for today.
- Workspace root is not a Git repository.
- Backend folder is not a Git repository.
- Frontend is a Git repository on master; HEAD 5ff4047 (initial commit).
- Frontend has NO configured remote (git remote -v returns nothing).
- GitHub CLI was not found on PATH; no GitHub connector is available in this session.
- No destination repository URL found in project configuration.
- No GitHub push has occurred. Obtain repository URL before selecting remote/repository layout.
- Do not commit .env, generated target/dist/node_modules, or runtime logs.
- Preserve frontend Git history when arranging a repository for both applications.

## Appointment scheduling and patient deletion update - 2026-09-16

- V14 adds `appointment.end_time`, range constraints, the provider/date/status/time index, and persisted weekly `doctor_working_hours`. Existing appointment rows are retained.
- Appointment writes require an active doctor `providerUserId`; provider display names are resolved from the locked database user and frontend text is ignored.
- Scheduling validation is centralized in `AppointmentServiceImpl`: valid same-day ranges, doctor hours, full-range overlap, and a minimum 30-minute gap. Cancelled rows alone do not reserve time.
- The selected doctor row is pessimistically locked before conflict lookup/save, preventing concurrent requests from accepting the same slot.
- Cancellation is locked, transactional, idempotent, audited for secretaries, and returns the updated record. Appointment undo uses compatible time snapshots and re-runs conflict validation.
- Added working-hours GET/PUT APIs. Secretary availability changes are snapshotted as `DOCTOR_AVAILABILITY_UPDATED` and marked non-undoable.
- Angular now loads active doctors from `/api/users`, uses start/end fields, renders a selected-day doctor-lane timeline with 30-minute drop slots and proportional heights, upserts successful writes, and leaves failed drops unchanged.
- The appointment screen includes persisted working-hours editing, a collapsible cancelled section, a database table, legacy unassigned/conflict warnings, and refreshes on entry plus repeated Rendez-vous clicks.
- Fixed zero-balance patient deletion: role/balance checks and pessimistic locking remain, while the final hard delete uses a direct repository operation so PostgreSQL cascades execute reliably. An authenticated doctor API test with a linked treatment now returns success.
- Verification: 30 Spring tests pass; 10 Angular tests pass; Angular production/SSR build passes. Live PostgreSQL API checks confirmed overlap and 15-minute-gap rejection, exact 30-minute acceptance, same-time appointments for different doctors, cancelled-slot reuse, and concurrent same-slot results of one `201` plus one `409`. Temporary verification rows were removed.
- The in-app browser connector was unavailable during final visual inspection. No replacement browser automation path was used; API, unit/component tests, and production rendering build were used instead.
