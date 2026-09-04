# MedKeen — Requirements Sheet (build-ready)

> Build reference for every feature. Complements `TECHSTACK.md`, `ARCHITECTURE.md`, `SECURITY.md`.
> **Auth = Google OAuth ONLY. No phone/OTP, no email/password, no anonymous.**
> **No hardcoded values rule** applies everywhere (see §0).

## 0. Global Rules (binding)
- Constant values (windows, radius, DPDP version) come from `app/src/constants.ts` / `functions/src/config.ts`. Never inline.
- Secrets never in source. Firebase project/bucket/region via env/config.
- Every write to a protected path also writes an immutable `/audit_logs` entry (SECURITY §7).
- Roles (custom claim `role`): `PATIENT | DOCTOR | BLOOD_BANK | HOSPITAL | ADMIN`. `verified` claim for DOCTOR/BLOOD_BANK.
- Data layer is canonical for field names/types (below); UI and functions must match exactly.

## 1. Auth & Onboarding (Module A)
- **A1 Sign-in:** single "Continue with Google" → `signInWithPopup(GoogleAuthProvider)` → on success upsert `/users/{uid}` if absent (`role: null`).
- **A2 Role select:** new users must pick one of 5 roles (can change role independently from auth).
- **A3 Onboarding gate (blocking):** user must complete profile first name + accept mandatory DPDP consents (`dpdpConsents` all `true` for `store_records`, `share_with_doctor`, `ai_processing`, `blood_network`, `emergency_contact`) bound to `DPDP_VERSION`. Until done, app shows only the gate.
- **A4 Role claim:** after onboarding, call Function `setRoleClaim({role})` → sets custom claims `role`. App re-reads `getIdTokenResult(true)`.
- AC: No screen beyond auth/gate is reachable before role claim + consent complete.

## 2. Patient Module (P)
- **P2 Profile:** read/write own `/users/{uid}.profile` (name, bloodGroup, allergies[], conditions[], meds[], emergencyContacts[] inline). Donor opt-in toggle → `/donors/{uid}.available`.
- **P3 Booking:** search `/doctors` (verified=true) by name/specialty; open `/doctors/{uid}/slots` where `status==OPEN`; booking via Function `bookAppointment` (transaction: slot OPEN→BOOKED, create `/appointments/{id}`, optional `grants[]`).
- **P4 Blood request:** submit `/blood_requests` (bloodGroup, units, location) → appears live on B2; contact hidden until acknowledged (audited reveal).
- **P5 Vault:** upload doc → AES-256-GCM encrypt client-side → upload ciphertext to `vault/{uid}/{docId}` → metadata `/health_vault/{docId}` (encrypted `storagePath`, `iv`, type, mime). Share/revoke via `sharedWith[]` (see §5).
- **P6 Appointments:** list own appointments (filter status), cancel (Function resets slot to OPEN), detail shows shared-grants + revoke.
- **P7 Donation:** eligibility auto-check (age/weight), donor registry opt-in (already P2), community request feed read.
- **P8 Settings:** biometric lock toggle (SecureStore), consent manager (re-read/revoke per purpose via `/users/{uid}.dpdpConsents`), audit trail viewer (own entries), delete account (cascade).

## 3. Doctor Module (D)
- **D1 Profile/Shifts:** `/doctors/{uid}` (specialty, licenseNumber, clinics[], scheduleMatrix[]) only writable by owner. `verified` claim required to appear in P3 search.
- **D2 Slots:** batch-write `/doctors/{uid}/slots` (start,end,status) for selected dates; generate from `scheduleMatrix` template.
- **D3 Queue:** `onSnapshot` `/appointments` where doctorId==uid; gated document viewer calls `getSharedFile` returning ≤60s signed URL when `grant.status==ACTIVE && validFrom<=now<=validUntil`; show countdown; `ACCESS_EXPIRED` state after window.

## 4. Blood Bank Module (B)
- **B1 Inventory:** read/write `/blood_inventory` for owned bank (bloodGroup, unitsAvailable). Low-stock (<5) UI warning.
- **B2 Requests:** `onSnapshot` `/blood_requests`; Acknowledge unmask (write `auditReveals[]` + audit log), Fulfill decrements inventory + status `FULFILLED`.

## 5. Hospital Module (H)
- **H1 Directory:** read `/doctors` (verified) to manage affiliations (`/doctors/{uid}.hospitalIds`).
- **H2 Inpatient request:** write hospital-scoped `/blood_requests` (via `requesterHospitalId`).
- **H3 Allocation:** manage ward/bed fields on `/blood_banks/{uid}.reservations`.

## 6. Admin Module (A)
- **A1 Metrics:** read-only aggregates via Function `systemMetrics` (counts) — no client-side aggregation over all docs.
- **A2 Verification:** review `/verifications` queue → Function `verifyEntity({targetUid, decision})` sets `verified` claim + `verificationStatus`.
- **A3 Audit:** read `/audit_logs` (immutable, write-only rule) with client-side filters.

## 7. Time-Boxed Sharing (cross-cutting, SECURITY §5)
- Grant document shape on owner doc: `sharedWith: [{ doctorId, appointmentId, validFrom, validUntil, status: 'ACTIVE'|'REVOKED' }]`.
- Auto-expire: `validUntil = appointment.end + SHARE_GRACE_MS`.
- Revocation = set `status: 'REVOKED'` → subsequent `getSharedFile` calls reject instantly.

## 8. AI (deferred to Phase 8; gates only)
- `ai_processing` consent required before any Gemini call (`aiProxy`, report summarizer). 112 guardrail middleware blocks emergency prompts.

## 9. Firestore collection contract
| Collection | Key fields |
|---|---|
| `/users/{uid}` | role, status, dpdpVersion, dpdpConsents{...}, profile{name,bloodGroup,allergies,conditions,meds,emergencyContacts}, fcmToken |
| `/doctors/{uid}` | specialty, licenseNumber, verificationStatus, clinics[], scheduleMatrix[], hospitalIds[], rating |
| `/doctors/{uid}/slots/{slotId}` | start, end, status(OPEN\|LOCKED\|BOOKED\|BLOCKED), lockExpiresAt?, appointmentId? |
| `/appointments/{appointmentId}` | patientId, doctorId, clinicId, slot{start,end}, status(PENDING_LOCK\|BOOKED\|IN_CONSULTATION\|COMPLETED\|CANCELLED\|NO_SHOW), grants[] |
| `/health_vault/{docId}` | patientId, documentType, name, storagePath, encryptionIv, mime, size, createdAt, sharedWith[] |
| `/blood_inventory/{id}` | bloodBankId, bloodGroup, unitsAvailable, lastUpdatedAt |
| `/blood_requests/{id}` | requesterPatientId, requesterHospitalId?, bloodGroup, unitsRequired, status(SEARCHING\|NOTIFIED\|FULFILLED\|EXPIRED), location, auditReveals[] |
| `/donors/{uid}` | bloodGroup, available, lastDonationAt |
| `/verifications/{id}` | targetUid, targetRole, documentsRefs[], decision, adminUid |
| `/audit_logs/{id}` | timestamp, actorId, action, resourceId, payloadHash |

## 10. Acceptance gate (every screen)
1. Uses components from `app/src/components` + tokens from `theme.ts`.
2. Writes/reads only through typed `app/src/lib/api/*` + rules (no inline `firestore.doc` strings in views).
3. Loading/empty/error states from State Matrix.
4. Associated backend + rules + audit tested before marking DONE in `TASKS.md`.