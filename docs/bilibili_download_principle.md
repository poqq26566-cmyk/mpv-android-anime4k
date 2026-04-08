# Bilibili Video/Bangumi Download Implementation Principle

## I. Core Flow

```
User Input Link → Parse Link → Get Video Info → Download Audio/Video Streams → Merge Files → Complete
```

## II. Key Steps Explained

### 1. Link Parsing
- **Short Link**Handle**: `b23.tv` short links get real URL via HTTP redirect
- **ID Extraction**: Extract BV number (e.g., `BV1eRYfzxEJj`) or AV number (e.g., `av123456`) from URL
- **Type Detection**: Distinguish between regular videos (`/video/`) and bangumi (`/bangumi/play/`)

### 2. Get Video Details
Use Bilibili API to get basic video information:
```
API: https://api.bilibili.com/x/web-interface/view
Parameters: bvid=BV number or aid=AV number
Returns: Title, cid (content ID), duration, etc.
```

### 3. Get Download URLs
Get actual audio/video stream URLs using cid:
```
API: https://api.bilibili.com/x/player/playurl
Parameters: bvid/aid + cid + qn(quality) + fnval=4048(DASH format)
Returns: Audio stream URL + Video stream URL
```

**Key Points**:
- Bilibili uses DASH format, audio and video are separate
- Requires Cookie (SESSDATA) to access HD videos
- fnval=4048 indicates requesting DASH format streaming media

### 4. Segmented Download
Audio and video downloaded separately, using multi-threading for speed:
- Use `Range` header for resumable downloads
- Each segment downloaded independently, can retry on failure
- Real-time download progress updates

### 5. File Merging
Use Android's `MediaMuxer` to merge audio and video:
```kotlin
1. Create MediaExtractor to read audio and video separately
2. Create MediaMuxer as output
3. Add audio and video tracks
4. Write data frame by frame
5. Generate final MP4 file
```

## III. Technical Details

### BV/AV Number Conversion
- **BV Number**: Base58 encoded, e.g., `BV1eRYfzxEJj` (12 characters)
- **AV Number**: Pure digits, e.g., `av123456`
- Algorithm based on Bilibili's official formula, using position mapping and XOR operations

### Cookie Management
- Stored in `SharedPreferences`
- Key field: `SESSDATA` (login credentials)
- Must include complete Cookie in every API request

### Special Bangumi Handling
- Need to get complete episode list
- Query using `season_id` or `ep_id`
- API: `https://api.bilibili.com/pgc/view/web/season`

### Storage Path Selection
- Use `DocumentFile` API to access external storage
- Support SAF (Storage Access Framework)
- Persist URI permissions to avoid repeated authorization

## IV. Code Structure

```
download/
├── BilibiliDownloadManager.kt    # Core download logic
├── BilibiliDownloadViewModel.kt  # UI state management
├── DownloadItem.kt               # Download item data class
└── MediaParseResult.kt           # Parse result

DownloadActivity.kt                # Download interface (Compose)
utils/CookieManager.kt             # Cookie storage management
```

## V. User Workflow

1. **Input Link**: Supports full URL, short link, text-embedded share link
2. **Parse Link**: Auto-detect video/bangumi type
3. **Select Episodes**: Multi-select episodes for bangumi download
4. **Select Path**: First-time use requires selecting download directory
5. **Start Download**: Background download, supports pause/resume/cancel
6. **Auto Merge**: Automatically merge audio/video after download completes

## VI. Important Notes

- **Obfuscation Protection**: Release builds need keep rules in ProGuard
- **Permission Requirements**: Requires network and storage permissions
- **API Limitations**: Download speed may be limited by Bilibili servers
- **Copyright Notice**: Downloaded content for personal learning use only

---

**Implementation Language**: Kotlin + Jetpack Compose  
**Main Dependencies**: OkHttp, Coroutines, MediaMuxer
