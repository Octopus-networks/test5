# Mithaq (ميثاق) - Premium Islamic Matchmaking Platform

**Mithaq (ميثاق)** is a highly secure, privacy-first matchmaking platform designed for serious marriage in the Islamic world. It employs a clean-architecture MVVM approach with Jetpack Compose to deliver a premium, culturally-sensitive experience.

---

## 🚀 Core Features & Architectural Overview

### 1. Smart Compatibility Match Score (`com.mithaq.app.ui.match`)
- **Islamic Compatibility Algorithm**: Scores profiles on a `0 - 100%` scale based on crucial values (Sect: 20%, Prayer: 15%, Religious Values: 15%, etc.).
- **Dynamic Match Badges**: Visual circular compatibility progress with color grading (Emerald for high, Amber for moderate, Red/Gray for low matches).

### 2. The Guardian (Wali) Ecosystem (`com.mithaq.app.ui.guardian`)
- **Guardian Invitation**: Seamless invitation flow with email validation and "Pending/Verified" status tracking.
- **Enhanced Wali Dashboard**: A dedicated interface for guardians to monitor active conversations, approve photo access, and review identity verification requests.
- **Wali Safety Alerts**: (NEW) Automatic detection and flagging of contact information exchange to ensure chaperoned safety.

### 3. Secure Chaperoned Chat (`com.mithaq.app.ui.chat`)
- **Chaperonage Flags**: Chat rooms are marked `isChaperoned` with direct transcript mirroring to `waliLogs` in Firestore.
- **In-Chat Translation**: Instant Arabic <-> English translation preloaded with matchmaking phrases.
- **Ice Breakers**: (NEW) Pre-defined, respectful questions to facilitate serious and purposeful conversations.
- **Voice Calls**: Lifecycle-aware chaperoned voice calls with integrated Wali oversight.

### 4. Advanced Privacy & Security Layer (`com.mithaq.app.security`)
- **Biometric Lock**: (NEW) Fingerprint and Face ID authentication using `BiometricPrompt` to protect sensitive data on app launch.
- **SecureScreen**: Blocks screenshots and screen recordings on chat and profile views using `FLAG_SECURE`.
- **Modesty Blur Overlay**: Applies hardware-accelerated `RenderEffect` blur on profile images, unlockable via multi-stage requests or premium access.
- **Contact Info Protection**: Real-time warning system preventing the exchange of phone numbers or social media handles.

### 5. Advanced Search & Filtering (`com.mithaq.app.ui.filter`)
- **Logical Filtering**: Combines Firestore queries with local logical evaluation for complex criteria (Age, Sect, Prayer, Modesty, Relocation, Polygamy).
- **Material 3 Bottom Sheets**: Intuitive UI using custom FlowRow layouts, sliders, and filter chips.

### 6. Verification & Trust (`com.mithaq.app.ui.auth`)
- **AI-Powered Selfie Verification**: Uses Google ML Kit Face Detection to ensure identity authenticity via selfie videos and ID card uploads.
- **Trust Badges**: Visual verification markers across the app for verified users.

### 7. User Limits & Premium Experience (`com.mithaq.app.ui.limit`)
- **Smart Daily Limits**: Restricts free users to 3 new chat initiations per day using calendar-based Firestore counters.
- **Premium Store**: A gold-branded store for platinum/gold subscriptions, offering unlimited chats and enhanced modesty controls.

---

## 📂 Repository File Index

```
src/main/java/com/mithaq/app/
│
├── model/
│   ├── UserProfile.kt           # Demographic, religious & lifestyle attributes
│   ├── FilterCriteria.kt        # Advanced search preference model
│   └── ChatRoom.kt              # Chaperoned chat room metadata
│
├── security/
│   ├── BiometricAuthManager.kt  # (NEW) Biometric authentication logic
│   ├── SafetyUtils.kt           # (NEW) Shared safety & contact detection logic
│   ├── SecurityExtensions.kt    # Screenshot prevention wrapper
│   └── BlurModifier.kt          # Modesty image blur utility
│
├── ui/
│   ├── auth/
│   │   ├── AuthViewModel.kt     # Firebase Sign In / Sign Up & Online status
│   │   └── RegisterScreen.kt    # Multi-step religious onboarding
│   │
│   ├── chat/
│   │   ├── IceBreakerGenerator.kt # (NEW) Purposeful chat suggestions
│   │   ├── ChaperonedChatViewModel.kt # Wali mirroring & secure messaging
│   │   └── TranslationHelper.kt # Instant Arabic/English bridge
│   │
│   ├── guardian/
│   │   └── WaliDashboard.kt     # Guardian monitoring & safety alerts
│   │
│   ├── match/
│   │   ├── MatchScoreCalculator.kt # Compatibility scoring algorithm
│   │   └── MatchScoreBadge.kt   # Dynamic UI scoring components
│   │
│   └── theme/
│       ├── Color.kt             # Emerald & Gold HSL branding
│       └── Theme.kt             # Status bar & Light/Dark M3 coordination
```

---

## 🛠️ Technical Stack
- **Language**: Kotlin (100%)
- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM + Clean Architecture
- **Backend**: Firebase (Auth, Firestore, Storage, Cloud Messaging)
- **Local Cache**: Room Database (Offline-first readiness)
- **AI/ML**: Google ML Kit (Face Detection) + Gemini SDK (AI Icebreakers)
- **Security**: Biometric API + Android Window Manager Flags
