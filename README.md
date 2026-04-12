# 📺 REMOTE TV - Báo Cáo Phân Tích Kiến Trúc & Luồng Ứng Dụng

Tài liệu này mô tả chi tiết cách ứng dụng Remote TV hoạt động, toàn bộ kiến trúc mã nguồn và các tính năng đột phá đã được tích hợp trong dự án.

## 1) Tổng quan kiến trúc (Architecture)

Ứng dụng tuân thủ nghiêm ngặt mô hình **MVVM (Model - View - ViewModel)** và **Clean Architecture**, kết hợp giao tiếp mạng thời gian thực:
- **UI Layer (Giao diện):** Xây dựng hoàn toàn bằng công nghệ Jetpack Compose (`ui/screens` và `ui/components`).
- **State Layer (Quản lý trạng thái):** `ui/viewmodel/TVViewModel` điều phối dữ liệu với `StateFlow` và `Coroutines`.
- **Data Layer (Dữ liệu & Logic mạng):** Nằm tại kho trung tâm `data/repository/TVRepositoryImpl`.
- **Connection & Protocol Layer:** Quản lý vòng lặp thử lại mạng (Backoff Policy) tại `TVConnectionManager`, xử lý gói tin mạng và ADB thông qua `AdbProtocol`.
- **Discovery Layer:** Dịch vụ quét thiết bị mạng nội bộ `TVDiscoveryService` (mDNS/NSD).
- **CI/CD & DevOps:** Hệ thống GitHub Actions tự động nhảy vào chạy bộ Test cấu hình và xuất xưởng file cài đặt APK.

## 2) Các tính năng nổi bật (Key Features)

1. **Điều khiển TV thông minh đa giao thức:** Hỗ trợ Android TV & Samsung. Luôn có Fallback xử lý đa luồng thông minh qua củ Socket TCP/IP và Android Debug Bridge (ADB).
2. **Nhập Tiếng Việt & Ký tự đặc biệt (Đột phá):** Giải quyết triệt để lỗi mất chữ, vỡ font của giao thức ADB chuẩn bằng thuật toán bắt chặn ký tự Unicode và tự động chuyển hóa thành một lệnh gọi `Global Search Intent`.
3. **Ứng dụng truy cập nhanh (Quick Launch):** Nhận diện trạng thái kết nối TV. Yêu cầu kết nối trước khi khởi chạy. Hiển thị viền đỏ báo hiệu cho biết ứng dụng nào đang chiếu màn trên tivi ("Đang phát").
4. **Cast Media & Screen Mirroring:** Truyền tải tệp ảnh/video trực tiếp từ thư viện điện thoại sang TV bằng cách dựng liền một máy chủ ảo trên điện thoại sinh viên (qua NanoHTTPD).
5. **Chuyển Ngôn ngữ Real-time tức thời:** Đổi ngôn ngữ Ứng dụng (Anh/Việt) ngay lập tức trong Settings. Áp dụng kỹ thuật cao bằng Delegate của `AppCompatActivity` hỗ trợ App tự vẽ (repaint) lại UI mà không bị giật, hất văng hay tái khởi động (Restart App).
6. **Nhận diện giọng nói (Voice AI On-device):** Phân tích dải âm thanh theo thời gian thực (RMS Audio), gọi thuật toán Speech Recognition AI quét chữ và truyền song song lên thanh tìm kiếm TV.

## 3) Sơ đồ cây hoạt động của ứng dụng (App Flow)

```text
App Launch
|
+-- AndroidManifest
|   +-- MainActivity (LAUNCHER - Kế thừa AppCompatActivity hõ trợ Real-time Locale)
|
+-- MainActivity.onCreate
|   +-- setContent(REMOTE_TVTheme)
|       +-- RemoteScreen(viewModel = TVViewModel)
|
+-- TVViewModel.init
|   +-- repository.startDiscovery()
|       +-- TVDiscoveryService.startDiscovery()
|           +-- NSD discover _androidtvremote2._tcp & _googlecast
|           +-- Cập nhật danh sách thiết bị vào StateFlow
|
+-- RemoteScreen (Thanh điều hướng phân Tab tùy chỉnh)
|   +-- Tab 0: REMOTE -> MainRemoteTab
|   |   +-- Tổ hợp Phím di chuyển, Home, Nguồn, Tắt Âm
|   |   +-- Voice AI -> Gọi SpeechRecognizer SDK -> Gửi TEXT / SEARCH lên màn lớn.
|   |
|   +-- Tab 1: CHANNELS -> ChannelsScreen
|   |   +-- Quick Launch (Netflix, YT...) -> Check isTvConnected -> launchApp(appId)
|   |   +-- Render trạng thái biểu tượng "Đang phát" mượt mà
|   |
|   +-- Tab 2: CAST -> CastScreen
|   |   +-- Screen Mirroring (Mở trình phóng màn hình gốc của Android OS)
|   |   +-- Chọn Ảnh/Video từ file -> Bắn lệnh Endpoint TV Web View
|   |
|   +-- Tab 3: MACRO -> MacroScreen
|   |   +-- Gửi chuỗi kịch bản lệnh liên hoàn (delay cấu hình tự động)
|   |
|   +-- Tab 4: SETTINGS -> SettingsScreen
|       +-- Quản trị Dark/Light Mode, Chế độ chống tắt màn hình, Đổi ngôn ngữ (Locale)
|
+-- Luồng Chọn Trạm Phủ Sóng (Connection Flow)
|   +-- Chọn thiết bị -> connectToDevice(device)
|       +-- TVConnectionManager.connect(device)
|           +-- Samsung / AndroidTV Protocol -> Fallback ADB khẩn (Port 5555)
|
+-- Luồng Thực Thi Mạng TCP (Command Execution Flow)
    +-- Người dùng thao tác trên màn UI điện thoại
        +-- TVViewModel.sendCommand / launchApp
            +-- TVRepositoryImpl -> Giao thức (Protocol) đang được ghim
```

## 4) Quy trình Phát triển Nhóm & Quản trị (QA/QC)

- **Workflow GitHub Chuẩn mực:** Tổ chức code với các nhánh Tính năng (Feature Branch). Cấu trúc yêu cầu Code Review trước mỗi nhịp Pull Requests (Approve chéo).
- **Kiểm định Unit Test (JUnit & Mockito):** Kịch bản kiểm thử tĩnh viết tại cụm `data/connection/ConnectionPolicyTest.kt` và logic ViewModel bảo vệ ứng dụng, làm rào cản chặn mã độc.
- **Continuous Integration (CI/CD):** Tự động kiểm duyệt Checklist PR (`pr-checklist-enforcer.yml`) và đóng gói mã nguồn, kiểm tra file thành phẩm xuất ra `.apk` trên máy chủ mây GitHub Actions (`build_apk.yml`).

## 5) Ghi chú Kỹ thuật Chuyên sâu (Technical Notes)
- Bất kỳ lỗi ngắt kết nối mạng chập chờn nào đều được bắt exception và lùi thời gian Timeout tuân theo thuật toán tính chậm dần `ConnectionPolicy.backoffMs`.
- Trạng thái phản hồi thực tế từ Tivi với Điện thoại liên tục được đẩy ngược về giao diện luồng qua bộ chẩn đoán (In-App Diagnostics), giúp giảng viên và người thiết kế dễ dàng Tracking toàn bộ gói tin đi và về.

