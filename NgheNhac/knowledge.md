# NgheNhac — Android Music Player App

A music player for Android (API 28–30, Java) with local playback, streaming, offline cache, Firebase sync, and background playback via Foreground Service.

## Quickstart

- **Build:** `./gradlew assembleDebug`
- **Install:** `./gradlew installDebug`
- **Run tests:** `./gradlew test` (unit) / `./gradlew connectedAndroidTest` (instrumented)
- **Lint:** `./gradlew lint`
- **Clean:** `./gradlew clean`

## Architecture

| Layer | Location | Notes |
|-------|----------|-------|
| App entry | `app/src/main/java/com/example/nghenhac/MainActivity.java` | Uses EdgeToEdge, Material3 DayNight theme |
| UI resources | `app/src/main/res/` | Layouts, drawables, values, themes, XML |
| Config | `app/build.gradle.kts` | Dependencies, SDK versions |
| System design | `MoTaHeThong.md` | Full design doc (Vietnamese) |
| Version catalog | `gradle/libs.versions.toml` | Central dependency versions |

## Key conventions & constraints

- **Language:** Java (not Kotlin)
- **Target API:** 28–30 (Android 9–11), but currently targets SDK 36
- **UI Framework:** Android XML layouts + Material Design 3 (`com.google.android.material`)
- **Build system:** Gradle with Kotlin DSL (`.kts` files), AGP 9.1.1
- **No ViewBinding/DataBinding** in current code — uses `findViewById` pattern
- **Dependencies managed** via `libs.versions.toml` version catalog (not inline versions)
- **ProGuard rules** at `app/proguard-rules.pro` (currently empty)
- **Testing:** JUnit 4.13.2 (unit) + Espresso 3.7.0 (instrumented)
- **Set up:** Project already initialized — open in Android Studio to sync

## Planned stack (from SystemSpecification.md — not yet implemented)

- Local playback: ExoPlayer
- Streaming: ExoPlayer + Retrofit + OkHttp + CacheDataSource
- Local DB: Room + LiveData
- Cloud sync: Firebase Auth + Realtime Database
- Background playback: Foreground Service + MediaSessionCompat + MediaStyle
- Image loading: Glide
- Offline cache: CacheDataSource + full-file download to external cache
- State persistence: SharedPreferences + `onSaveInstanceState()`

## Gotchas

- `compileSdk` uses a version catalog helper (`release(36) { minorApiLevel = 1 }`) — non-standard syntax
- All resource IDs use `R.id.xxx` — no view-binding wrappers yet
- MinSdk 28 means notification channels are required for background playback
- Planned support for Android 10+ scoped storage (`MediaStore` instead of file paths)
- The `MoTaHeThong.md` is the authoritative design document (in Vietnamese)

## Mandatory Agent Workflow

For EVERY task requested by the user:

1. Analyze the request.
2. Identify files that need modification.
3. Implement changes.
4. Run validation when applicable:

    * `./gradlew test`
    * `./gradlew lint`
5. Update `CHANGELOG.md`.
6. Summarize completed work.

Never mark a task as completed before updating `CHANGELOG.md`.

## CHANGELOG Rules

Every task must append an entry to `CHANGELOG.md`.

Required format:

# CHANGELOG

| DateTime         | Request                  | Files Modified                       | Files Created                          | Files Deleted | Changes                                    | Status    |
| ---------------- | ------------------------ | ------------------------------------ | -------------------------------------- | ------------- | ------------------------------------------ | --------- |
| 2026-06-12 16:00 | Tạo màn hình đăng nhập   | AndroidManifest.xml                  | LoginActivity.java, activity_login.xml | -             | Thêm LoginActivity và giao diện đăng nhập  | Completed |
| 2026-06-12 16:30 | Thêm Firebase Auth       | LoginActivity.java, build.gradle.kts | FirebaseAuthManager.java               | -             | Tích hợp đăng nhập Firebase Email/Password | Completed |
| 2026-06-12 17:00 | Sửa lỗi crash khi mở app | MainActivity.java                    | -                                      | -             | Kiểm tra null trước khi khởi tạo Player    | Completed |

## Project Safety Rules

* Do not delete files without explicit user approval.
* Do not change package names unless requested.
* Do not introduce Kotlin code.
* Keep compatibility with Android API 28–30.
* Use version catalog (`libs.versions.toml`) for dependencies.

# Comment Generation Rule

Khi tạo comment cho method Java, luôn sử dụng JavaDoc theo định dạng:

```java
/**
 * Mô tả ngắn gọn chức năng.
 *
 * Nguyên lý:
 * - Giải thích business logic cốt lõi.
 * - Mô tả mục đích xử lý.
 *
 * Luồng xử lý:
 * 1. Bước xử lý đầu tiên.
 * 2. Bước xử lý tiếp theo.
 * 3. Điều kiện rẽ nhánh nếu có.
 * 4. Kết quả cuối cùng.
 *
 * Input:
 * - Liệt kê các tham số quan trọng.
 *
 * Output:
 * - Giá trị trả về hoặc tác động tạo ra.
 *
 * Lưu ý:
 * - Validation.
 * - Transaction.
 * - Exception.
 * - Điều kiện đặc biệt.
 */
```

Yêu cầu:

* Comment phải viết bằng tiếng Việt.
* Đặt ngay phía trên method.
* Tập trung giải thích WHY và FLOW hơn là mô tả từng dòng code.
* Không mô tả những gì đã rõ từ tên biến hoặc tên method.
* Nếu method chứa business logic phức tạp, ưu tiên mô tả luồng xử lý chi tiết theo từng bước.
* Nếu method chỉ là helper đơn giản thì chỉ cần mô tả chức năng và đầu vào/đầu ra.

Nếu bạn đang dùng Cursor hoặc Claude Code, tôi có thể giúp bạn viết một `knowledge.md` hoàn chỉnh để AI **tự động comment toàn bộ project Java/Spring Boot theo chuẩn JavaDoc tiếng Việt**.

