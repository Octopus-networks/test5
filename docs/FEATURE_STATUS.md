# Mithaq — Feature Status Matrix

> Honest status of each user-facing feature. Legend:
> **✅ Done** (works end-to-end) ·
> **🟡 Foundation** (data/UI exists, enforcement or backend incomplete) ·
> **🖼️ UI-only** (screen exists, not wired to a real backend) ·
> **📋 Planned**.
>
> Canonical messaging system = **`chats/*` (new)**. `chatRooms/*` is legacy and not built upon.
> Current `main` includes PRs #74-#96; latest release tag is `v2.1.0` at PR #93.

## Auth & onboarding
| Feature | Status | Notes / evidence |
|---|---|---|
| Email/password auth + email verification | ✅ Done | `AuthViewModel`, gated by `isVerifiedEmailUser()` in rules |
| Password reset / email verification deep links | ✅ Done | `docs/auth/`, manifest deep links |
| Mandatory profile onboarding | ✅ Done | one 58-step question engine; incomplete users cannot enter the main experience |
| Data-driven onboarding configuration | ✅ Done | `assets/onboarding/onboarding_steps.json`; all sections load from JSON with a static fallback and consistency gate |
| Onboarding → matchable user mirror | ✅ Done | `mirrorProfileToUser` maps structured `profiles/{uid}` answers into `users/{uid}` |

## Discovery, matching & requests
| Feature | Status | Notes / evidence |
|---|---|---|
| Discovery / search from `publicProfiles` mirror | ✅ Done | `PublicProfileRepository`, server-owned mirror |
| Discover opposite-gender filter | ✅ Done | direction filtering in `PublicProfileRepository` |
| Incognito exclusion from Discover / Search | ✅ Done | `isIncognito` mirrored server-side and filtered before presentation |
| Like / unlike toggle + mutual match | ✅ Done | heart action toggles; mutual match computed server-side (`onLikeCreated`) |
| Interest → photo → chat request flow | ✅ Done | deterministic ids and rules-gated transitions |
| Server-created chat requests + daily cap | ✅ Done | `recordChatInitiation` atomically enforces 3 free initiations per UTC day; premium is unlimited; client creates are denied |
| Compatibility scoring | ✅ Done | `ui/match/MatchScoreCalculator.kt` |

## Chat
| Feature | Status | Notes / evidence |
|---|---|---|
| Text chat (**canonical `chats/*` stack**) | ✅ Done | `ChatRepository` + `ChatMessageRepository`; room creation requires an approved chat request and rules validation |
| Chat image attachments | ✅ Done | `chat_attachments/{chatId}/{messageId}` Storage rule + notification function |
| Chat voice notes | ✅ Done | record + playback |
| Message reactions (emoji) | ✅ Done | participant-scoped reaction updates in rules |
| Premium read receipts | ✅ Done | recipients write `readAt`; only premium senders see the read indicator |
| Chaperoned chat + wali logs | 🟡 Foundation (legacy) | implemented on legacy `chatRooms/*`; re-implement on canonical `chats/*` later |

## Premium entitlements
> The perks are implemented on current `main`, but **billing is OFF**. Premium status is
> server/admin controlled until Google Play Billing and purchase verification are added.

| Feature | Status | Notes / evidence |
|---|---|---|
| Incognito | ✅ Done | premium setting; member is hidden from Discover / Search while enabled |
| Boost | ✅ Done | bounded premium re-rank in `SearchViewModel`; preserves relevance rather than hard-sorting all premium first |
| Read receipts | ✅ Done | premium message senders see when messages are read |
| Profile Highlight | ✅ Done | reusable `PremiumHighlight` renders a gold border around premium profile cards |
| 2x Exposure | ✅ Done | deterministic 2:1 premium/free interleave in Discover while preserving recency inside each group |
| Gold / Platinum purchase flow | 🖼️ UI-only | store UI exists; `BetaFeatureGates.PREMIUM_BILLING = false`; no Google Play Billing |

## Trust, safety & verification
| Feature | Status | Notes / evidence |
|---|---|---|
| Identity verification (ID + selfie + ML Kit) | ✅ Done | upload → `verification/{uid}/`, PENDING → wali/admin approval |
| Verified badge in profile surfaces | ✅ Done | reusable `VerifiedBadge`; `isIdentityVerified` mirrored into `publicProfiles` |
| Photo privacy (request + approved viewers) | ✅ Done | Firestore metadata and Storage bytes are both access-gated |
| Photo-access revocation | ✅ Done | owner deletes the approved request; Storage access stops immediately |
| Logout/login state isolation | ✅ Done | per-user ViewModel keys prevent cross-account state leakage |
| Bidirectional block enforcement | ✅ Done | Firestore and Storage rules deny protected interaction/media paths when either member blocked the other |
| Admin console (users, premium, roles, verification) | ✅ Done | callable Cloud Functions, admin-gated |
| Admin moderation — reports & photo review | ✅ Done | `AdminModerationRepository`, UI/ViewModel/rules gating |
| Admin moderation — suspend / ban | ✅ Done | Auth account disabled, refresh tokens revoked, and interactive writes denied until reactivated |
| Real admin account deletion | ✅ Done | deletes Firebase Auth identity, Firestore documents/subcollections, Storage media, and writes an audit record |

## Profile hub & account settings (`ui/profile`)
All 10 hub cards are wired. The hub raises `onOpenXxx` callbacks → `MithaqMainExperience` →
`MainActivity` routes.

| Feature | Status | Notes / evidence |
|---|---|---|
| My photos management | ✅ Done | `MyPhotosScreen` |
| Edit profile (info, bio, modesty, guardian) | ✅ Done | `ProfileSettingsScreen`; Gemini action remains gated |
| Privacy — per-field public visibility | ✅ Done | stored at `profiles/{uid}.privacyTrust`, honored by `buildPublicProfile` |
| Photo privacy management (approve / reject / revoke) | ✅ Done | `PhotoPrivacyScreen` + `PhotoAccessManager`; revocation enforced by rules |
| Prayer settings (Adhan) | ✅ Done | location-aware `AdhanScheduler` + `PrayerSettingsScreen` |
| Notification settings | ✅ Done | global + per-type toggles |
| Language / theme | ✅ Done | Arabic ⇄ English + dark mode |
| Security — biometric app lock | ✅ Done | opt-in per-user setting is enforced at app launch; disabled users are not prompted |
| Support & help (FAQ + email contact) | ✅ Done | `SupportScreen` |
| Guardian (Wali) invite / status | ✅ Done | `GuardianScreen` + `GuardianViewModel` |

## Notifications
| Feature | Status | Notes / evidence |
|---|---|---|
| Push notifications (FCM) | ✅ Done | `users/{uid}/fcmTokens`, Cloud Function triggers |
| Per-type notification sounds | ✅ Done | bundled `res/raw` chimes; global default + per-category override |
| Notification preferences gating | ✅ Done | `users/{uid}/notificationSettings/preferences` honored in Functions |

## Islamic & lifestyle
| Feature | Status | Notes / evidence |
|---|---|---|
| Location-based Adhan scheduling / prayer tracking | ✅ Done | location requested on app open; exact-alarm scheduling uses per-location prayer times |

## Deferred / incomplete
| Feature | Status | Notes / evidence |
|---|---|---|
| Google Play Billing + purchase verification | 📋 Planned | premium perks exist, but real purchases remain disabled |
| AI matchmaker / Gemini assistance | 🖼️ UI-only / debug | release ships an empty key; keep hidden without a backend proxy |
| Voice call | 🖼️ UI-only | scaffolding only; keep hidden |
| Wali monitoring on canonical chat | 🟡 Foundation | still tied to legacy `chatRooms/*` |

## Known "before production" gaps

- Add Google Play Billing with server-side receipt verification and entitlement lifecycle.
- Re-implement chaperoned/wali monitoring on canonical `chats/*`; retire legacy chat code.
- Consider moving final `chats/*` room creation behind a callable; current rules already require
  an approved server-created chat request and validate the room shape.
- Verify the latest Functions / Firestore / Storage deployment in every environment.
- Complete signed-release and two-account on-device smoke testing.

See [`TECH_DEBT.md`](./TECH_DEBT.md) for the engineering backlog behind the remaining gaps.
