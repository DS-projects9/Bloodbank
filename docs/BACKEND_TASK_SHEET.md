# MedKeen Backend — Complete Task Sheet

> No hardcoded data. No mock values. Every field on every screen must come from the API.

---

## 1. BUSINESS RULES (confirmed)

| Rule | Detail |
|------|--------|
| **Slot lock is ONLY for doctor-patient appointments** | Two-phase: PENDING_LOCK (5 min) → BOOKED |
| **Blood donation uses NO slot lock** | Donors book donation slots directly; no locking/expiry needed |
| **Fulfill via app auto-manages inventory** | When blood bank accepts request through app → auto-decrement inventory |
| **Fulfill via manual update** | Blood bank uses "Log/Update Inventory" screen → manual stock adjustment |
| **All mutations go through backend** | Client never writes directly to Firestore for sensitive collections |
| **Time-boxed vault sharing** | Doctor can only view patient documents during active appointment window |
| **DPDP compliance** | 5 consents required, version tracked, revocable from settings |
| **Emergency escalation** | Expands search radius (5→10→15→20 km) with FCM push to nearby banks |

---

## 2. FIRESTORE COLLECTIONS (data model)

### 2a. `/users/{uid}`

| Field | Type | Required By |
|-------|------|-------------|
| `uid` | String | All screens |
| `email` | String | Profile, auth |
| `displayName` | String | Profile, appointment display |
| `photoUrl` | String | Profile avatar |
| `phone` | String | **MISSING from model** — needed by PatientDashboard, DoctorRecord |
| `role` | Enum (PATIENT/DOCTOR/BLOOD_BANK/HOSPITAL/ADMIN) | Role routing |
| `isOnboarded` | Boolean | Auth flow gate |
| `fcmToken` | String | Push notifications |
| `createdAt` | Timestamp | Audit |
| `profile.firstName` | String | Profile |
| `profile.lastName` | String | Profile |
| `profile.bloodGroup` | String | Profile, emergency search |
| `profile.allergies` | List\<String\> | Profile |
| `profile.conditions` | List\<String\> | Profile |
| `profile.meds` | List\<String\> | Profile |
| `profile.dateOfBirth` | Timestamp | **MISSING** — needed for age display on DoctorRecord ("28 Yrs / Male") |
| `profile.gender` | String | **MISSING** — needed for DoctorRecord display |
| `dpdpConsents.storeRecords` | Boolean | ConsentGate |
| `dpdpConsents.shareWithDoctor` | Boolean | ConsentGate |
| `dpdpConsents.aiProcessing` | Boolean | ConsentGate, AI routes |
| `dpdpConsents.bloodNetwork` | Boolean | ConsentGate, donor opt-in |
| `dpdpConsents.emergencyContact` | Boolean | ConsentGate |
| `dpdpConsents.dpdpVersion` | String | ConsentGate (current: "1.0") |

### 2b. `/users/{uid}/contacts/{cid}` (subcollection)

| Field | Type | Required By |
|-------|------|-------------|
| `id` | String | PatientDashboard |
| `name` | String | PatientDashboard |
| `relationship` | String | PatientDashboard |
| `phone` | String | PatientDashboard, DoctorRecord |
| `isPrimary` | Boolean | PatientDashboard |

### 2c. `/doctors/{uid}`

| Field | Type | Required By |
|-------|------|-------------|
| `uid` | String | DoctorSearch, AppointmentDetails |
| `displayName` | String | DoctorSearch, AppointmentDetails |
| `photoUrl` | String | DoctorSearch, AppointmentDetails |
| `specialty` | String | DoctorSearch, Profile |
| `licenseNumber` | String | Profile |
| `qualifications` | String | **MISSING** — Profile ("MBBS, MD (Cardiology)") |
| `experienceYears` | Int | **MISSING** — Profile ("12+ Yrs") |
| `verificationStatus` | String | DoctorSearch filter |
| `rating` | Float | DoctorSearch, Profile |
| `consultCount` | Int | **MISSING** — Profile ("1,240+") |
| `isOnline` | Boolean | **MISSING** — Dashboard toggle |
| `clinics[].id` | String | Profile |
| `clinics[].name` | String | Profile, AppointmentDetails |
| `clinics[].address` | String | Profile |
| `clinics[].latitude` | Double | Geo search |
| `clinics[].longitude` | Double | Geo search |
| `clinics[].department` | String | **MISSING** — Profile ("Cardiology (OPD 3)") |
| `clinics[].roomNumber` | String | **MISSING** — Profile ("Cabin 302") |
| `clinics[].phone` | String | **MISSING** — Profile ("+91 98765 00112") |
| `clinics[].email` | String | **MISSING** — Profile ("s.smith@cityheart.org") |
| `hospitalIds` | List\<String\> | Hospital association |

### 2d. `/doctors/{doctorId}/slots/{slotId}` (subcollection)

| Field | Type | Required By |
|-------|------|-------------|
| `id` | String | DoctorSearch |
| `doctorId` | String | Slot booking |
| `start` | Timestamp | DoctorSearch, AppointmentQueue |
| `end` | Timestamp | DoctorSearch, AppointmentQueue |
| `status` | Enum (OPEN/LOCKED/BOOKED/BLOCKED/UNAVAILABLE) | DoctorSearch filter, slot publish |
| `lockExpiresAt` | Timestamp? | Auto-expire cron |
| `appointmentId` | String? | Slot→appointment link |

### 2e. `/appointments/{appointmentId}`

| Field | Type | Required By |
|-------|------|-------------|
| `id` | String | AppointmentDetails, DoctorRecord |
| `patientId` | String | Patient list, doctor queue |
| `patientName` | String | DoctorQueue |
| `doctorId` | String | Patient appointments |
| `doctorName` | String | AppointmentDetails |
| `clinicId` | String | AppointmentDetails |
| `clinicName` | String | AppointmentDetails |
| `slot.start` | Timestamp | AppointmentDetails, DoctorQueue time display |
| `slot.end` | Timestamp | DoctorRecord time range |
| `status` | Enum (PENDING_LOCK/BOOKED/IN_CONSULTATION/COMPLETED/CANCELLED/NO_SHOW) | All appointment screens |
| `grants[].doctorId` | String | Vault sharing |
| `grants[].appointmentId` | String | Vault sharing |
| `grants[].validFrom` | Timestamp | Vault access check |
| `grants[].validUntil` | Timestamp | DoctorRecord countdown timer, vault access |
| `grants[].status` | Enum (ACTIVE/REVOKED/EXPIRED) | Vault access check |
| `grants[].documentIds` | List\<String\> | DoctorRecord document list |
| `createdAt` | Timestamp | Appointment list ordering |

### 2f. `/health_vault/{docId}`

| Field | Type | Required By |
|-------|------|-------------|
| `id` | String | Vault list |
| `patientId` | String | Patient vault query |
| `documentType` | String | **MISSING from model** — DoctorRecord badge ("LAB REPORT", "SCAN") |
| `name` | String | Vault list, DoctorRecord filename |
| `storagePath` | String | Download URL generation |
| `encryptionIv` | String | End-to-end encryption |
| `mime` | String | File type |
| `size` | Int | File size display |
| `createdAt` | Timestamp | Vault list ordering |
| `sharedWith` | List\<String\> | Shared documents tracking |

### 2g. `/blood_inventory/{id}`

| Field | Type | Required By |
|-------|------|-------------|
| `id` | String | Inventory grid |
| `bloodBankId` | String | Query filter |
| `bloodGroup` | String | Inventory grid, stock details |
| `unitsAvailable` | Int | Inventory grid, stock details |
| `maxCapacity` | Int | **MISSING** — Inventory summary card (e.g., 200) |
| `storageTemp` | Double | **MISSING** — Stock details ("4.0° C") |
| `lastUpdatedAt` | Timestamp | Inventory "Last Updated" |
| `lastUpdatedBy` | String | **MISSING** — Inventory "by Admin" |

### 2h. `/blood_inventory/{id}/batches/{batchId}` (subcollection — NEW)

| Field | Type | Required By |
|-------|------|-------------|
| `id` | String | Stock details |
| `bloodBankId` | String | Query filter |
| `bloodGroup` | String | Stock details |
| `batchId` | String | Stock details ("#OB-2026-0815") |
| `collectionDate` | Timestamp | Stock details |
| `expiryDate` | Timestamp | Stock details, near-expiry filter |
| `volumePerUnit` | Int | Stock details (500 ml) |
| `storageTemp` | Double | Stock details |
| `vaultLocation` | String | Stock details ("Chiller B • Rack 03") |

### 2i. `/blood_requests/{requestId}`

| Field | Type | Required By |
|-------|------|-------------|
| `id` | String | Request list, fulfill |
| `requesterPatientId` | String | Patient ownership |
| `requesterPatientName` | String | Dashboard display |
| `requesterHospitalId` | String? | Hospital association |
| `hospitalName` | String | **MISSING** — Dashboard ("City General Hospital") |
| `bloodGroup` | String | Request display, inventory match |
| `unitsRequired` | Int | Request display |
| `severity` | String | **MISSING** — Dashboard ("CRITICAL"/"URGENT") |
| `status` | Enum (SEARCHING/NOTIFIED/ULFILLED/EXPIRED) | All blood screens |
| `location.latitude` | Double | Geo search |
| `location.longitude` | Double | Geo search |
| `distance` | Double | **MISSING** — computed at query time |
| `auditReveals` | List\<AuditReveal\> | Audit trail |
| `createdAt` | Timestamp | Time display ("8 mins ago") |

### 2j. `/donors/{uid}`

| Field | Type | Required By |
|-------|------|-------------|
| `uid` | String | Donor registration |
| `bloodGroup` | String | Donor profile |
| `available` | Boolean | Donor opt-in, dashboard |
| `lastDonationAt` | Timestamp? | Donation history, eligibility |

### 2k. `/donor_bookings/{bookingId}` (NEW — for blood donation slots)

| Field | Type | Required By |
|-------|------|-------------|
| `id` | String | Donations tab |
| `donorId` | String | Donor association |
| `donorName` | String | Donations tab display |
| `bloodGroup` | String | Donations tab display |
| `bloodBankId` | String | Blood bank query |
| `slotTime` | Timestamp | Donations tab ("Today • 02:15 PM") |
| `status` | Enum (CONFIRMED/CHECKED_IN/CANCELLED) | Donations tab |
| `lastDonationAt` | Timestamp? | Donor history ("6 Months ago") |

### 2l. `/donor_bookings/{id}/eligibility` (virtual — computed)

Computed from donor profile + medical history:

| Criterion | Rule | Required By |
|-----------|------|-------------|
| Age 18-65 | Computed from `dateOfBirth` | DonationDash2 |
| Weight > 50kg | New field or check | DonationDash2 |
| No recent illness | `lastDonationAt` gap check | DonationDash2 |
| Hemoglobin > 12.5 | Lab result or self-report | DonationDash2 |

### 2m. `/blood_banks/{uid}` (NEW — facility profile)

| Field | Type | Required By |
|-------|------|-------------|
| `uid` | String | BloodBankProfile |
| `name` | String | Dashboard header, Profile |
| `bloodBankCode` | String | Profile ("BB-LIC-2026-9901") |
| `address` | String | Profile |
| `phone` | String | Profile |
| `email` | String | Profile |
| `licenseNumber` | String | Profile |
| `accreditation` | String | Profile ("Govt. Licensed & NABL") |
| `facilityCategory` | String | Profile ("Major Component Unit") |
| `licenseExpiryDate` | Timestamp | Profile |
| `nodalOfficer.name` | String | Profile |
| `nodalOfficer.qualification` | String | Profile ("MD Pathology") |
| `location.latitude` | Double | Geo search |
| `location.longitude` | Double | Geo search |
| `location.city` | String | Profile display |
| `isOnline` | Boolean | Dashboard indicator |
| `emergencyDispatchEnabled` | Boolean | Profile toggle |
| `maxCapacity` | Int | Inventory summary |

### 2n. `/audit_logs/{logId}`

| Field | Type | Required By |
|-------|------|-------------|
| `id` | String | Audit trail |
| `timestamp` | Timestamp | Audit display |
| `actorId` | String | Audit display |
| `actorName` | String | **MISSING** — Audit display ("Staff ID #88") |
| `action` | String | Audit display |
| `resourceId` | String | Audit trail link |
| `resourceType` | String | Audit categorization |
| `payloadHash` | String | Integrity check |

### 2o. `/verifications/{id}`

| Field | Type | Required By |
|-------|------|-------------|
| `id` | String | Admin verification queue |
| `targetUid` | String | Verification target |
| `targetRole` | Enum | Verification target |
| `documentsRefs` | List\<String\> | Verification documents |
| `decision` | String | Verification status |
| `adminUid` | String | Admin who decided |
| `createdAt` | Timestamp | Queue ordering |

---

## 3. CROSS-MODULE INTERACTIONS

These are actions in one module that affect data in another module:

| Action | Module | Affects | Effect |
|--------|--------|---------|--------|
| Patient books appointment | Patient | Doctor | Slot status: OPEN → LOCKED → BOOKED |
| Patient shares documents | Patient | Doctor | `Appointment.grants[].documentIds` populated |
| Doctor completes consultation | Doctor | Patient | Appointment status → COMPLETED, grant validUntil set |
| Doctor views patient record | Doctor | Vault | `GET /vault/documents/:id/download` checks grant validity |
| Patient requests blood | Patient | BloodBank | `blood_requests` doc created, FCM sent to nearby banks |
| Blood bank fulfills request | BloodBank | Inventory | `blood_inventory.unitsAvailable` decremented (auto) |
| Blood bank manual update | BloodBank | Inventory | `blood_inventory.unitsAvailable` updated (manual log) |
| Blood bank accepts donation | BloodBank | Donors | `donor_bookings.status` → CHECKED_IN |
| Patient registers as donor | Patient | Donors | `donors/{uid}` doc created/updated |
| Patient books donation slot | Patient | BloodBank | `donor_bookings` doc created |
| Doctor publishes slots | Doctor | Patient | Patient sees available slots in DoctorSearch |
| Emergency escalation | Patient | BloodBank | Radius expands, FCM notifications sent |
| Role selection | Auth | All | Custom claim set, routing changes |
| Consent acceptance | Auth | Patient | `isOnboarded = true`, DPDP version tracked |

---

## 4. API ENDPOINTS (complete list)

### 4a. Auth Module

| # | Method | Path | Auth | Request Body | Response | Business Logic |
|---|--------|------|------|-------------|----------|----------------|
| A1 | POST | `/api/auth/setup-role` | Yes | `{ role: UserRole }` | `{ ok: true }` | Validate role enum, `setCustomUserClaims({role})`, update Firestore `users/{uid}.role` |
| A2 | POST | `/api/auth/setup-consents` | Yes | `{ consents: DpdpConsents, name: String }` | `{ ok: true }` | Validate name non-empty, validate all 5 consents true, update Firestore, set `isOnboarded = true` |
| A3 | GET | `/api/config/consents` | No | — | `ConsentConfig[]` | Return consent definitions (titles, descriptions, required flags, DPDP version) |
| A4 | GET | `/api/config/roles` | No | — | `RoleConfig[]` | Return available roles with names, descriptions, icons |

### 4b. User Module

| # | Method | Path | Auth | Request Body | Response | Business Logic |
|---|--------|------|------|-------------|----------|----------------|
| U1 | GET | `/api/users/me` | Yes | — | `User` | Read `users/{uid}`, return full profile |
| U2 | PUT | `/api/users/me` | Yes | Partial `UserProfile` | `User` | Update allowed fields (firstName, lastName, bloodGroup, allergies, conditions, meds, dateOfBirth, gender) |
| U3 | POST | `/api/users/fcm-token` | Yes | `{ token: String }` | `{ ok: true }` | Update `users/{uid}.fcmToken` |
| U4 | GET | `/api/users/me/contacts` | Yes | — | `List<EmergencyContact>` | Read `users/{uid}/contacts` subcollection |
| U5 | POST | `/api/users/me/contacts` | Yes | `EmergencyContact` | `EmergencyContact` | Create in subcollection, return with generated ID |
| U6 | PUT | `/api/users/me/contacts/{id}` | Yes | `EmergencyContact` | `EmergencyContact` | Update in subcollection |
| U7 | DELETE | `/api/users/me/contacts/{id}` | Yes | — | `{ ok: true }` | Delete from subcollection |

### 4c. Doctor Module

| # | Method | Path | Auth | Request Body | Response | Business Logic |
|---|--------|------|------|-------------|----------|----------------|
| D1 | GET | `/api/doctors` | Yes | Query: `?specialty=&lat=&lng=&radius=` | `List<Doctor>` | Query where `verificationStatus == "VERIFIED"`, optional specialty filter, optional geo-sort |
| D2 | GET | `/api/doctors/:id` | Yes | — | `Doctor` | Get single doctor with full profile + clinics |
| D3 | GET | `/api/doctors/:id/slots` | Yes | Query: `?date=` | `List<ScheduleSlot>` | Read `doctors/{id}/slots` subcollection, filter by date range, return OPEN slots only |
| D4 | GET | `/api/doctors/me/profile` | Yes (DOCTOR) | — | `Doctor` | Get own full profile |
| D5 | PUT | `/api/doctors/me/profile` | Yes (DOCTOR) | `DoctorProfileUpdate` | `Doctor` | Update own profile (specialty, qualifications, experienceYears, clinics) |
| D6 | PUT | `/api/doctors/me/availability` | Yes (DOCTOR) | `{ isOnline: Boolean }` | `{ ok: true }` | Update `doctors/{uid}.isOnline` |
| D7 | GET | `/api/doctors/me/schedule/config` | Yes (DOCTOR) | — | `ScheduleConfig` | Return saved shift config (start, end, interval, date range) |
| D8 | POST | `/api/doctors/me/slots/publish` | Yes (DOCTOR) | `SlotPublishRequest` | `{ slotCount: Int }` | **Transaction**: validate date range, generate slots with interval, write to `doctors/{uid}/slots` subcollection, set all to OPEN |

### 4d. Appointment Module

| # | Method | Path | Auth | Request Body | Response | Business Logic |
|---|--------|------|------|-------------|----------|----------------|
| AP1 | POST | `/api/appointments/lock` | Yes (PATIENT) | `{ doctorId, slotId }` | `Appointment` | **Transaction**: (1) verify slot OPEN, (2) set slot LOCKED + lockExpiresAt=now+5min, (3) create appointment PENDING_LOCK, (4) return appointment |
| AP2 | POST | `/api/appointments/:id/confirm` | Yes (PATIENT) | — | `Appointment` | **Transaction**: (1) verify PENDING_LOCK + not expired, (2) slot → BOOKED, (3) appointment → BOOKED, (4) create DocumentGrant with validUntil=slot.end+15min |
| AP3 | POST | `/api/appointments/:id/cancel` | Yes | — | `{ ok: true }` | Verify caller is patient or doctor. Slot → OPEN (clear appointmentId). Appointment → CANCELLED |
| AP4 | POST | `/api/appointments/:id/complete` | Yes (DOCTOR) | — | `Appointment` | Verify caller is the doctor. Appointment → COMPLETED. Optionally update grant validUntil |
| AP5 | GET | `/api/appointments` | Yes | Query: `?date=&status=` | `List<Appointment>` | If DOCTOR: query by doctorId. If PATIENT: query by patientId. Filter by date range and status |
| AP6 | GET | `/api/appointments/:id` | Yes | — | `Appointment` | Get single appointment, verify caller is patient or doctor |
| AP7 | GET | `/api/appointments/:id/patient-record` | Yes (DOCTOR) | — | `PatientRecordView` | Return patient profile + shared documents (from grants) + session timer. Verify caller is the doctor for this appointment |

### 4e. Blood Module

| # | Method | Path | Auth | Request Body | Response | Business Logic |
|---|--------|------|------|-------------|----------|----------------|
| B1 | GET | `/api/blood/inventory` | Yes | Query: `?bloodBankId=` | `List<BloodInventoryItem>` | Query blood_inventory by bloodBankId |
| B2 | GET | `/api/blood/inventory/summary` | Yes | Query: `?bloodBankId=` | `InventorySummary` | Compute totalUnits, maxCapacity, percentUsed, lastUpdated, lastUpdatedBy |
| B3 | PUT | `/api/blood/inventory/:id` | Yes (BLOOD_BANK) | `{ delta: Int, reason: String }` | `BloodInventoryItem` | **Transaction**: read current, add delta (min 0), update lastUpdatedAt + lastUpdatedBy. Write audit log |
| B4 | GET | `/api/blood/inventory/:id/batches` | Yes | — | `List<BloodBatch>` | Read batches subcollection for this inventory item |
| B5 | POST | `/api/blood/inventory/:id/batches` | Yes (BLOOD_BANK) | `BatchCreateRequest` | `BloodBatch` | Create new batch in subcollection. Write audit log |
| B6 | GET | `/api/blood/inventory/:id/audit` | Yes | — | `List<AuditEntry>` | Read audit entries for this blood group |
| B7 | POST | `/api/blood/request` | Yes (PATIENT) | `BloodRequestCreate` | `BloodRequest` | Create blood_requests doc with status=SEARCHING. Find nearby banks (GeoFirestore). Send FCM to each |
| B8 | GET | `/api/blood/requests` | Yes (BLOOD_BANK) | — | `List<BloodRequest>` | Query blood_requests where status in [SEARCHING, NOTIFIED], ordered by createdAt DESC. Join patient name |
| B9 | POST | `/api/blood/requests/:id/fulfill` | Yes (BLOOD_BANK) | — | `{ ok: true }` | **Transaction**: (1) request → FULFILLED, (2) find matching blood_inventory item, (3) decrement unitsAvailable, (4) write audit log. **This is the auto-manage flow** |
| B10 | POST | `/api/blood/requests/:id/decline` | Yes (BLOOD_BANK) | — | `{ ok: true }` | Request → EXPIRED. Write audit log |
| B11 | POST | `/api/blood/requests/:id/escalate` | Yes (PATIENT) | — | `{ ok: true }` | Expand search radius, re-notify banks with wider radius. Send FCM |
| B12 | GET | `/api/blood/nearby` | Yes | Query: `?lat=&lng=&radius=` | `List<BloodBankProfile>` | GeoFirestore query for nearby blood banks |
| B13 | POST | `/api/blood/donors/register` | Yes (PATIENT) | `{ bloodGroup, available }` | `Donor` | Create or update `donors/{uid}` doc |
| B14 | PUT | `/api/blood/donors/me` | Yes | `{ available: Boolean }` | `{ ok: true }` | Update own donor availability |
| B15 | POST | `/api/blood/donation-bookings` | Yes (PATIENT) | `{ bloodBankId, slotTime }` | `DonorBooking` | Create donor_booking doc. **No lock needed** — direct booking |
| B16 | GET | `/api/blood/donation-bookings` | Yes (BLOOD_BANK) | — | `List<DonorBooking>` | Query donor_bookings by bloodBankId, ordered by slotTime |
| B17 | POST | `/api/blood/donation-bookings/:id/checkin` | Yes (BLOOD_BANK) | — | `{ ok: true }` | Booking → CHECKED_IN. Update donor lastDonationAt |
| B18 | POST | `/api/blood/donation-bookings/:id/cancel` | Yes (BLOOD_BANK) | — | `{ ok: true }` | Booking → CANCELLED |
| B19 | GET | `/api/blood/donations` | Yes (BLOOD_BANK) | — | `List<DonationRecord>` | Query completed donations for this bank |
| B20 | GET | `/api/blood/donor-eligibility/:donorId` | Yes | — | `EligibilityResult` | Compute eligibility from donor profile + medical checks |
| B21 | GET | `/api/blood/stats` | Yes (BLOOD_BANK) | — | `BloodBankStats` | Compute totalDonors, activeToday, unitsCollected |

### 4f. Vault Module

| # | Method | Path | Auth | Request Body | Response | Business Logic |
|---|--------|------|------|-------------|----------|----------------|
| V1 | GET | `/api/vault/documents` | Yes (PATIENT) | — | `List<HealthVaultDoc>` | Query health_vault where patientId=currentUser |
| V2 | POST | `/api/vault/documents` | Yes (PATIENT) | `{ name, documentType, mime, size }` | `{ uploadUrl, docId }` | Create Firestore doc, generate signed upload URL (Storage), return both |
| V3 | GET | `/api/vault/documents/:id/download` | Yes | — | `{ downloadUrl }` | **Time-boxed access**: If owner → signed URL. If doctor → check grants, verify validUntil > now, return signed URL. Else 403 |
| V4 | DELETE | `/api/vault/documents/:id` | Yes (PATIENT) | — | `{ ok: true }` | Delete Firestore doc + Storage file |

### 4g. AI Module

| # | Method | Path | Auth | Request Body | Response | Business Logic |
|---|--------|------|------|-------------|----------|----------------|
| AI1 | POST | `/api/ai/analyze` | Yes (PATIENT) | `{ documentId, prompt? }` | `AiAnalysisResult` | Verify ownership, check `aiProcessing` consent, download doc, call OpenAI/Gemini, store summary, return |
| AI2 | POST | `/api/ai/chat` | Yes (PATIENT) | `{ threadId?, message }` | `{ response, threadId }` | Create or continue thread, send message, store both, return |

### 4h. Blood Bank Profile Module

| # | Method | Path | Auth | Request Body | Response | Business Logic |
|---|--------|------|------|-------------|----------|----------------|
| BB1 | GET | `/api/blood-banks/me` | Yes (BLOOD_BANK) | — | `BloodBankProfile` | Get own facility profile |
| BB2 | PUT | `/api/blood-banks/me` | Yes (BLOOD_BANK) | `BloodBankProfileUpdate` | `BloodBankProfile` | Update facility profile |
| BB3 | PUT | `/api/blood-banks/me/settings` | Yes (BLOOD_BANK) | `{ emergencyDispatchEnabled: Boolean }` | `{ ok: true }` | Toggle emergency dispatch |

### 4i. System / Audit

| # | Method | Path | Auth | Request Body | Response | Business Logic |
|---|--------|------|------|-------------|----------|----------------|
| S1 | GET | `/api/audit/logs` | Yes (ADMIN) | Query: `?resourceId=&action=` | `List<AuditLog>` | Query audit_logs with filters |
| S2 | GET | `/api/system/metrics` | Yes (ADMIN) | — | `SystemMetrics` | Aggregated counts (users, appointments, units) |

---

## 5. PER-SCREEN DATA REQUIREMENTS

### 5a. Patient Module

| Screen | Data Fields Needed | API Endpoint(s) |
|--------|-------------------|-----------------|
| **PatientDashboard** | emergencyContacts[] (name, relationship, phone), donorOptIn status | U4, J14 |
| **PatientProfile** | firstName, lastName, bloodGroup, allergies[], conditions[], meds[], emergencyContacts[] | U1, U4 |
| **DoctorSearch** | doctorList[] (name, specialty, clinic, distance, rating), availableSlots[] (date, time) | D1, D3 |
| **AppointmentDetails** | doctorName, doctorSpecialty, clinicName, slotTime, status, sharedDocuments[] (name, type) | AP6, V3 |
| **AIHealthAssistant** | quickActions[], chatHistory[], aiResponse (hemoglobin, platelet, message) | AI2, AI1 |
| **EmergencyEscalation** | requestStatus, elapsed time, escalationLevel, nearestBanks[] | B11, B12 |
| **RegistrationSlotBooking** | donorName, phone, bloodGroup, bloodBankList[] (name, distance), timeSlots[] | B15, B12 |
| **PatientAppointments** | appointments[] (doctorName, clinicName, slotTime, status) | AP5 |

### 5b. Doctor Module

| Screen | Data Fields Needed | API Endpoint(s) |
|--------|-------------------|-----------------|
| **DoctorDashboard** | isOnline, clinicName, scheduleConfig (start, end, interval), generatedSlots[] | D6, D7, D4 |
| **DoctorProfile** | displayName, specialty, qualifications, licenseNumber, experienceYears, rating, consultCount, clinic (name, department, room, phone, email) | D4 |
| **AppointmentQueue** | todayAppointments[] (patientName, time, age, gender, bloodGroup, hasSharedRecords, sharedRecordCount, isCompleted), tomorrowCount, upcomingCount | AP5, AP7 |
| **PatientRecordDetails** | patientName, bloodGroup, age, gender, slotTimeRange, patientPhone, emergencyContact (name, phone), sessionTimer, documents[] (name, type), grantValidUntil | AP7, V3 |

### 5c. Blood Bank Module

| Screen | Data Fields Needed | API Endpoint(s) |
|--------|-------------------|-----------------|
| **BloodDash (Requests)** | requests[] (bloodGroup, severity, units, timeAgo, patientName, hospitalName, distance), bankName, isOnline | B8, BB1 |
| **BloodDash (Donations)** | bookings[] (donorName, bloodGroup, lastDonated, slotTime, status) | B16 |
| **BloodDonationStats** | totalDonors, activeToday, unitsCollected, nearbyRequests[] | B21, B8 |
| **BloodDonationHistory** | eligibility[], donationHistory[] (donorName, date, bloodGroup, units) | B20, B19 |
| **RequestBlood** | bloodGroups[], recentRequests[] (patientName, bloodGroup, units, status, timeAgo) | B8 (for list) |
| **BloodInventory** | inventory[] (bloodGroup, unitsAvailable, status, liters), totalUnits, maxCapacity, lastUpdated, lastUpdatedBy, filterCounts | B1, B2 |
| **InventoryStockDetails** | bloodGroup, totalVolume, unitsAvailable, storageTemp, batches[] (batchId, collectionDate, expiryDate, shelfLife, location), auditHistory[] | B4, B6 |
| **BloodBankProfile** | name, bloodBankCode, address, phone, email, licenseNumber, accreditation, facilityCategory, licenseExpiry, nodalOfficer, location, emergencyDispatchEnabled | BB1 |
| **LogUpdateInventory** | bloodGroups[], currentStock (per group) | B1 |

---

## 6. CRON JOBS / SCHEDULED TASKS

| Job | Frequency | Logic |
|-----|-----------|-------|
| Auto-expire PENDING_LOCK slots | Every 1 minute | Query `slots` where status=LOCKED AND lockExpiresAt < now. For each: set slot.status=OPEN, appointment.status=CANCELLED |
| Expire blood requests | Every 5 minutes | Query `blood_requests` where status in [SEARCHING, NOTIFIED] AND createdAt < now - 30min. Set status=EXPIRED |
| Expire vault grants | Every 1 minute | Query `appointments` where grants[].validUntil < now AND grants[].status=ACTIVE. Set grant.status=EXPIRED |

---

## 7. COLLECTIONS NOT YET IN FIRESTORE RULES

These are new collections needed that don't exist in the current `firestore.rules`:

| Collection | Purpose |
|------------|---------|
| `/donor_bookings/{bookingId}` | Blood donation slot bookings |
| `/blood_inventory/{id}/batches/{batchId}` | Blood batch tracking (subcollection) |

These need to be added to `firestore.rules`.

---

## 8. IMPLEMENTATION ORDER

### Phase 1: Foundation (Day 1)
1. Project scaffold (Gradle, Ktor, Dockerfile)
2. Firebase Admin SDK init
3. Auth middleware (verifyIdToken)
4. Error handling plugin
5. Firestore adapter

### Phase 2: Auth (Day 1)
6. A1: POST /api/auth/setup-role
7. A2: POST /api/auth/setup-consents
8. A3: GET /api/config/consents
9. A4: GET /api/config/roles

### Phase 3: Users (Day 1-2)
10. U1: GET /api/users/me
11. U2: PUT /api/users/me
12. U3: POST /api/users/fcm-token
13. U4-U7: CRUD /api/users/me/contacts

### Phase 4: Doctors (Day 2)
14. D1: GET /api/doctors (search)
15. D2: GET /api/doctors/:id
16. D3: GET /api/doctors/:id/slots
17. D4-D5: GET/PUT /api/doctors/me/profile
18. D6: PUT /api/doctors/me/availability
19. D7-D8: Schedule config + publish slots

### Phase 5: Appointments (Day 2-3)
20. AP1: POST /api/appointments/lock (transaction)
21. AP2: POST /api/appointments/:id/confirm (transaction)
22. AP3: POST /api/appointments/:id/cancel
23. AP4: POST /api/appointments/:id/complete
24. AP5-AP7: GET appointments + patient record

### Phase 6: Blood (Day 3-4)
25. B1-B6: Inventory CRUD + batches + audit
27. B7: POST /api/blood/request (with FCM)
28. B8: GET /api/blood/requests
29. B9: POST /api/blood/requests/:id/fulfill (transaction with inventory decrement)
30. B10-B11: Decline + escalate
31. B12: GET /api/blood/nearby
32. B13-B14: Donor registration
33. B15-B18: Donation bookings (no lock)
34. B19-B21: Donation history + eligibility + stats
35. BB1-BB3: Blood bank profile + settings

### Phase 7: Vault (Day 4)
36. V1-V2: Document list + upload URL
37. V3: Time-boxed download URL
38. V4: Delete document

### Phase 8: AI (Day 4-5)
39. AI1: POST /api/ai/analyze
40. AI2: POST /api/ai/chat

### Phase 9: Cron Jobs (Day 5)
41. Auto-expire PENDING_LOCK slots
42. Auto-expire blood requests
43. Auto-expire vault grants

### Phase 10: Deploy (Day 5)
44. Docker build
45. Cloud Run deploy
46. Firebase rules update (add new collections)
47. Android app API client integration

---

## 9. MISSING MODEL FIELDS (to add to existing Kotlin models)

### User.kt — add:
- `phone: String = ""`
- `profile.dateOfBirth: Long = 0`
- `profile.gender: String = ""`

### Doctor.kt — add:
- `qualifications: String = ""`
- `experienceYears: Int = 0`
- `consultCount: Int = 0`
- `isOnline: Boolean = false`

### Clinic (in Doctor.kt) — add:
- `department: String = ""`
- `roomNumber: String = ""`
- `phone: String = ""`
- `email: String = ""`

### BloodInventoryItem (in BloodBank.kt) — add:
- `maxCapacity: Int = 0`
- `storageTemp: Double = 0.0`
- `lastUpdatedBy: String = ""`

### BloodRequest (in BloodBank.kt) — add:
- `hospitalName: String = ""`
- `severity: String = ""`
- `distance: Double = 0.0` (computed at query time)

### NEW models needed:
- `BloodBatch` (batch tracking)
- `DonorBooking` (donation slot booking)
- `BloodBankProfile` (facility profile)
- `BloodBankStats` (aggregate stats)
- `AuditLog` (audit trail)
- `ScheduleConfig` (doctor shift config)
- `PatientRecordView` (appointment + patient + documents)
