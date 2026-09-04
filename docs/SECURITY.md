# Security & DPDP Requirements — MedKeen

This document is the binding security/legal spec. Every implementation task must satisfy the relevant section or cite an explicit deviation approved by Admin.

## 1. DPDP Act (India) Alignment
- **Lawful purpose & consent:** Processing PHI requires **specific, informed, revocable** consent. `dpdpConsents` map on `/users/{uid}` stores per-purpose booleans (`store_records`, `share_with_doctor`, `ai_processing`, `blood_network`, `emergency_contact`).
- **Versioning:** Consent bound to `DPDP_VERSION` (from `constants.ts`). New legal version → forced re-consent gate before app unlock.
- **Data minimization:** Only fields needed for the active purpose are read; profile sub-collections gated by purpose.
- **Right to erasure:** "Delete Account" cascades delete `/users`, `/health_vault` docs + Storage objects + `/appointments` references; logged to audit.
- **Breach logging:** Any access anomaly writes to `/audit_logs` with `payloadHash`.

## 2. Authentication (no shortcuts)
- **Method (SOLE):** Google OAuth via `GoogleAuthProvider` (`signInWithPopup`/`signInWithRedirect`). No phone OTP, no email/password, no anonymous.
- **Providers linked** to one `uid` when a Google account already exists (account linking to avoid duplicate identities).
- **Custom claims:** `role` and `verified` set server-side only (Cloud Function / Admin SDK); **never** client-writable.
- **Admin MFA:** `ADMIN` requires MFA (TOTP/hardware) before claim grant.
- **Session:** short-lived ID token + refresh in `expo-secure-store`; biometric re-auth to unlock app.

## 3. Authorization & RBAC
- All access decisions enforced by **Firestore + Storage Security Rules** keyed on `request.auth.token` (claims) + resource ownership.
- Rules deny by default. No document readable cross-role without explicit rule.
- `audit_logs`: `allow create` only; `update`/`delete` denied for every principal (immutability).

## 4. Encryption
- **At rest (client):** Health docs AES-256-GCM encrypted on device before upload; random per-file key sealed in `expo-secure-store` (unlocked by biometrics). Storage holds only ciphertext + IV.
- **In transit:** TLS 1.2+ everywhere (Firebase default); no plaintext PHI in logs/URLs.
- **Key handling:** encryption keys never leave device except sealed in secure-store; never written to Firestore.

## 5. Time-Boxed Access & Instant Revocation
- Share grant = `sharedWith[]{doctorId, appointmentId, validFrom, validUntil, status}`.
- Access served **only** through Cloud Function `getSharedFile`, which re-evaluates `status==ACTIVE && validFrom ≤ now ≤ validUntil` on every call → mint ≤60s signed URL.
- **Revoke:** flip `status=REVOKED` → next call rejected immediately (no stale URL honored).
- Auto-expiry: `validUntil = appointment.end + SHARE_GRACE_MS` (15 min).

## 6. Double-Booking Prevention
- Booking wrapped in `runTransaction`: slot must be `OPEN`, else abort `SLOT_TAKEN`. `PENDING_LOCK` auto-reset after `LOCK_WINDOW_MS` (5 min) by scheduled function.

## 7. Audit & Forensics
- Immutable `/audit_logs`: auth events, consent grant/revoke, share grant/revoke/view, booking lock/confirm/cancel, blood reveal, admin verification.
- Each entry: `timestamp, actorId, action, resourceId, payloadHash`.

## 8. Secrets & "No Hardcoding" (enforced)
- Gemini key, FCM server key, service accounts → **Secret Manager**; injected at deploy.
- Firebase project id / bucket / region → `config` module from env; never literal in source.
- Lint fails build on detected secret patterns.

## 9. Threat Notes
- Storage rules cannot query Firestore → sharing gate **must** be a Function (not pure rules).
- Auth abuse: Google OAuth only; rate-limit role-claim requests in Function; verify token audience/issuer on all calls.
- AI PHI: Gemini calls gated behind `ai_processing` consent; consider de-identification before send (Phase 8).
- Geo precision: patient exact location used only for blood radius; not stored beyond request lifetime unless consented.
