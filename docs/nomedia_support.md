# .nomedia Support Functionality

## Feature Overview

Complete support for `.nomedia` files has been added to the player project. When a `.nomedia` file exists in a folder, videos in that folder and its subfolders will not be scanned and displayed.

## Implementation Details

### 1. New Utility Class: `NoMediaChecker.kt`

Location: `app/src/main/java/com/fam4k007/videoplayer/utils/NoMediaChecker.kt`

Provides three main methods:
- `containsNoMedia(path: String?)`: Check if path or any of its parent directories contains .nomedia file
- `folderHasNoMedia(folderPath: String?)`: Check if specified folder directly contains .nomedia file
- `fileInNoMediaFolder(filePath: String?)`: Check if file's folder or its parent directories contain .nomedia file

### 2. Modified Files

#### VideoBrowserActivity.kt
- Added .nomedia detection in `scanVideoFiles()` method
- Automatically skips folders containing .nomedia during video scan

#### VideoListActivity.kt
- Added .nomedia detection in `refreshVideoList()` method
- Added .nomedia detection in `scanFolderVideos()` method
- Skips folders containing .nomedia when refreshing video list

#### VideoScanner.kt
- Added .nomedia filtering in `getAllVideos()` async method
- Added .nomedia filtering in `getAllVideosSync()` sync method

#### SeriesManager.kt
- Added .nomedia detection in `getVideosFromMediaStore()` method
- Added .nomedia detection in `getVideosFromFile()` method
- Continuous playback automatically skips folders containing .nomedia

## Usage Instructions

### How to Mark Folders as Non-Scannable

1. Create an empty file named `.nomedia` in any folder (note: filename starts with a dot)
2. Videos in that folder and all its subfolders will not be scanned and displayed by the player
3. Takes effect after rescanning the video list

### Testing Steps

1. Create `.nomedia` file in a folder containing videos:
   ```bash
   # Using file manager or command line
   touch /sdcard/Videos/TestFolder/.nomedia
   ```

2. Pull down to refresh video list in the player

3. Verify that videos in that folder no longer appear

4. After deleting the `.nomedia` file, refresh again - videos should reappear

## Technical Features

- **Efficient Detection**: Detection only during video scan, no impact on normal playback performance
- **Recursive Check**: Supports checking parent directory hierarchy, complies with Android system behavior
- **Logging**: Skipped videos logged in Logcat for easy debugging
- **Good Compatibility**: Compatible with different file access methods (MediaStore, DocumentFile, File)

## Important Notes

1. `.nomedia` file is an Android system standard convention, system media scanner also follows this rule
2. This feature is mainly used to exclude private files or temporary files from media library display
3. Files already indexed by MediaStore may require app restart or rescan to fully take effect
4. After deleting `.nomedia` file, need to manually refresh video list
