# Detailed Feature Specifications

This document provides detailed descriptions of all features in Meow Player.

---

## 🎬 Video Playback

### Supported Formats

Supports all mainstream video formats, including but not limited to:
- MP4, MKV, AVI, FLV
- WEBM, MOV, WMV
- TS, M2TS, RMVB
- And other formats supported by libmpv

### Playback Controls

- Fast forward/rewind
- Playback speed (0.25x - 4.0x)
- Frame step forward/backward
- AB loop playback
- Playback progress save and resume

---

## 🌐 Web Video Sniffing

### Feature Description

Built-in WebView browser for sniffing video resources from web pages.

### Features

- Auto-detect video streams in web pages
- Intelligently select best quality
- One-click playback of sniffed videos
- Support for multiple web video protocols

---

## 📺 Bilibili Features

### Bangumi Support

Login to Bilibili account for online bangumi playback.

**Feature Highlights:**
- Bangumi search and browse
- Bangumi episode list
- Smooth online playback
- Premium bangumi support (requires premium account login)

**Technical Documentation:**
- [Login Implementation](bilibili_login.md)
- [Bangumi Parsing Principle](bilibili_bangumi.md)

### Video/Bangumi Download

Download Bilibili videos and bangumi to local storage.

**Feature Highlights:**
- Support full URL, short links (b23.tv), text-embedded share links
- Auto-parse video information, display title, cover, etc.
- Bangumi supports multi-episode batch download
- Automatic audio-video merge to MP4 format
- Support pause, resume, cancel downloads
- Real-time download progress display
- Download history management

**Technical Documentation:**
- [Download Implementation Principle](bilibili_download_principle.md)

⚠️ **Important**: Video download feature for personal learning only, commercial use strictly prohibited. Downloaded content copyright belongs to original authors.

---

## ☁️ WebDAV Network Storage

### Feature Description

Connect to WebDAV server, directly stream cloud video files online without downloading to local.

### Features

- Support multiple WebDAV server configurations
- Folder browsing
- Online streaming playback
- Support basic authentication and digest authentication
- HTTPS encrypted transmission support

**Usage Documentation:**
- [WebDAV Usage Guide](webdav使用说明.md)

---

## 📂 Playlists

### Feature Highlights

- Auto-scan videos in local folders
- Support multiple sorting methods:
  - Sort by filename
  - Sort by modification time
  - Sort by file size
- Video categorization management
- Recently played list
- Playback history

---

## 📝 Subtitle Features

### Subtitle Support

**Embedded Subtitles:**
- Auto-recognize embedded subtitle tracks
- Multi-subtitle track switching
- Support ASS, SSA, SRT formats, etc.

**External Subtitles:**
- Manual import external subtitle files
- Auto-load same-name subtitles in same folder
- Support subtitle search and selection

### Subtitle Adjustment

- Subtitle position adjustment (move up/down)
- Subtitle size adjustment
- Subtitle delay adjustment (sync with video)
- Subtitle color and style customization

---

## 🔊 Audio Features

### Audio Tracks

- Multi-audio track video support
- Track switching
- Track information display (language, codec, etc.)

### Volume Control

- Volume boost (exceed system volume limit)
- Fine volume adjustment (0.1% precision)
- Volume boost toggle
- Volume memory function

---

## 💬 Danmaku Features

### Local Danmaku

- Import local XML format danmaku files
- Auto-associate with videos
- Danmaku file management

### DanDanPlay Danmaku Matching

**Feature Highlights:**
- Auto-match local video files to DanDanPlay danmaku library
- Smart filename recognition, support anime bangumi matching
- One-click download matched danmaku
- Support manual search and selection of match results
- Batch matching and download

### Bilibili Danmaku Download

**Feature Highlights:**
- Use Bilibili segmented danmaku API to get complete danmaku data
- Support regular video and bangumi danmaku download
- Bangumi supports entire season batch danmaku download
- Concurrent download technology, 10-20x speed improvement
- Auto-include login Cookie to get premium member exclusive danmaku

**Technical Documentation:**
- [Danmaku Download Principle](bilibili_danmaku_download.md)

### Danmaku Style & Display

**Style Customization:**
- Danmaku size adjustment
- Danmaku speed adjustment
- Danmaku transparency
- Danmaku outline thickness
- Danmaku font settings

**Display Control:**
- Danmaku track management
- Show/hide different types of danmaku (scrolling, top, bottom)
- Danmaku density control
- Danmaku blocking rules

**Advanced Features:**
- Auto-remember danmaku file and display state
- High refresh rate screen adaptation (90Hz/120Hz/144Hz support)
- Precise danmaku-video progress sync
- Danmaku sync on chapter jump

---

## 👆 Gesture Controls

### Gesture Operations

- **Left swipe**: Adjust screen brightness
- **Right swipe**: Adjust volume
- **Horizontal swipe**: Fast forward/rewind (thumbnail preview display)
- **Double tap**: Pause/play (changeable to fast forward/rewind in settings)
- **Long press**: Speed playback (effective during press)
- **Progress bar drag**: Precise playback position

### Customization

- Gesture sensitivity adjustment
- Double tap behavior customization
- Gesture function toggle

---

## 🎨 Video Super-Resolution

### Anime4K Upscaling

Integrated Anime4K real-time super-resolution algorithm, optimized for anime videos.

**Feature Highlights:**
- Real-time video upscaling
- Multiple upscaling algorithms selectable
- Upscaling strength adjustment
- Performance mode switching (balance quality vs performance)

**Use Cases:**
- Low-resolution anime video enhancement
- Older bangumi quality optimization
- Enhanced viewing experience

⚠️ **Note**: Upscaling requires high device performance, recommended for mid-to-high-end devices.

---

## 🖼️ Other Features

### Screenshot Function

- Video screenshot save
- Auto-save to gallery
- Screenshot file naming rules
- Screenshot quality settings

### Playback Memory

- Auto-save playback progress
- Auto-resume on next open
- Precise playback position recording
- Optional memory function toggle

---

## 🔍 More Features

This project is continuously updated, more features coming soon!

If you have feature suggestions or requests, welcome to [submit an Issue](https://github.com/azxcvn/mpv-android-anime4k/issues).
