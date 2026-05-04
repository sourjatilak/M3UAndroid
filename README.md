<a href="https://github.com/sourjatilak/M3UAndroid">
  <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://socialify.git.ci/sourjatilak/M3UAndroid/image?font=Raleway&forks=1&issues=1&logo=https%3A%2F%2Fraw.githubusercontent.com%2Fsourjatilak%2FM3UAndroid%2Fmaster%2Fapp%2Fsmartphone%2Ficon.png&name=1&pattern=Plus&pulls=1&stargazers=1&theme=Dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://socialify.git.ci/sourjatilak/M3UAndroid/image?font=Raleway&forks=1&issues=1&logo=https%3A%2F%2Fraw.githubusercontent.com%2Fsourjatilak%2FM3UAndroid%2Fmaster%2Fapp%2Fsmartphone%2Ficon.png&name=1&pattern=Plus&pulls=1&stargazers=1&theme=Light" />
   <source src="https://socialify.git.ci/sourjatilak/M3UAndroid/image?font=Raleway&forks=1&issues=1&logo=https%3A%2F%2Fraw.githubusercontent.com%2Fsourjatilak%2FM3UAndroid%2Fmaster%2Fapp%2Fsmartphone%2Ficon.png&name=1&pattern=Plus&pulls=1&stargazers=1&theme=Auto" alt="M3UAndroid" width="640" height="320" />
  </picture>
</a>

![GitHub release](https://img.shields.io/github/v/release/sourjatilak/M3UAndroid?color=blue)
![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen?logo=android)

**M3UAndroid** is a feature-rich streaming media player built with modern Android development practices. Perfect for phones, tablets, and TV devices, delivering a seamless viewing experience powered by Jetpack Compose.

> 🍴 This is a fork of [oxyroid/M3UAndroid](https://github.com/oxyroid/M3UAndroid) with additional feature enhancements.

## ✨ Key Features

- 📺 Adaptive UI for mobile & TV
- 🎭 DLNA casting support
- 🔍 Smart stream analysis
- 🌐 Xtream protocol compatibility
- 📥 Playlist management
- 🚀 Lightweight & ad-free 
- 🇺🇳 Multi-language support

## 🆕 What's New

- 📦 **Universal APK for Phone, Tablet & TV** — A single build now installs and runs on Android phones, tablets, *and* Android TV devices. No more per-form-factor builds.
- 🎁 **First-Run Playlist Import** — On initial launch, the app can auto-subscribe to a bundled set of preset playlists with a live progress dialog. Skips gracefully once complete.
- ⚡ **Xtream Response Cache** — Xtream API responses are cached on disk for 30 minutes, cutting re-subscribe latency and API chatter after relaunches.
- 🏠 **Home Tab Redesigned** — Each subscription is now a single wrapper card. For Xtream, Live / VOD / Series collapse into one card with vertical sub-chips, a clear title, and a gear icon. M3U subscriptions follow the same layout with a single chip.
- ⚙️ **Gear → Configure → Unsubscribe** — Tap the gear on any home card to open its configuration screen. A new "Remove playlist" (or "Unsubscribe server" for Xtream) action at the bottom removes the whole subscription — for Xtream this cleans up Live, VOD, and Series sibling rows in one action.
- ⌨️ **D-Pad On-Screen Keyboard** — New focusable grid keyboard for Android TV / remote-control text entry (URLs, credentials, search).
- ⏭️ **±10 s Player Seek** — The player mask now includes Rewind 10 s and Forward 10 s buttons flanking Play/Pause on VOD content. The refresh icon only appears after a real playback error, not on idle or ended states.
- ⭐ **Smarter Favorite Tab** — The Favorite tab only appears once you have at least one favorited channel, keeping the nav bar tidy.

## 🎞️ Previously

- 🔎 **Global Search** — Dedicated Search tab with cross-playlist results grouped by categories, channels, live streams, and VOD
- 🧠 **Smart Search Bar** — Context-aware search bar that adapts per screen
- 🗂️ **Category Navigation** — Tap a category from search results to jump into the matching playlist
- 🛡️ **Player Memory Leak Fix** — Resolved a ComposeView leak in PlayerActivity during Picture-in-Picture re-attach cycles
- 📭 **Empty State Messages** — Friendly prompts on For You and Favorite tabs when empty

## 📸 Screenshots

| Mobile Experience | TV Experience |
|--------------------|---------------|
| <img src=".github/images/phone/deviceframes.png" width="400"> | <img src=".github/images/tv/playlist.png" width="400"> |
|  | <img src=".github/images/tv/foryou.png" width="400"> |
|  | <img src=".github/images/tv/player.png" width="400"> |

> TV UI is going to be remade in the future...

## ⬇️ Download Now

[![GitHub Release](https://img.shields.io/badge/Download-GitHub%20Release-black?style=for-the-badge&logo=github)](https://github.com/sourjatilak/M3UAndroid/releases/latest)

**Nightly Builds**: [Pre-release Packages](https://nightly.link/sourjatilak/M3UAndroid/workflows/android/master/artifact.zip)

## 🛠 Tech Stack

- 100% Kotlin-first approach
- 🎨 Jetpack Compose UI toolkit
- 🧬 MVVM architecture pattern
- 🚦 Coroutines & Flows
- 🗃️ Room database
- 💉 Hilt dependency injection
- 📦 Modular architecture
- 🎥 ExoPlayer + FFmpeg core

## 🌍 Localization

Help us translate the app! Current support:

| Core Languages | Community Translations |
|----------------|------------------------|
| 🇬🇧 [English](i18n/src/main/res/values) | 🇫🇷 [French](i18n/src/main/res/values-fr-rFR) by [@Gouar](https://github.com/Gouar) |
| 🇨🇳 [Simplified Chinese](i18n/src/main/res/values-zh-rCN) | 🇩🇪 [German](i18n/src/main/res/values-de-rDE) by [@PhynixP](https://github.com/PhynixP) |
|  | 🇮🇩 [Indonesian](i18n/src/main/res/values-id-rID) by [@ca-kraa](https://github.com/ca-kraa) |
|  | 🇮🇹 [Italian](i18n/src/main/res/values-it-rIT) by [@LucaMaroglio](https://github.com/LucaMaroglio) |
|  | 🇧🇷 [Portuguese (BR)](i18n/src/main/res/values-pt-rBR) by [@Suburbanno](https://github.com/Suburbanno) |
|  | 🇷🇴 [Romanian](i18n/src/main/res/values-ro-rRO) by [@iboboc](https://github.com/iboboc) |
|  | 🇪🇸 [Spanish](i18n/src/main/res/values-es-rES) by [@sguinetti](https://github.com/sguinetti) |
|  | 🇪🇸 [Spanish (MX)](i18n/src/main/res/values-es-rMX) by [@sguinetti](https://github.com/sguinetti) |
|  | 🇸🇪 [Swedish](i18n/src/main/res/values-sv-rSE) by [@optiix](https://github.com/optiix) |
|  | 🇹🇷 [Turkish](i18n/src/main/res/values-tr-rTR) by [@patr0nq](https://github.com/patr0nq) |

## 🤝 Contribution

We welcome all contributions! Here's how you can help:
- 🐛 Report bugs via Issues
- 💡 Suggest new features
- 📝 Improve documentation
- 🔧 Submit code changes

## 📜 License

Distributed under the **GPL 3.0**. See [LICENSE](LICENSE) for details.

## 🙏 Acknowledgments

Based on [oxyroid/M3UAndroid](https://github.com/oxyroid/M3UAndroid). Thanks to the original author and contributors.
