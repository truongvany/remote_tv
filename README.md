# 📺 Remote TV - Smart IoT Controller

Dự án phát triển ứng dụng điều khiển Android TV / Smart TV toàn diện, sử dụng kiến trúc hiện đại và giao thức mạng cục bộ (LAN), kết hợp CI/CD tự động hóa.

## 🚀 Tính năng nổi bật (Key Features)
- **Auto Discovery (NSD):** Tự động quét và phát hiện các Smart TV trong cùng mạng Local.
- **Multi-Protocol Control:** Giao tiếp qua ADB (TCP Port 5555), hỗ trợ Samsung/Android TV. Tự động xử lý tính toán lại chu kỳ kết nối (Connection Policy & Backoff).
- **Smart Input & Voice AI:** Chuyển đổi giọng nói thành văn bản (SpeechRecognizer AI). Xử lý hoàn hảo việc gửi ký tự Tiếng Việt (Unicode) lên TV thông qua Global Search Intent.
- **Local Media Casting:** Trình chiếu video/ảnh từ thư viện điện thoại lên TV thông qua máy chủ cục bộ ảo (NanoHTTPD).
- **Dynamic UX/UI:** Real-time đa ngôn ngữ (chớp tắt đổi ngôn ngữ ngay lập tức không khởi động lại), giao diện Jetpack Compose mượt mà.

## 🏗 Kiến trúc dự án (Architecture)
Áp dụng triệt để **MVVM (Model - View - ViewModel)** kết hợp **Clean Architecture**:
- **UI Layer (`ui/`):** Xây dựng bằng Jetpack Compose 100%. Lắng nghe dữ liệu thông qua `StateFlow`.
- **Presentation Layer (`viewmodel/`):** Chứa `TVViewModel` xử lý các luồng State, nhận tương tác từ UI và chuyển xuống Repository thông qua Coroutines bất đồng bộ.
- **Data/Domain Layer (`data/`):**
  - `repository/`: Single Source of Truth (Nguồn dữ liệu duy nhất).
  - `protocol/`: Chứa `AdbProtocol` xử lý các tệp nhị phân, Socket TCP/IP.
  - `casting/`: Khởi tạo Local Server phát file đa phương tiện.

## 🔄 Luồng dữ liệu (Data Flow)
`User Click UI` ➔ `ViewModel (launch Coroutine)` ➔ `TVRepository` ➔ `ConnectionManager` ➔ `Protocol (TCP Socket)` ➔ `Smart TV`.

## 🛠️ Trải nghiệm Nhà phát triển (CI/CD & Testing)
- **GitHub Actions:** Tự động bắt lỗi bằng PR Checklist Bot và Luồng `Build APK CI/CD` tự động build ra file `.apk` cài đặt sau mỗi lượt gộp code.
- **Unit Test (JUnit 4 + Mockito):** Xây dựng các Test case cho `TVViewModel` và `ConnectionPolicy` giả lập thao tác mạng, đảm bảo không sập dự án khi đứt kết nối.
