# Bilibili Bangumi Parsing and Playback

## Parsing Flow

### 1. Extract ID

Extract Season ID or Episode ID from the input link

```
Supported formats:
https://www.bilibili.com/bangumi/play/ss12345  -> season_id: 12345
https://www.bilibili.com/bangumi/play/ep67890  -> ep_id: 67890
https://b23.tv/xxxxx                           -> Requires redirect to get full link
```

```kotlin
val ssPattern = "ss(\\d+)".toRegex()
val epPattern = "ep(\\d+)".toRegex()
val seasonId = ssPattern.find(url)?.groupValues?.get(1)
val episodeId = epPattern.find(url)?.groupValues?.get(1)
```

### 2. Get Bangumi Information

```
API: https://api.bilibili.com/pgc/view/web/season
Parameters: season_id or ep_id
Request Headers:
  User-Agent: Browser identifier
  Referer: https://www.bilibili.com
  Cookie: Login credentials (optional)
```

Key response fields:

```json
{
  "result": {
    "season_id": 12345,
    "title": "Bangumi Title",
    "episodes": [
      {
        "id": 67890,
        "aid": 11111,  // AV number
        "cid": 22222,  // CID number
        "title": "Episode 1"
      }
    ]
  }
}
```

### 3. Get Playback URL

```
API: https://api.bilibili.com/pgc/player/web/playurl
Parameters:
  avid: Video AV number
  cid: Video CID number
  qn: Quality code (64=720P, 80=1080P, 112=1080P+, 116=1080P60)
  fnval: Format (1=MP4, 16=DASH)
Request Headers:
  Cookie: Login credentials (required for premium content)
  User-Agent: Browser identifier
  Referer: https://www.bilibili.com
```

**MP4 format response:**

```json
{
  "data": {
    "durl": [
      {"url": "https://..."}  // Direct playback URL
    ]
  }
}
```

**DASH format response:**

```json
{
  "data": {
    "dash": {
      "video": [{"base_url": "https://..."}],  // Video stream
      "audio": [{"base_url": "https://..."}]   // Audio stream
    }
  }
}
```

### 4. Play Video

Using libmpv player

```kotlin
// DASH format (separate video and audio)
mpv.command(arrayOf("loadfile", videoUrl))
mpv.command(arrayOf("audio-add", audioUrl))
mpv.setOptionString("http-header-fields", 
    "Referer: https://www.bilibili.com,User-Agent: ...")

// MP4 format (single file)
mpv.command(arrayOf("loadfile", videoUrl))
mpv.setOptionString("http-header-fields", 
    "Referer: https://www.bilibili.com,User-Agent: ...")
```

---

## Technical Details

### Request Header Requirements

Following headers are required, otherwise returns 403:

- `Referer: https://www.bilibili.com`
- `User-Agent: Mozilla/5.0 ...`

### Quality & Membership

```
Quality code mapping:
16  = 360P
32  = 480P
64  = 720P
80  = 1080P
112 = 1080P+
116 = 1080P60
120 = 4K

Permission requirements:
Not logged in     -> Max 480P
Regular user      -> Max 720P
Premium member    -> 1080P, 1080P+, 1080P60
Annual premium    -> 4K (when video supports)
```

### DASH vs MP4

**DASH:**
- Separate video and audio
- Supports high quality (1080P+)
- Requires player to merge streams

**MP4:**
- Single file direct link
- Max 720P
- Better compatibility

---

## Common Issues

**Q: Playback failed?**  
Check: Membership permissions, regional restrictions, Cookie validity

**Q: Low quality?**  
Reason: Not logged in, non-member, video source limitation

**Q: Playback lag?**  
Lower quality or check network connection
