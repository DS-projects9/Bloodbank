# Architecture & Per-Module Flow — MedKeen

Applies the **MDOREHS** pattern uniformly: **M**odule → **D**ata → **O**peration → **R**outing → **E**vent Bus → **H**andover → **S**tate Matrix.

- **State Matrix enum:** `IDLE | LOADING | SUCCESS | ERROR | TIME_LOCKED | EXPIRED | GOOGLE_AUTH | VERIFYING_TOKEN | ROLE_REDIRECT`
- **Roles:** `PATIENT`, `DOCTOR`, `BLOOD_BANK`, `HOSPITAL`, `ADMIN`
- **Auth:** Google OAuth is the **only** sign-in method.

---

## Module 1 — Patient (`PATIENT_ROLE`)

**Screens**
- **P1 Auth & Role Init** — Google OAuth button → token verify → role check/select → DPDP consent. → `IDLE|GOOGLE_AUTH|VERIFYING_TOKEN|ROLE_REDIRECT|ERROR_AUTH`
- **P2 Profile & Emergency Contacts** — profile pic, emergency contact cards (instant call), donor opt-in, vitals bar (blood group, allergies). Writes `/users/{uid}` + `/users/{uid}/contacts/{cid}`.
- **P3 Doctor Search & Time-Boxed Booking** — specialty search, doctor cards, slot picker, vault-doc attach checkboxes, "Book & Share Vault". Flow: select slot → select docs → `runTransaction` lock → mint time-boxed grant.
- **P4 Emergency Blood Search & Request** — 8-group grid, units counter, expanding radius indicator (5→50km), live result list, "Send Emergency Request & Share Contact". Writes `/blood_requests`.
- **P5 Health Vault** — upload/list/decrypt docs, share manager, instant revoke toggle.
- **P6 Appointments** — list (Upcoming/Past/Cancelled) + detail with Cancel/Reschedule + Share CTA.
- **P7 Donation/Opt-in** — donor toggle, eligibility checklist, request feed, audited contact reveal.
- **P8 Settings** — biometric lock, consent manager, audit view, logout, delete account.

**Handover payload (P3→D3):** `{ patientId, appointmentId, doctorId, grantedDocIds[], validFrom, validUntil }`

**Flow — Booking (atomic):**
```
[Select Slot] → [Select Vault Docs] → [runTransaction: slot OPEN→BOOKED] → [Create Appointment + sharedWith[]] → [Navigate P6]
```

---

## Module 2 — Doctor (`DOCTOR_ROLE`)

**Screens**
- **D1 Profile & Shift Config** — license badge, clinic picker, shift start/end, slot duration (15/30). Writes `/doctors/{uid}`. `UNVERIFIED|ACTIVE_EDIT|SAVING|SHIFTS_PUBLISHED`
- **D2 Slot Matrix Manager** — date strip, toggle chips (UNAVAILABLE/OPEN/BOOKED), Publish. Batch-writes `/doctors/{uid}/slots`.
- **D3 Appointment Queue & Time-Boxed Vault Viewer** — queue cards, attached records with countdown `14:59`, preview button. Reads `/appointments` + requests signed URL.

**Flow — Slot publish:** `[Select Dates] → [Toggle OPEN] → [Batch Write] → [Live in P3 search]`
**Flow — Vault view (gated):** `IsActive = (now ≥ validFrom) && (now ≤ validUntil)`; else `ACCESS_EXPIRED`.

---

## Module 3 — Blood Bank (`BLOOD_BANK_ROLE`)

**Screens**
- **B1 Live Inventory Dashboard** — 4×2 grid (A+,A-,B+,B-,AB+,AB-,O+,O-), +/- controls, low-stock alert (<5). Atomic write `/blood_inventory`.
- **B2 Incoming Request & Dispatch Logger** — request feed (name, phone, group, units, distance), Acknowledge/Mark Fulfilled. Unmasks contact on acknowledge; writes `auditReveals`.

**Flow:** `P4 write /blood_requests` → B2 `onSnapshot` alert → staff fulfill → deduct inventory → `FULFILLED`.

---

## Module 4 — Hospital (`HOSPITAL_ROLE`)
- **H1 Doctor Directory Mgmt** — list/verify affiliated doctors.
- **H2 Inpatient Blood Request** — internal ward request form → `/blood_requests` (hospital-scoped).
- **H3 Ward/Bed Allocation** — reserve blood units for OR.

---

## Module 5 — Admin (`ADMIN_ROLE`)
- **A1 Governance Dashboard** — metrics (users, appointments, units dispatched, active consents).
- **A2 Verification Console** — inspect license/accreditation, approve/reject → sets custom claim `verified`.
- **A3 Audit & DPDP Explorer** — filterable immutable `/audit_logs`.

---

## Cross-Module Event Bus
```
SLOT_BOOKED   ──> Doctor D3 queue updates (onSnapshot)
ACCESS_GRANTED─> Vault signed-URL timer starts
BLOOD_REQUESTED─> Blood Bank B2 alert + FCM to opt-in donors ≤10km
```
All cross-module sync via **Firestore snapshot listeners**; payloads serialized per Handover contracts above. No module polls directly.

## Routing
- Post-auth, `userRole` claim selects root stack: `PatientApp | DoctorApp | BloodBankApp | HospitalApp | AdminConsole`.
- Deep links: `medkeen://appointment/{id}`, `medkeen://doc/{docId}` resolve inside the correct stack.
