# Bilibili Login Implementation

## Login Flow

### 1. Get QR Code

```
Request: https://passport.bilibili.com/x/passport-login/web/qrcode/generate
Returns:
  - url: QR code link
  - qrcode_key: For polling login status
```

### 2. Display QR Code

Use ZXing library to convert url to QR code image and display

```kotlin
val qrCodeWriter = QRCodeWriter()
val bitMatrix = qrCodeWriter.encode(
    url,
    BarcodeFormat.QR_CODE,
    size,
    size
)
```

### 3. Poll Login Status

Query login status every 3 seconds

```
Request: https://passport.bilibili.com/x/passport-login/web/qrcode/poll
Parameters: qrcode_key

Status codes:
  86101 - Not scanned
  86090 - Scanned, awaiting confirmation
  0 - Login successful
  86038 - QR code expired
```

```kotlin
while (isActive) {
    val result = authManager.pollQRCodeStatus(qrcodeKey)
    when (result.code) {
        0 -> break // Login successful
        86101 -> {} // Continue waiting
        86090 -> {} // Prompt user to confirm
        86038 -> break // QR code expired
    }
    delay(3000)
}
```

### 4. Save Login Credentials

Extract key Cookie fields after successful login and encrypt for storage

```
Key fields:
  - SESSDATA
  - bili_jct
  - DedeUserID
  - DedeUserID__ckMd5
  - buvid3

Encryption: AES-256
Storage: EncryptedSharedPreferences
```

---

## Technical Implementation

### Encrypted Storage

```kotlin
// Using Android Jetpack Security library
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "bilibili_auth",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

### Using Credentials

Add saved Cookie to request header when calling Bilibili API

```kotlin
val cookies = "SESSDATA=$sessdata; bili_jct=$biliJct; ..."
httpClient.newCall(
    Request.Builder()
        .url(apiUrl)
        .addHeader("Cookie", cookies)
        .build()
)
```

---

## Common Issues

**Q: Credential expiration?**  
Approximately one month, requires re-login after expiration

**Q: Waiting for confirmation after scanning?**  
App obtains status via polling, recommended to wait for "Scanned" display before confirming
