# Bilibili Danmaku Download Principle

## Introduction

Implementing the feature to download danmaku (bullet comments) from Bilibili videos/bangumi, supporting XML format export.

## Core Principles

### 1. Danmaku API

Bilibili provides two sets of danmaku APIs:

#### Segmented Danmaku API (Primary)
```
https://api.bilibili.com/x/v2/dm/web/view?type=1&oid={cid}
https://api.bilibili.com/x/v2/dm/web/seg.so?type=1&oid={cid}&segment_index={index}
```

- Format: Protobuf binary format
- Segmentation: Long videos are divided into segments (approximately 6 minutes each)
- Can retrieve complete danmaku data

#### Regular Danmaku API (Fallback)
```
https://comment.bilibili.com/{cid}.xml
```

- Purpose: Backup when segmented API fails
- Format: Deflate compressed XML
- Limitation: May be incomplete

### 2. Download Flow

```
User inputs video link
    ↓
Extract CID (Video ID)
    ↓
Get danmaku metadata (total segments)
    ↓
Concurrent download all segments
    ↓
Parse Protobuf data
    ↓
Merge, deduplicate, sort
    ↓
Convert to XML format
    ↓
Save to local storage
```

### 3. Performance Optimization

Using concurrent download strategy:

```kotlin
// Concurrent download of all segments
(1..totalSegments).map { segmentIndex ->
    async { downloadSegment(segmentIndex) }
}.awaitAll()
```

### 4. Cookie Support

Cookie functions:
- Get premium member exclusive danmaku
- Increase API call rate limit
- Access content requiring login

Implementation:
```kotlin
// Automatically include user login Cookie
builder.addHeader("Cookie", authManager.getCookieString())
```

## Supported Features

- Regular video danmaku
- Bangumi episode danmaku
- Batch download for entire bangumi seasons
- Short link parsing
- Login state (Cookie)
- Concurrent download

## Key Parameters

- CID: Unique identifier for video, used to retrieve danmaku
- Segment index: Segment number starting from 1
- Protobuf: Google's binary serialization format, requires manual parsing

## XML Format

Generated danmaku XML format:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<i>
  <chatserver>chat.bilibili.com</chatserver>
  <chatid>0</chatid>
  <d p="time,type,size,color,timestamp,pool,user_hash,danmaku_id">Danmaku content</d>
  <d p="5.234,1,25,16777215,1699999999,0,abc123,12345">This is a danmaku</d>
</i>
```

p attribute explanation:
1. Time (seconds)
2. Type (1-3 scrolling, 4 bottom, 5 top)
3. Font size (18/25 etc.)
4. Color (decimal RGB)
5. Sent timestamp
6. Danmaku pool (0 regular)
7. User ID hash
8. Danmaku ID
