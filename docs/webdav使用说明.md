# WebDAV Usage Guide

## Quick Start

### 1. Enter WebDAV Management

Click **cloud icon** on homepage → Enter WebDAV management interface

### 2. Add Account

Click **Add Account**, fill in the following information:
- **Server Address**: e.g. `https://dav.jianguoyun.com/dav/`
- **Display Name**: Custom name
- **Login Mode**: Select account login or anonymous access
- **Account/Password**: WebDAV credentials (Jianguoyun requires app password)

Click **Test Connection** → After success **Save**

### 3. Browse and Play

Click saved account → Browse files → Click on video to play

---

## Technical Implementation

### Core Components

- **Sardine (OkHttpSardine)**: WebDAV client library
- **EncryptedSharedPreferences**: Encrypted credential storage
- **Jetpack Compose**: Modern UI implementation
- **OkHttp**: HTTP request handling

### Code Architecture

```
webdav/
├── WebDavAccountManager.kt    # Multi-account management
├── WebDavClient.kt             # Client wrapper
├── WebDavComposeActivity.kt    # Account management page
├── WebDavBrowserComposeActivity.kt  # File browser
└── WebDavScreen.kt             # UI components
```

---

## Common Issues

**Q: Connection failed?**  
Check: Server address format, account password, network connection

**Q: Jianguoyun password?**  
Use third-party app password, not login password
