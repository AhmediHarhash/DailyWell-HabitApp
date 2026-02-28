# DailyWell - Features Roadmap (February 2026)

## Current Status: 90% Complete
The core app is production-ready with AI coaching, gamification, social features, premium monetization, proactive notifications, voice AI input, and calendar integration.

---

## MISSING FEATURES TO IMPLEMENT (Priority Order)

### 1. PROACTIVE AI NOTIFICATIONS
**Status:** ✅ COMPLETED
**Priority:** 🔴 HIGH
**Effort:** Low
**Impact:** High retention boost

**Description:**
AI coach initiates conversations instead of waiting for users. Context-aware nudges based on user patterns, time of day, and activity.

**What was built:**
- [x] "Haven't seen you today" notification after X hours of inactivity
- [x] "Your streak is at risk" warning before day ends
- [x] Morning motivation push at user's optimal time
- [x] Comeback messages after 2+ days of missed check-ins
- [x] Celebration pushes when milestones are near ("Just 1 more day for 7-day streak!")
- [x] Context-aware nudges ("It's lunch time - perfect for your hydration habit")
- [x] 12 notification types with templates
- [x] Smart frequency capping (max per day, min time between)
- [x] Do Not Disturb / Quiet Hours support
- [x] Smart timing that learns from user behavior
- [x] AI-generated personalized messages using Claude
- [x] Full settings UI with tone selection
- [x] WorkManager integration for reliable scheduling

**Files created:**
- `shared/src/commonMain/kotlin/com/dailywell/app/data/model/ProactiveNotification.kt`
- `shared/src/commonMain/kotlin/com/dailywell/app/data/repository/ProactiveNotificationRepository.kt`
- `shared/src/androidMain/kotlin/com/dailywell/app/data/repository/ProactiveNotificationRepositoryImpl.kt`
- `shared/src/androidMain/kotlin/com/dailywell/app/notification/ProactiveNotificationWorker.kt`
- `shared/src/commonMain/kotlin/com/dailywell/app/ui/screens/notifications/ProactiveNotificationSettingsViewModel.kt`
- `shared/src/commonMain/kotlin/com/dailywell/app/ui/screens/notifications/ProactiveNotificationSettingsScreen.kt`

---

### 2. VOICE AI CHAT INPUT
**Status:** ✅ COMPLETED
**Priority:** 🔴 HIGH
**Effort:** Medium
**Impact:** Huge differentiator in 2026

**Description:**
Users can speak to their AI coach instead of typing. Voice input transcribed and sent to Claude AI, response displayed as text (with optional TTS playback).

**What was built:**
- [x] Microphone button in AI chat screen
- [x] Real-time speech-to-text transcription
- [x] Visual feedback while recording (waveform animation)
- [x] Auto-send after speech pause detection
- [x] Option to play AI response aloud (TTS)
- [x] "Hold to talk" vs "Tap to start/stop" modes
- [x] VoiceInputStatus component for transcription progress
- [x] Settings persistence via DataStore

**Technical approach used:**
- Android SpeechRecognizer API (free, offline capable)
- Android TextToSpeech for AI response playback
- RECORD_AUDIO permission handling
- DataStore for voice settings persistence

**Files created:**
- `shared/src/commonMain/kotlin/com/dailywell/app/speech/SpeechRecognitionService.kt`
- `shared/src/androidMain/kotlin/com/dailywell/app/speech/SpeechRecognitionServiceImpl.kt`
- `shared/src/commonMain/kotlin/com/dailywell/app/ui/components/VoiceInputButton.kt`

---

### 3. CALENDAR INTEGRATION
**Status:** ✅ COMPLETED
**Priority:** 🔴 HIGH
**Effort:** Medium
**Impact:** Top competitor feature (Reclaim.ai is #1 because of this)

**Description:**
Sync with Google Calendar/Outlook to auto-schedule habit time and provide smart suggestions based on free slots.

**What was built:**
- [x] Google Calendar OAuth integration
- [x] Outlook Calendar OAuth integration
- [x] "Best time for [habit] today" suggestions based on free slots
- [x] Auto-block time for habits (optional)
- [x] Reschedule suggestions when calendar gets busy
- [x] "You have a free 30 min at 2 PM - perfect for your workout" notifications
- [x] Calendar view of habit schedule (week view with events)
- [x] Interactive weekly habit tracker matrix (tap-to-check per day/habit)
- [x] Real persistence for matrix checkoffs via EntryRepository
- [x] Future-day lock and per-cell loading states for safe edits
- [x] Free slot quality scoring algorithm
- [x] Habit time suggestion generation
- [x] Background calendar sync with WorkManager
- [x] Calendar settings UI with account management

**Technical approach used:**
- Google Calendar API with OAuth 2.0
- Microsoft Graph API for Outlook with OAuth 2.0
- Ktor HTTP client with Android engine
- Store calendar events locally for offline suggestions
- AI analyzes patterns between calendar busyness and habit completion
- WorkManager for background sync every 30 minutes

**Files created:**
- `shared/src/commonMain/kotlin/com/dailywell/app/data/model/CalendarModels.kt`
- `shared/src/commonMain/kotlin/com/dailywell/app/data/repository/CalendarRepository.kt`
- `shared/src/androidMain/kotlin/com/dailywell/app/api/CalendarService.kt`
- `shared/src/androidMain/kotlin/com/dailywell/app/data/repository/CalendarRepositoryImpl.kt`
- `shared/src/commonMain/kotlin/com/dailywell/app/ui/screens/calendar/CalendarViewModel.kt`
- `shared/src/commonMain/kotlin/com/dailywell/app/ui/screens/calendar/CalendarIntegrationScreen.kt`
- `shared/src/androidMain/kotlin/com/dailywell/app/notification/CalendarNotificationWorker.kt`

---

### 4. PREDICTIVE "AT-RISK" ALERTS
**Status:** ⏳ Pending
**Priority:** 🟡 MEDIUM
**Effort:** Low
**Impact:** AI edge over competitors

**Description:**
AI predicts when habits are likely to fail before it happens, based on historical patterns and current context.

**What to build:**
- [ ] Pattern analysis: "You usually skip workouts on Wednesdays"
- [ ] Calendar-aware: "Back-to-back meetings today - your meditation is at risk"
- [ ] Weather-aware: "Rainy day - outdoor exercise might be harder"
- [ ] Streak risk scoring (low/medium/high)
- [ ] Preemptive suggestions: "Consider doing your workout now before your 3 PM meeting"
- [ ] "Habit health" indicator per habit

**Technical approach:**
- Analyze historical completion data by day of week, time, weather
- Integrate with calendar data (from feature #3)
- Weather API integration
- ML model or rule-based scoring

---

### 5. DAILY MICRO-CHALLENGES
**Status:** ⏳ Pending
**Priority:** 🟡 MEDIUM
**Effort:** Low
**Impact:** Fresh daily engagement

**Description:**
Optional bonus challenges each day that add variety and extra XP rewards. Keeps the app feeling fresh, not repetitive.

**What to build:**
- [ ] Daily challenge card on home screen
- [ ] Challenge types:
  - Time-based: "Complete your first habit before 8 AM"
  - Combo: "Complete 3 habits in a row without breaks"
  - Specific: "Drink 2 extra glasses of water today"
  - Social: "Send a high-five to someone"
  - Streak: "Maintain your streak for one more day"
- [ ] Bonus XP rewards (25-100 XP)
- [ ] Optional (users can skip without penalty)
- [ ] Challenge history and completion rate

**Technical approach:**
- Challenge template system
- Daily rotation algorithm (avoid repetition)
- Difficulty scaling based on user level
- AI-generated personalized challenges (premium)

---

### 6. AUTO-COMPLETE FROM WEARABLES
**Status:** ⏳ Pending
**Priority:** 🟡 MEDIUM
**Effort:** Medium
**Impact:** Zero-friction habit tracking

**Description:**
Automatically mark habits complete when wearable/health data confirms it. No manual check-in needed.

**What to build:**
- [ ] Auto-complete "Move" when step goal reached
- [ ] Auto-complete "Rest" when sleep duration >= 7 hours
- [ ] Auto-complete "Hydrate" from smart water bottle (if available)
- [ ] Auto-complete "Calm" when meditation app session detected
- [ ] User toggle: "Auto-complete enabled" per habit
- [ ] Manual override option
- [ ] Notification: "Your workout was auto-logged!"

**Technical approach:**
- Health Connect API (already scaffolded)
- Background sync every 30 min
- Threshold configuration per habit
- Conflict resolution (manual vs auto)

---

### 7. VIRTUAL REWARDS (REAL VALUE)
**Status:** ⏳ Pending
**Priority:** 🟡 MEDIUM
**Effort:** High
**Impact:** Strong retention driver

**Description:**
XP/coins can be redeemed for real rewards like gift cards or charity donations. Makes progress tangible.

**What to build:**
- [ ] "WellCoins" virtual currency (separate from XP)
- [ ] Earn WellCoins from:
  - Daily check-in: 5 coins
  - Perfect day: 20 coins
  - Weekly streak: 50 coins
  - Achievements: 25-100 coins
  - Referrals: 200 coins
- [ ] Redemption store:
  - $5 Amazon gift card: 5,000 coins
  - $10 Starbucks: 10,000 coins
  - Donate to charity: 2,500 coins
  - Premium themes: 1,000 coins
  - Extra streak shields: 500 coins
- [ ] Partner integrations for rewards
- [ ] Coin balance display in profile
- [ ] Transaction history

**Technical approach:**
- Secure coin ledger (server-side validation)
- Partner APIs for gift card fulfillment (Tango Card, etc.)
- Fraud prevention
- Premium users earn 2x coins

---

### 8. AI COACH MEMORY (RELATIONSHIP BUILDING)
**Status:** ⏳ Pending
**Priority:** 🟡 MEDIUM
**Effort:** Medium
**Impact:** Deep emotional connection

**Description:**
AI coach remembers past conversations and references them in future interactions. Creates genuine relationship feeling.

**What to build:**
- [ ] Store conversation summaries/key points
- [ ] AI references past struggles: "Last week you mentioned work stress..."
- [ ] Remember user preferences mentioned in chat
- [ ] Track emotional patterns over time
- [ ] "Our journey together" summary (weeks/months of coaching)
- [ ] Remember names of family, pets, coworkers mentioned

**Technical approach:**
- Conversation summary storage (Firestore)
- Context injection into Claude prompts
- Key entity extraction (names, events, goals)
- Privacy controls (user can clear memory)

---

### 9. WEEKLY "WELLNESS SCORE" REPORT
**Status:** ⏳ Pending
**Priority:** 🟡 MEDIUM
**Effort:** Low
**Impact:** Shareable, trackable progress

**Description:**
Single 0-100 score combining all habit performance. Easy to understand, track, and share.

**What to build:**
- [ ] Wellness Score algorithm:
  - Habit completion rate (40%)
  - Streak maintenance (20%)
  - Consistency across habits (20%)
  - Improvement trend (20%)
- [ ] Weekly report push notification (Sunday evening)
- [ ] Score history graph
- [ ] Week-over-week comparison
- [ ] Shareable score card (Instagram Stories format)
- [ ] Breakdown by habit category

**Technical approach:**
- Score calculation on weekly basis
- Store historical scores
- Image generation for share cards
- Deep link to app from shared card

---

### 10. COMMUNITY GUILDS
**Status:** ⏳ Pending
**Priority:** 🟢 LOW
**Effort:** High
**Impact:** Long-term engagement play

**Description:**
Public communities organized by interest where users join themed groups, participate in collective challenges, and support each other.

**What to build:**
- [ ] Guild categories:
  - Morning People
  - Fitness Warriors
  - Digital Minimalists
  - Meditation Masters
  - Hydration Nation
  - Sleep Champions
- [ ] Join/leave guilds
- [ ] Guild leaderboards
- [ ] Guild collective challenges ("Together, log 10,000 habits this week")
- [ ] Guild chat
- [ ] Guild achievements
- [ ] Guild levels based on collective activity

**Technical approach:**
- Firestore collections for guilds
- Real-time chat (Firestore or dedicated chat service)
- Aggregate stats calculation
- Moderation tools

---

### 11. AR HABIT OVERLAYS
**Status:** ⏳ Pending
**Priority:** 🟢 LOW
**Effort:** High
**Impact:** Trendy but niche appeal

**Description:**
Augmented reality features for habit engagement. Experimental/innovative feature.

**What to build:**
- [ ] AR water bottle overlay (point camera, see hydration progress)
- [ ] AR morning routine checklist floating in view
- [ ] AR achievement celebrations (confetti, badges in real world)
- [ ] AR habit reminders placed in environment
- [ ] Gamified AR challenges

**Technical approach:**
- ARCore for Android
- ARKit for iOS (future)
- 3D asset creation
- Camera permission handling

---

### 12. GLP-1/MEDICATION SUPPORT
**Status:** ⏳ Pending
**Priority:** 🟢 LOW
**Effort:** Low
**Impact:** Access to $50B+ market

**Description:**
Specific support for users on GLP-1 medications (Ozempic, Wegovy, Mounjaro). Growing market segment.

**What to build:**
- [ ] "Medication reminder" habit type
- [ ] Protein tracking habit (critical for GLP-1 users)
- [ ] Enhanced hydration tracking (2-3L recommended on GLP-1)
- [ ] AI coaching aware of medication context
- [ ] Content library: "Habits for GLP-1 Success"
- [ ] Side effect tracking (optional)

**Technical approach:**
- New habit templates
- AI prompt modifications for GLP-1 context
- Educational content creation
- Privacy-sensitive data handling

---

## IMPLEMENTATION ORDER

| Phase | Feature | Timeline |
|-------|---------|----------|
| 1 | Proactive AI Notifications | First |
| 2 | Voice AI Chat Input | Second |
| 3 | Calendar Integration | Third |
| 4 | Predictive At-Risk Alerts | Fourth |
| 5 | Daily Micro-Challenges | Fifth |
| 6 | Auto-Complete from Wearables | Sixth |
| 7 | Virtual Rewards | Seventh |
| 8 | AI Coach Memory | Eighth |
| 9 | Weekly Wellness Score | Ninth |
| 10 | Community Guilds | Tenth |
| 11 | AR Habit Overlays | Eleventh |
| 12 | GLP-1/Medication Support | Twelfth |

---

## NOTES

- Each feature should be fully tested before moving to next
- Premium features should be gated appropriately
- Analytics should track feature adoption
- User feedback collected after each release

---

*Last Updated: February 5, 2026*
