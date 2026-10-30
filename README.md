# 🎵 Skim Music

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform: Android" />
  <img src="https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin Multiplatform" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Engine-Media3%20ExoPlayer-E53935?style=for-the-badge&logo=google&logoColor=white" alt="Media3 ExoPlayer" />
  <img src="https://img.shields.io/badge/Design-Apple%20Liquid%20Glass-black?style=for-the-badge&logo=apple&logoColor=white" alt="Liquid Glass UI" />
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge" alt="License: GPL-3.0" />
</p>

<p align="center">
  <b>A modern, immersive, vertical-feed music streaming experience.</b><br/>
  Blending the addictive discovery of TikTok and Resso with the aesthetic sophistication of Apple's Liquid Glass and the power of YouTube Music.
</p>

---

## 🌟 Overview

**Skim Music** reimagines music discovery for the modern mobile generation. Instead of navigating static albums and traditional queues, Skim places music into a fluid, vertical feed where every swipe reveals a new track with instant previews, synced word-by-word karaoke lyrics, and one-tap chorus jumps.

Designed with **Apple Liquid Glass** aesthetics—featuring frosted specular highlights, subtle ambient glass surfaces, and responsive haptic interactions—Skim stays lightweight and battery-efficient while delivering a fluid 60–120 FPS experience.

---

## ✨ Key Features

### 📱 1. Vertical Discovery Feed
* **Full-Screen Continuous Swipe:** Browse music intuitively like TikTok, Resso, or YouTube Shorts.
* **🎯 Hook Jump:** Don't wait for build-ups—tap the Hook button to immediately jump straight to the song's most iconic chorus or climax.
* **Synced Karaoke Lyrics:** Real-time, word-by-word highlighted lyrics overlaid seamlessly over album artwork.
* **Double-Tap to Like:** Quick heart animation with instant library and playlist synchronization.
* **Clean Player Screen:** Stripped of unnecessary clutter, with secondary controls tucked neatly into an expansive 3-dots options sheet.

### 🧭 2. Three Dedicated Navigation Tabs
* **🔥 For You:** Algorithmic feed curated to your daily listening habits, recent favorites, and trending tracks.
* **🌍 Explore:** Deep dive into top charts, global new releases, genres, and curated YouTube Music categories.
* **✨ Moods & Vibes:** Instant mood selectors (Chill, Workout, Focus, Party, Midnight, Commute) rendered in vibrant Liquid Glass banners.

### 💿 3. Self-Healing Playback & Tap-to-Reload Vinyl Disc
* **Visual Turntable:** Bottom-right rotating vinyl disc that animates in sync with playback status.
* **Fail-Safe Tap to Reload:** If poor network conditions or expired YouTube streaming tokens cause playback to stall, simply **tap the rotating vinyl disc**. The app immediately re-fetches a clean streaming token, purges buffered cache, and restarts the song without interrupting your session.
* **Atomic Queue Handling:** Powered by Media3 ExoPlayer with race-condition prevention, eliminating mid-swipe audio freezes or timer desync.
* **Synchronized Session Resumption:** Cold-starting the app gracefully detects active background playback and synchronizes the cover art, title, and lyrics with zero desynchronization.

### 💎 4. Apple Liquid Glass UI Design
* **Translucent Frosted Layers:** Multi-layered specular gradients, diffused rim lighting, and glass-like blur cards.
* **GPU-Optimized:** Built with native Compose canvas shaders and linear alpha blends rather than expensive multi-pass Gaussian blurs, preserving device battery and thermals.

### 🎛️ 5. Landscape & Tablet Dual-Pane Player
* **Optimized Horizontal Experience:** Dual-pane layout featuring a large turntable vinyl player on the left and full-screen synchronized lyrics on the right.
* **Lyric Timing Calibration:** Built-in micro-adjust controls (`-0.5s`, `+0.5s`, and reset) to fine-tune karaoke lyric timing on the fly.

### 📲 6. Home Screen Widgets (AndroidX Glance)
* **Turntable Player Widget:** Interactive rotating disc home screen widget with quick play/pause and skip controls.
* **Recent Playlists Widget:** One-tap home screen access to your top playlists and saved albums.
* **Listening Insights Widget:** 30-day listening metrics, total hours streamed, and top artist breakdown right on your launcher.

### 🔄 7. Playlist Migration & Universal Importer
* **Cross-Platform Conversion:** Import playlists from **Spotify URLs**, **YouTube links**, or raw plain-text tracklists (`Track Name - Artist`).
* **Automated Catalog Matching:** Automatically resolves Spotify metadata against YouTube Music's high-bitrate audio catalog and saves it into your local Room database.
* **Offline Backup:** Export and import playlist backups in JSON format anytime.

### 🧠 8. On-Device AI Music Taste Engine
* **100% Offline & Private:** Zero OpenAI/Gemini API keys or external server dependencies.
* **Behavioral Listening Analysis:** Evaluates skip frequencies, completion rates, replay counts, and time-of-day listening habits.
* **Persona Archetypes & Variety Score:** Generates dynamic personas (e.g., *"Late-Night Melancholic Explorer"*, *"High-Energy Groove Hunter"*) alongside listening entropy metrics and tailored recommendations.

### 🛡️ 9. SponsorBlock & Pure Audio Focus
* **Automatic Segment Skipping:** Bypasses non-music intros, promotional sponsored shoutouts, outro cards, and silence interludes via SponsorBlock integration.
* **System-Level Media Controls:** Native Android 13+ Notification Media Player with seekable scrubber, lock screen wallpaper, and Bluetooth/Android Auto metadata.
* **Audio Focus Management:** Graceful audio ducking during GPS navigation and auto-pause on headphone disconnect.

### 📻 10. Last.fm Scrobbling
* **Automated Scrobble:** Automatically scrobbles played tracks at the 50% or 4-minute threshold.
* **Real-Time Now-Playing:** Broadcasts current track status and syncs liked songs directly with your Last.fm profile.

---

## 🏗️ Architecture & Tech Stack

```
Skim/
├── app/                  # Android application module & MainActivity
│   └── src/main/
│       ├── java/         # Application entry point & Glance Widgets
│       └── res/          # Android drawables, layouts, and widget metadata
├── core/
│   ├── model/            # Domain models (Track, Artist, Playlist, Lyrics, Mood)
│   ├── ui/               # Design system, Liquid Glass modifiers, themes, typography
│   ├── player/           # ExoPlayer & Media3 MediaSessionService implementation
│   ├── database/         # Local Room Database (History, Playlists, AI Stats)
│   ├── network/          # Ktor client & YouTube Music extraction engine
│   └── data/             # Repositories, SponsorBlock, Last.fm, & AI Taste Engine
└── build.gradle.kts      # Multi-module Gradle configuration
```

| Layer | Technology |
|---|---|
| **Language** | [Kotlin](https://kotlinlang.org/) (Coroutines, Flow, Serialization) |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) & [Material 3](https://m3.material.io/) |
| **Audio Engine** | [AndroidX Media3 ExoPlayer](https://developer.android.com/media/media3) + MediaSession |
| **Widgets** | [AndroidX Glance](https://developer.android.com/jetpack/compose/glance) (App Widgets) |
| **Local Storage** | [Room Database](https://developer.android.com/training/data-storage/room) with SQLite |
| **Dependency Injection** | [Koin](https://insert-koin.io/) |
| **Networking** | [Ktor Client](https://ktor.io/) & [Coil](https://coil-kt.github.io/coil/) (Image Loading) |
| **Integrations** | YouTube Music (Innertube), SponsorBlock API, Last.fm API |

---


## 🎮 Gestures & Shortcuts Quick Reference

| Gesture / Action | Result |
|---|---|
| **Swipe Up / Down** | Advance to next / previous song in the feed |
| **Double Tap Card** | Like track and add to Favorites (plays heart animation) |
| **Tap Rotating Vinyl** | **Self-healing reload:** Refreshes stream tokens and resumes audio |
| **Tap 🎯 Hook Button** | Immediately seek to the song's chorus / hook |
| **Rotate to Landscape** | Launches Dual-Pane Player with lyrics timing controls |
| **Tap 3-Dots (...)** | Opens full track details, album info, share, and playlist controls |

---

## 🤝 Contributing

Contributions, feature suggestions, and bug reports are welcome!
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the **GNU General Public License v3.0**. See `LICENSE` for more information.

---

<p align="center">
  Crafted with ❤️ for music lovers everywhere by Krishhh....
</p>
