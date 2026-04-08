# Data Security Documentation

## Bilibili Login Data Encryption

### Encryption Method
- AES-256 encrypted storage
- Key managed by Android KeyStore
- Hardware security module protection, app cannot export

### Data Usage
- Used only for calling Bilibili API
- Not uploaded to third-party servers
- Completely saved on local device

### User Control
- Can log out at any time
- Local data cleared after logout
- Data automatically destroyed upon app uninstallation

Detailed technical implementation: [Bilibili Login Security Analysis](bilibili_security_analysis.md)

---

## Local Data Storage

### Stored Content
- Video, subtitle, danmaku files: User-specified folder
- Playback progress: Local database
- Login credentials: Encrypted storage

### Storage Characteristics
- Not synced or backed up to cloud
- User has complete control over storage location
- Can view, manage, or delete at any time

---

## App Permissions

### Storage Permission (Manage All Files)
Used to read local videos, save subtitles/danmaku, video screenshots, download files

### Network Permission
Used for online playback, danmaku matching/download, video download, WebDAV access

---

## Data Flow

### Local Data
```
User Device
├─ Video/Subtitle/Danmaku files (local storage)
├─ Playback progress (local database)
└─ Login credentials (encrypted storage)
```

### Network Requests
```
App → Bilibili API (bangumi features)
App → DanDanPlay API (danmaku matching)
App → WebDAV server (user configured)
```

All network requests initiated only when using corresponding features, no data collection or upload.

---

## Privacy Statement

- Does not collect personal information
- Does not upload user data
- Does not share data with third parties
- Does not track user behavior
- Does not include ads or analytics SDK
- Completely open source, code is auditable

---

## Legal Liability

Download feature for personal learning only. Content copyright belongs to original authors, users must comply with relevant laws and regulations, legal liability borne by users.

---

**Security Feedback**: [GitHub Issues](https://github.com/azxcvn/mpv-android-anime4k/issues)
