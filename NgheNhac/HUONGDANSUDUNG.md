# 🎵 HƯỚNG DẪN SỬ DỤNG NGHE NHAC — A-Z

> **Phiên bản:** 1.0  
> **Nền tảng:** Android 9–15 (API 28–36)  
> **Ngôn ngữ:** Java  
> **Giao diện:** Material Design 3

---

## 📑 MỤC LỤC

1. [Giới thiệu](#1-giới-thiệu)
2. [Yêu cầu hệ thống](#2-yêu-cầu-hệ-thống)
3. [Cài đặt ứng dụng](#3-cài-đặt-ứng-dụng)
4. [Màn hình chính](#4-màn-hình-chính)
5. [Quản lý thư viện nhạc](#5-quản-lý-thư-viện-nhạc)
6. [Phát nhạc](#6-phát-nhạc)
7. [Playlist](#7-playlist)
8. [Tìm kiếm](#8-tìm-kiếm)
9. [Equalizer & Hiệu ứng âm thanh](#9-equalizer--hiệu-ứng-âm-thanh)
10. [Hẹn giờ tắt nhạc](#10-hẹn-giờ-tắt-nhạc)
11. [Đăng nhập & Đồng bộ](#11-đăng-nhập--đồng-bộ)
12. [Import/Export Playlist](#12-importexport-playlist)
13. [Tải nhạc offline](#13-tải-nhạc-offline)
14. [Cài đặt](#14-cài-đặt)
15. [Khắc phục sự cố thường gặp](#15-khắc-phục-sự-cố-thường-gặp)

---

## 1. GIỚI THIỆU

**NgheNhac** là ứng dụng nghe nhạc mã nguồn mở dành cho Android, cho phép bạn:

- 🎧 **Nghe nhạc từ thiết bị** — quét tất cả bài hát được lưu trên máy
- 🌐 **Streaming trực tuyến** — phát nhạc qua Internet (nếu có API)
- 📋 **Quản lý Playlist** — tạo, chỉnh sửa, xoá playlist
- 🔄 **Đồng bộ đám mây** — đồng bộ danh sách yêu thích qua Firebase
- 🌙 **Phát nền** — nghe nhạc khi khoá màn hình hoặc dùng app khác
- 🔊 **Equalizer** — tuỳ chỉnh âm thanh với các preset và band slider
- ⏰ **Hẹn giờ tắt nhạc** — tự động dừng sau N phút
- 📤 **Import/Export Playlist** — chia sẻ playlist dạng file M3U hoặc XML

---

## 2. YÊU CẦU HỆ THỐNG

| Yêu cầu | Chi tiết |
|---------|----------|
| **Phiên bản Android** | 9.0 (API 28) trở lên |
| **RAM tối thiểu** | 2 GB |
| **Dung lượng trống** | ~50 MB cho ứng dụng + thêm cho cache nhạc |
| **Kết nối Internet** | Cần cho streaming, đăng nhập, đồng bộ |
| **Quyền cần thiết** | Quyền đọc nhạc (tự động yêu cầu khi mở app) |

---

## 3. CÀI ĐẶT ỨNG DỤNG

### Cách 1: Cài file APK

1. Tải file `NgheNhac.apk` về máy
2. Mở file APK → nhấn **"Cài đặt"**
3. Nếu yêu cầu "Cho phép cài đặt từ nguồn không xác định" → vào **Cài đặt → Bật**
4. Sau khi cài xong → nhấn **"Mở"**

### Cách 2: Cài bằng Android Studio (dành cho lập trình viên)

```bash
# Mở terminal tại thư mục dự án
cd NgheNhac

# Build và cài lên thiết bị
./gradlew installDebug
```

### Lần đầu chạy ứng dụng

1. Mở ứng dụng → màn hình **Splash Screen** hiện ra (logo NgheNhac)
2. Hệ thống yêu cầu **quyền đọc nhạc** → nhấn **"Cho phép"**
   > Nếu từ chối, bạn có thể vào **Cài đặt ứng dụng** để bật sau
3. Ứng dụng tự động **quét nhạc** từ bộ nhớ thiết bị (có thể mất vài giây)
4. Sau khi quét xong → thư viện nhạc hiển thị đầy đủ

---

## 4. MÀN HÌNH CHÍNH

### Thanh điều hướng dưới (Bottom Navigation)

```
┌──────────────────────────────────┐
│            NgheNhac              │
│                                  │
│     [Nội dung tab hiện tại]      │
│                                  │
│                                  │
│                                  │
│  📚 Thư viện  🔍 Tìm kiếm  ⚙ Cài đặt  │  ← Bottom Navigation
└──────────────────────────────────┘
```

| Biểu tượng | Tab | Chức năng |
|:---------:|:----:|----------|
| 📚 | **Thư viện** | Xem tất cả bài hát, album, nghệ sĩ, playlist |
| 🔍 | **Tìm kiếm** | Tìm bài hát, nghệ sĩ, album |
| ⚙ | **Cài đặt** | Tài khoản, giao diện, xoá cache, giới thiệu |

### Mini Player (thanh phát nhỏ)

Khi có bài hát đang phát, thanh **Mini Player** xuất hiện ở **cuối màn hình**:

```
┌────────────────────────────────────┐
│ [🎵]  Tên bài hát             ⏸ ▶  │  ← Mini Player
│       Tên nghệ sĩ          ⏮    ⏭ │
└────────────────────────────────────┘
```

> **Click vào Mini Player** → mở màn hình phát nhạc toàn màn hình

---

## 5. QUẢN LÝ THƯ VIỆN NHẠC

### 5.1. Tab Bài hát (Songs)

Vào **Thư viện → Bài hát** → danh sách tất cả bài hát trên thiết bị.

- **Click bài hát** → phát nhạc ngay
- **Click ♡** → thêm/xoá khỏi danh sách yêu thích
- **Click ⋮ (More)** → mở menu:
  - **Thêm vào playlist**
  - **Yêu thích/Bỏ thích**

> **Mẹo:** Bài hát đang phát được **đánh dấu chấm xanh** bên trái và background highlight.

### 5.2. Tab Album

Vào **Thư viện → Album** → xem tất cả album dạng lưới 2 cột.

- **Click album** → xem chi tiết album (danh sách bài hát trong album)
- **Click bài hát trong album** → phát toàn bộ album

### 5.3. Tab Nghệ sĩ (Artist)

Vào **Thư viện → Nghệ sĩ** → danh sách nghệ sĩ.

- **Click nghệ sĩ** → phát tất cả bài hát của nghệ sĩ đó

### 5.4. Tab Playlist

Vào **Thư viện → Playlist** → quản lý các playlist của bạn.

- **Click playlist** → mở chi tiết playlist
- **Click ⋮ (More)** → Đổi tên / Xoá playlist
- **Nhấn nút ➕ (FAB)** → tạo playlist mới
- **Nhấn giữ ➕ (FAB)** → import playlist từ file

---

## 6. PHÁT NHẠC

### 6.1. Màn hình phát nhạc (PlayerActivity)

Click vào bài hát hoặc click Mini Player → mở màn hình phát nhạc:

```
┌─────────────────────────────────────┐
│  ← [Back]                    [⋮]   │  ← Menu (Sleep timer, Queue, Equalizer)
│                                     │
│          ┌───────────┐              │
│          │  ẢNH BÌA  │              │  ← Album art (ảnh nền động theo màu)
│          │  ALBUM    │              │
│          └───────────┘              │
│                                     │
│       Tên bài hát                   │
│       Tên nghệ sĩ                   │
│                                     │
│  0:00 ─────●───────────── 4:30      │  ← Seekbar (có thể kéo tua)
│                                     │
│  🔀  ⏮  ▶⏸  ⏭  🔁              │  ← Controls
└─────────────────────────────────────┘
```

### 6.2. Các nút điều khiển

| Nút | Chức năng | Ghi chú |
|:--:|-----------|---------|
| ⏮ | **Bài trước** | Nếu đã phát > 3s thì quay lại đầu bài hiện tại |
| ▶ / ⏸ | **Phát / Tạm dừng** | |
| ⏭ | **Bài tiếp theo** | Chuyển sang bài kế tiếp |
| 🔀 | **Xáo trộn (Shuffle)** | Bật/tắt phát ngẫu nhiên |
| 🔁 | **Lặp lại** | Nhấn nhiều lần: Lặp 1 bài → Lặp tất cả → Tắt lặp |
| ⋮ | **Menu** | Hẹn giờ, Danh sách phát, Equalizer |

### 6.3. Chế độ lặp (Repeat)

| Trạng thái | Mô tả | Icon |
|:----------:|-------|:----:|
| **Tắt** | Phát hết danh sách thì dừng | 🔁 (xám) |
| **Lặp 1 bài** | Lặp lại bài hát hiện tại | 🔁 (màu, số 1) |
| **Lặp tất cả** | Lặp lại toàn bộ danh sách | 🔁 (màu) |

> Nhấn nút 🔁 nhiều lần để chuyển đổi giữa các chế độ.

### 6.4. Phát nhạc nền (Background)

Khi bạn thoát app hoặc khoá màn hình, nhạc **vẫn tiếp tục phát**.

Trên thanh thông báo (Notification) sẽ hiển thị:

```
┌─────────────────────────────────────────┐
│ [▶]  Tên bài hát    ⏮  ▶⏸  ⏭         │
│      Tên nghệ sĩ                       │
└─────────────────────────────────────────┘
```

> Bạn có thể điều khiển play/pause/next/prev ngay từ notification hoặc màn hình khoá.

---

## 7. PLAYLIST

### 7.1. Tạo Playlist mới

1. Vào **Thư viện → Playlist**
2. Nhấn nút **➕** (góc dưới phải)
3. Nhập **tên playlist**
4. Nhấn **"Tạo"**

### 7.2. Thêm bài hát vào Playlist

**Cách 1:** Khi click ⋮ (More) trên bài hát → **Thêm vào playlist** → chọn playlist

**Cách 2:** Trong màn hình chi tiết playlist → nhấn **➕** → chọn bài hát → nhấn **"Thêm"**

### 7.3. Xoá bài hát khỏi Playlist

Trong màn hình chi tiết playlist:
- **Click bài hát** → xác nhận xoá
- Hoặc nhấn giữ bài hát → chọn "Xoá"

### 7.4. Đổi tên / Xoá Playlist

1. Vào **Thư viện → Playlist**
2. Click **⋮ (More)** trên playlist cần đổi/xoá
3. Chọn **"Đổi tên"** hoặc **"Xoá"**

### 7.5. Xuất Playlist ra file

1. Mở chi tiết playlist
2. Nhấn **⋮ (Menu)** trên thanh công cụ
3. Chọn **"Xuất M3U"** hoặc **"Xuất XML"**
4. Chọn nơi lưu file

### 7.6. Nhập Playlist từ file

1. Vào **Thư viện → Playlist**
2. **Nhấn giữ nút ➕ (FAB)**
3. Chọn file M3U hoặc XML
4. Ứng dụng tự động **match bài hát** với thư viện của bạn
5. Playlist mới được tạo với các bài hát đã match

---

## 8. TÌM KIẾM

### Tìm kiếm bài hát

1. Vào tab **Tìm kiếm** (🔍)
2. Nhập tên bài hát, nghệ sĩ hoặc album
3. Kết quả hiển thị **tự động** sau 300ms

> **Tìm kiếm Local:** Tìm trong thư viện nhạc trên thiết bị  
> **Tìm kiếm Online:** Nếu không có kết quả local, tự động tìm trên Internet (nếu có API)

- **Click kết quả** → phát nhạc
- **Click ♡** → yêu thích/bỏ thích
- **Click ⋮** → thêm vào playlist

---

## 9. EQUALIZER & HIỆU ỨNG ÂM THANH

### Mở Equalizer

Trong màn hình phát nhạc toàn màn hình:
1. Nhấn **⋮ (Menu)** (góc trên phải)
2. Chọn **"Cân bằng âm thanh"**

### Giao diện Equalizer

```
┌─────────────────────────────────────┐
│    Cân bằng âm thanh                │
│                                     │
│  Preset                             │
│  ◀       Normal       ▶             │  ← Chọn preset
│                                     │
│  Tần số                             │
│  60 Hz   ───●───────  0.0 dB        │  ← Band slider
│  230 Hz  ──●────────  -2.5 dB       │
│  910 Hz  ─────●─────  +1.5 dB       │
│  4 kHz   ──────●────  +3.0 dB       │
│  14 kHz  ───●───────  -1.0 dB       │
│                                     │
│  Tăng cường bass  [══════○══]       │  ← Bass Boost toggle
│                                     │
│        [Đặt lại]    [Đóng]          │
└─────────────────────────────────────┘
```

### Các Preset có sẵn

| Preset | Phù hợp |
|--------|---------|
| **Normal** | Mọi thể loại |
| **Classical** | Nhạc cổ điển, giao hưởng |
| **Dance** | Nhạc sàn, EDM |
| **Flat** | Âm thanh gốc, không chỉnh |
| **Folk** | Dân ca, acoustic |
| **Heavy Metal** | Rock, Metal |
| **Hip Hop** | Rap, Hip Hop |
| **Jazz** | Jazz, blues |
| **Pop** | Nhạc Pop |
| **Rock** | Rock |

> Preset và band levels được **lưu tự động** — lần sau mở app vẫn giữ nguyên.

---

## 10. HẸN GIỜ TẮT NHẠC

### Cài đặt Sleep Timer

1. Mở màn hình phát nhạc toàn màn hình
2. Nhấn **⋮ (Menu)** → **"Hẹn giờ tắt nhạc"**
3. Chọn thời gian: **10, 15, 30, 45, 60 phút**
4. Nhấn **"Bắt đầu"**

### Huỷ Sleep Timer

- Khi timer đang chạy, mở lại menu Sleep Timer
- Nhấn **"Huỷ hẹn giờ"**
- Hoặc để yên — timer tự động tắt nhạc khi hết giờ

---

## 11. ĐĂNG NHẬP & ĐỒNG BỘ

### 11.1. Đăng ký tài khoản

1. Vào tab **Cài đặt (⚙)**
2. Chọn **"Đăng nhập / Đăng ký"**
3. Nhấn **"Chưa có tài khoản? Đăng ký"**
4. Nhập **Email** và **Mật khẩu** (tối thiểu 6 ký tự)
5. Nhấn **"Đăng ký"**

### 11.2. Đăng nhập

1. Vào tab **Cài đặt (⚙)** → **"Đăng nhập"**
2. Nhập Email và Mật khẩu
3. Nhấn **"Đăng nhập"**

> **Quên mật khẩu?** Nhấn **"Quên mật khẩu?"** → nhập email → kiểm tra hộp thư để reset.

### 11.3. Trang cá nhân (Profile)

Sau khi đăng nhập, vào **Cài đặt → "Tài khoản"** để xem:

- **Email** của bạn
- **UID** (mã định danh)
- Nút **"Đồng bộ ngay"** — đồng bộ thủ công
- Nút **"Đăng xuất"**

### 11.4. Đồng bộ dữ liệu

Sau khi đăng nhập, ứng dụng tự động:

1. **Tải lên** — danh sách bài hát yêu thích lên Firebase
2. **Tải xuống** — danh sách yêu thích từ các thiết bị khác
3. **Match** — so khớp bài hát dựa trên tên + nghệ sĩ + album
4. **Đồng bộ định kỳ** — tự động đồng bộ mỗi **8 giờ** (khi có WiFi)

> **Cần Internet:** Đồng bộ yêu cầu kết nối mạng.

---

## 12. IMPORT/EXPORT PLAYLIST

### Xuất Playlist (Export)

1. Mở playlist muốn xuất
2. Nhấn **⋮ (Menu)** → chọn định dạng:
   - **M3U** — phổ biến, tương thích nhiều app
   - **XML** — định dạng riêng, giữ được nhiều thông tin
3. Chọn nơi lưu file (Google Drive, Download, SD Card...)

### Nhập Playlist (Import)

1. Vào **Thư viện → Playlist**
2. **Nhấn giữ nút ➕ (FAB)** (góc dưới phải)
3. Chọn file `.m3u` hoặc `.xml` từ thiết bị
4. Ứng dụng sẽ:
   - Đọc danh sách bài hát từ file
   - Tìm bài hát tương ứng trong thư viện của bạn
   - Tạo playlist mới với các bài đã match

> **Lưu ý:** Nếu file import có bài hát không có trong thiết bị, bài đó sẽ bị bỏ qua.

---

## 13. TẢI NHẠC OFFLINE

### Cache tự động (Streaming)

Khi bạn phát nhạc trực tuyến (streaming), ExoPlayer **tự động cache** các chunk nhạc vào bộ nhớ tạm (tối đa **500MB**).

- Lần sau phát lại bài đó → **không cần tải lại** → phát nhanh hơn
- Khi **mất mạng** → bài đã cache vẫn phát được

### Xoá Cache

Vào **Cài đặt (⚙) → "Xoá cache"** → xác nhận.

> Cache tự động bị xoá khi đạt giới hạn 500MB (cơ chế LRU — ít dùng nhất bị xoá trước).

---

## 14. CÀI ĐẶT

Vào tab **Cài đặt (⚙)** → danh sách các mục:

### Mục tài khoản
| Mục | Mô tả |
|-----|-------|
| **Đăng nhập/Đăng ký** | Nếu chưa đăng nhập |
| **Tài khoản** | Xem profile, đồng bộ, đăng xuất (nếu đã đăng nhập) |

### Mục giao diện
| Mục | Mô tả |
|-----|-------|
| **Giao diện** | Chọn chế độ: **Theo hệ thống**, **Sáng**, **Tối** |

### Mục bộ nhớ
| Mục | Mô tả |
|-----|-------|
| **Dung lượng cache** | Hiển thị dung lượng cache hiện tại |
| **Xoá cache** | Xoá toàn bộ cache nhạc streaming |

### Mục ứng dụng
| Mục | Mô tả |
|-----|-------|
| **Giới thiệu** | Thông tin app, phiên bản, tác giả, license |

---

## 15. KHẮC PHỤC SỰ CỐ THƯỜNG GẶP

### ❌ "Không tìm thấy bài hát nào"

**Nguyên nhân:** Ứng dụng chưa có quyền đọc bộ nhớ.

**Cách khắc phục:**
1. Vào **Cài đặt điện thoại → Ứng dụng → NgheNhac → Quyền**
2. Bật quyền **"Nhạc và audio"** (Android 13+) hoặc **"Bộ nhớ"** (Android 9-12)
3. Quay lại ứng dụng → nhạc sẽ tự động xuất hiện

### ❌ "Lỗi phát nhạc"

**Nguyên nhân:** File nhạc bị hỏng hoặc không tương thích.

**Cách khắc phục:**
- Thử phát bài hát khác
- Nếu lỗi liên tục → nhấn **"Thử lại"** trong thông báo
- Kiểm tra file nhạc trên thiết bị

### ❌ "Không thể đăng nhập"

**Nguyên nhân:** Sai email/mật khẩu hoặc mất kết nối.

**Cách khắc phục:**
- Kiểm tra kết nối Internet
- Nhấn **"Quên mật khẩu?"** để reset
- Đảm bảo email đúng định dạng (có @)
- Mật khẩu tối thiểu 6 ký tự

### ❌ "Thiết bị không hỗ trợ Equalizer"

**Nguyên nhân:** Thiết bị không có chip âm thanh hỗ trợ AudioEffect API.

**Cách khắc phục:**
- Tính năng Equalizer không khả dụng trên thiết bị này
- Âm thanh vẫn phát bình thường, chỉ không thể tuỳ chỉnh

### ❌ Nhạc ngừng khi khoá màn hình

**Nguyên nhân:** Một số hãng (Xiaomi, Oppo, Huawei) giết background app để tiết kiệm pin.

**Cách khắc phục:**
1. Vào **Cài đặt điện thoại → Pin → Tối ưu hoá pin**
2. Tìm **NgheNhac** → chọn **"Không tối ưu"** hoặc **"Không giới hạn"**
3. Vào **Cài đặt → Ứng dụng → NgheNhac → Pin** → bật **"Cho phép chạy nền"**

### ❌ App bị treo hoặc crash

**Nguyên nhân:** Lỗi không mong muốn.

**Cách khắc phục:**
- Đóng app và mở lại
- Vào **Cài đặt → Ứng dụng → NgheNhac → Xoá cache**
- Nếu vẫn lỗi → gỡ và cài lại

---

## 📞 HỖ TRỢ

Nếu bạn gặp bất kỳ vấn đề nào, vui lòng:

- 📧 Email: [địa chỉ email hỗ trợ]
- 🌐 Website: [website dự án]
- 🐛 Báo lỗi: [link GitHub Issues]

---

> **NgheNhac v1.0** — Chúc bạn nghe nhạc vui vẻ! 🎶
