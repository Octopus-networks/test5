# Mithaq (ميثاق) - Premium Upgrade Release

This repository contains the core clean-architecture MVVM & Jetpack Compose updates for **Mithaq (ميثاق)**, a highly secure, privacy-first matchmaking platform designed for serious marriage in the Islamic world.

---

## 🚀 Added Features & Architectural Overview

### 1. Advanced Islamic Search Filters (`com.mithaq.app.ui.filter`)
- **`FilterCriteria`**: A structured data model encapsulating complex search queries (Age Range, Sect, Prayer frequency, Modesty/Hijab preferences, Relocation willingness, and Polygamy acceptance).
- **`SearchFilterBottomSheet`**: A Material Design 3 Composable sheet using custom `FlowRow` layouts, sliders, and filter chips for dynamic adjustments.
- **`SearchViewModel`**: Implements local filtering on loaded profile pools to allow complex logical evaluation not natively indexable by Firestore alone.

### 2. Smart Compatibility Match Score (`com.mithaq.app.ui.match`)
- **`MatchScoreCalculator`**: An Islamic compatibility algorithm that scores profiles on a scale of `0 - 100%`. It weighs crucial values (Sect matching: 30%, Prayer consistency: 30%, Modesty alignment: 20%, Relocation: 10%, Age: 10%).
- **`MatchScoreBadge`**: A dynamic Compose badge showing circular compatibility progress with dynamic color grading (Emerald Green for high, Amber for moderate, Red/Gray for low matches) and animated sweeps.

### 3. The Guardian (Wali) Integration (`com.mithaq.app.ui.guardian`)
- **`InviteGuardianDialog`**: Material Design 3 dialog prompting validation for the Guardian's name and email with interactive loading states.
- **`GuardianViewModel`**: Persists name and email details, establishing standard status markers (`guardianStatus = "Pending"`) on the user's root document.

### 4. In-Chat Instant Translation (`com.mithaq.app.ui.chat`)
- **`TranslationHelper`**: High-performance interface and mock translator preloaded with standard cross-cultural matchmaking phrases (Arabic <-> English).
- **`ChatBubble`**: UI messaging bubble displaying a translate icon, handling loading spinner feedback, and toggling translation text in-place.

### 5. Smart Daily Chat Limits (`com.mithaq.app.ui.limit`)
- **`ChatLimitManager`**: Limits free users to initiating 3 new chats per day by keeping calendar-based transaction counters in Firestore.
- **`PremiumUpgradeDialog`**: Gold-branded premium subscription modal displaying the full tier value proposition (unlimited chats, search filters, modesty blur control).

### 6. Privacy & Security Layer (`com.mithaq.app.security`)
- **`SecureScreen`**: A lifecycle-aware Composable wrapper that applies `FLAG_SECURE` to the active window, blocking screenshotting or screen recording on sensitive views (e.g. Chat logs, profiles).
- **`modestyBlur`**: Compose modifier applying modesty blur overlay on images utilizing hardware-accelerated `RenderEffect` (Android 12+) and standard backward-compatible blur fallbacks.

### 7. Wali Chaperoned Chat (`com.mithaq.app.ui.chat`)
- **`ChatRoom`**: Extended room metadata representing membership and active chaperonage flags (`isChaperoned`, `waliEmail`).
- **`ChaperonedChatBanner`**: Top-anchored warning header notifying both participants that a guardian has direct transcript permissions.
- **`ChaperonedChatViewModel`**: Seamlessly duplicates chat history writes to a dedicated `waliLogs` Firestore path for Wali reviews.

### 8. Multi-Stage Modesty Photo Unlock (`com.mithaq.app.ui.photo`)
- **`PhotoAccessManager`**: High-level permission broker coordinating photo-viewing requests and atomic approvals in Firestore.
- **`PhotoAccessRequestCard`**: Smart dialog controls. Prompts viewers to request photo unblur access, and notifies profile owners of incoming requests with single-tap Approve/Decline actions.

### 9. Core Design System & Mithaq Theme (Phase 3 Additions) 🎨
- **`Color.kt`**: Color scheme constants. Employs a luxurious HSL theme pairing Deep Emerald greens, warm metallic Golds/Ambers, soft eggshell backgrounds, and midnight charcoal backdrops.
- **`Type.kt`**: Material 3 typography mappings using specialized Google Fonts (Cairo for Arabic UI elements, Amiri for Arabic titles/Quranic texts, and Outfit for Latin symbols) with safe local fallbacks.
- **`Theme.kt`**: Main application Composable theme `MithaqTheme` managing system status bars and Light/Dark Material Design 3 color schemes.

---

## 📂 Repository File Index

```
src/main/java/com/mithaq/app/
│
├── model/
│   ├── UserProfile.kt           # Contains User profile attributes & modesty lists
│   ├── FilterCriteria.kt        # Search preference model
│   └── ChatRoom.kt              # Chat room metadata model
│
├── security/
│   ├── SecurityExtensions.kt    # SecureScreen screenshot prevention wrapper
│   └── BlurModifier.kt          # Modesty image blur utility
│
├── ui/
│   ├── theme/
│   │   ├── Color.kt             # Brand emerald and gold colors
│   │   ├── Type.kt              # Typography settings (Cairo, Amiri, Outfit)
│   │   └── Theme.kt             # MithaqTheme and Status Bar customization
│   │
│   ├── filter/
│   │   ├── SearchFilterBottomSheet.kt
│   │   └── SearchViewModel.kt
│   │
│   ├── match/
│   │   ├── MatchScoreBadge.kt
│   │   └── MatchScoreCalculator.kt
│   │
│   ├── guardian/
│   │   ├── InviteGuardianDialog.kt
│   │   └── GuardianViewModel.kt
│   │
│   ├── chat/
│   │   ├── ChatBubble.kt
│   │   ├── ChaperonedChatBanner.kt
│   │   ├── ChaperonedChatViewModel.kt
│   │   └── TranslationHelper.kt
│   │
│   ├── photo/
│   │   ├── PhotoAccessManager.kt
│   │   └── PhotoAccessRequestCard.kt
│   │
│   └── limit/
│       ├── PremiumUpgradeDialog.kt
│       └── ChatLimitManager.kt
```

---

## 🛠️ Integration Instructions

For deep integration guides and step-by-step code samples on how to link these Composables and ViewModels to your pre-existing layouts, please refer directly to the **[Walkthrough Guide](file:///C:/Users/ahmed/.gemini/antigravity/brain/e664f4e8-70a4-428c-b16f-1d9849e90f5e/walkthrough.md)**.
