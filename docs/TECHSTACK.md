# Tech Stack Specifications — MedKeen

> Single source of truth for all technology choices. **No credentials, API keys, bucket names, or endpoints may be hardcoded in source.** Everything sensitive lives in environment files / Firebase config / Secret Manager and is referenced via a typed `config` module.

## 1. Mobile Client
| Concern | Choice | Notes |
|---------|--------|-------|
| Framework | **Expo SDK 51+** (React Native) | Managed workflow; OTA updates; one codebase iOS/Android (+ web for Admin later) |
| Language | **TypeScript (strict: true)** | No `any` in app logic |
| Navigation | **Expo Router** (file-based) | Dynamic stack switch keyed on `userRole` claim; deep links per appointment/doc |
| Server state | **TanStack Query v5** | Firestore reads via query fns; real-time via `onSnapshot` adapters |
| Local state | **Zustand** | Auth session, active role, UI state matrix |
| Styling | **NativeWind** (Tailwind) + **React Native Paper** primitives | Theme tokens in `theme.ts` (no raw hex scattered) |
| Secure storage | **expo-secure-store** | Holds AES key + refresh token; never persisted to Firestore |
| Biometrics | **expo-local-authentication** | App lock + key unlock |
| Files | **expo-document-picker**, **expo-image-picker**, **expo-file-system** | Upload/decrypt flows |
| Forms | **react-hook-form** + **zod** | Validation schemas shared client/function side |

## 2. Backend (Firebase)
| Concern | Choice | Notes |
|---------|--------|-------|
| Auth | **Firebase Auth (Google OAuth only)** | Single sign-in method = Google OAuth (`GoogleAuthProvider`); **custom claims** for `role` + `verified`. No phone/OTP or email/password flows. |
| Database | **Cloud Firestore** | Document store; **GeoFirestore** for geo queries |
| File Vault | **Firebase Storage** | Private bucket `vault/{patientId}/{docId}`; v4 signed URLs via Functions |
| Compute | **Cloud Functions v2** (Node 20, TypeScript) | Slot lock txn, time-boxed URL mint, radius search, AI proxy, audit writer |
| Push | **Firebase Cloud Messaging (FCM)** | Priority notifications (blood requests, booking) |
| Config/Secrets | **Firebase config + Secret Manager / `.env.local`** | Never committed; loaded via `firebase-functions` params |

## 3. AI Engine
| Concern | Choice | Notes |
|---------|--------|-------|
| Model | **Gemini 1.5 Flash** (Vertex AI or AI Studio REST) | Streaming `generateContent` |
| Proxy | **Cloud Function `aiProxy`** | Holds API key (Secret Manager); SSE stream to client; 112 guardrail middleware |
| Vision | Gemini multimodal | Lab report image/PDF OCR → structured summary |

## 4. Configuration & "No Hardcoding" Rules
- All env-specific values (project id, bucket, region, API base, radius constants, lock windows) live in:
  - `app/config/index.ts` ← reads from `expo-constants` `extra` (from `app.config.js` / `.env`)
  - `functions/src/config.ts` ← reads from `process.env` / Functions params
- A single `constants.ts` exports: `LOCK_WINDOW_MS`, `SHARE_GRACE_MS`, `RADIUS_START_KM`, `RADIUS_MAX_KM`, `RADIUS_STEP_KM`, `DPDP_VERSION`.
- Secrets (Gemini key, FCM server key) → **Secret Manager**, injected at deploy; referenced only in Functions.
- Lint rule (`eslint-no-secrets`) fails build on detected secrets.
- `.env.example` committed; real `.env*` gitignored.

## 5. Tooling & Quality
- **ESLint + Prettier** (shared config).
- **TypeScript strict**; `tsc --noEmit` in CI.
- **Jest** + **React Native Testing Library** for units; **Firebase Emulator Suite** for integration (Auth/Firestore/Storage/Functions).
- **GitHub Actions**: lint → typecheck → emulator tests → build.

## 6. Dependency Versions (pin in package.json)
- `expo@~51`, `react@18.3`, `firebase@^10`, `@tanstack/react-query@^5`, `zustand@^4`, `nativewind@^4`, `geofirestore@^5`, `google-auth-library` (Functions), `@google-cloud/secret-manager`.
