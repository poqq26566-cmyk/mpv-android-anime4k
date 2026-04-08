# Third-Party API Usage Documentation

This application uses the following third-party API services.

---

## Bilibili

Used for login, bangumi playback, danmaku download, video download and other features.

### API List

- **Login API**: `https://passport.bilibili.com/x/passport-login/web/qrcode/*`
- **Bangumi Info API**: `https://api.bilibili.com/pgc/view/web/season`
- **Bangumi Playback API**: `https://api.bilibili.com/pgc/player/web/playurl`
- **Danmaku Download API**: `https://api.bilibili.com/x/v1/dm/list.so`
- **Video Info API**: `https://api.bilibili.com/x/web-interface/view`
- **Video Download API**: `https://api.bilibili.com/x/player/playurl`
- **Bangumi Download API**: `https://api.bilibili.com/pgc/player/web/playurl`

### Disclaimer

This application has no official affiliation with Bilibili, only uses its public APIs.

---

## DanDanPlay

Used for local video danmaku matching and download features.

### API List

- **Danmaku Matching API**: `https://api.dandanplay.net/api/v2/match`
- **Danmaku Search API**: `https://api.dandanplay.net/api/v2/search/episodes`
- **Danmaku Download API**: `https://api.dandanplay.net/api/v2/comment/*`

### Configuration Instructions

Using DanDanPlay API requires configuring AppId and AppSecret in `local.properties`.

Steps to get credentials:
1. Go to [DanDanPlay Open Platform](https://www.dandanplay.com/)
2. Register and apply for AppId and AppSecret
3. Fill credentials into `local.properties` file in project root directory

---

## Wyzie Subs

Used for online search and download of subtitle files for movies and TV shows.

### API List

- **Media Search API**: `https://sub.wyzie.io/api/tmdb/search`
- **Subtitle Search API**: `https://sub.wyzie.io/search`

### Configuration Instructions

Using Wyzie API requires configuring API Key in `local.properties`.

Steps to get API Key:
1. Go to [Wyzie Key Application Page](https://sub.wyzie.io/redeem)
2. Apply for API Key for free
3. Fill key into `local.properties` file in project root directory

### Disclaimer

This application has no official affiliation with Wyzie, only uses its public API services.
