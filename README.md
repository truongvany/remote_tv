# REMOTE TV

Ứng dụng Android dùng để điều khiển Smart TV trong mạng LAN, kết hợp discovery tự động, remote command đa giao thức, cast media, macro và in-app diagnostics.

Mục tiêu của dự án:

- Tìm TV nhanh trong cùng Wi-Fi.
- Kết nối linh hoạt theo từng hãng TV.
- Điều khiển TV bằng phím, text, voice query, quick launch app.
- Có log nội bộ để dễ debug khi demo và test.

## Tính Năng Chính

1. Discovery TV trong LAN
- NSD/DNS-SD: _googlecast._tcp, _androidtvremote2._tcp, _samsungmsf._tcp, _roku-ecp._tcp.
- Subnet scan 2 giai đoạn (quick scan + full scan) theo port phổ biến.

2. Kết nối và điều khiển đa giao thức
- Android TV/Google TV: ưu tiên đường ADB (5555) để điều khiển ổn định.
- Samsung Tizen: WebSocket remote API (8001/8002, có fallback).
- LG webOS: SSAP qua WebSocket (3000).

3. Remote command
- Navigation, volume, channel, power, home/back/menu.
- Text input có fallback cho trường hợp khó xử lý ký tự.
- Voice query flow: mở search + gửi text + enter.

4. Quick Launch app
- Launch app theo appId.
- Có mapping appId theo hãng TV (Android TV/Samsung/LG).
- Có fallback launch bằng search query khi launch trực tiếp thất bại.

5. Cast media
- Google Cast session cho ảnh/video.
- LocalMediaServer (NanoHTTPD) dựng endpoint nội bộ để TV lấy file.

6. Macro và diagnostics
- Macro command sequence có delay và khả năng dừng giữa chừng.
- In-app diagnostics stream để theo dõi từng bước scan/connect/send.

7. Preferences và auto reconnect
- Lưu cài đặt app, profile, macro.
- Lưu last device để auto reconnect.
- Hỗ trợ Wake-on-LAN nếu có MAC address.

## Hệ Điều Hành TV Đang Hỗ Trợ

- Android TV / Google TV: mức hỗ trợ cao nhất trong project hiện tại.
- Samsung Tizen OS: hỗ trợ điều khiển tốt.
- LG webOS: hỗ trợ cơ bản đến trung bình (tùy model).
- Roku OS: chủ yếu ở mức discovery.
- Fire OS: chủ yếu ở mức discovery.

Lưu ý: Google Cast là giao thức cast media, không phải hệ điều hành TV.

## Kiến Trúc

Project theo hướng MVVM + Data layer rõ vai trò:

- UI: Jetpack Compose screens/components.
- ViewModel: điều phối state và use-case.
- Repository: cửa vào duy nhất cho discovery/connect/command.
- Discovery: NSD + subnet scan.
- Connection manager: timeout/retry/backoff/fallback.
- Protocols: Samsung, LG, ADB, Android TV remote2 (một phần).
- Debug: InAppDiagnostics cho log trong app.

## Luồng Hoạt Động Chính

1. Mở Cast tab hoặc bấm scan.
2. Discovery tìm TV (NSD + quét IP subnet).
3. User chọn TV và bấm connect.
4. ConnectionManager chọn protocol phù hợp và kết nối.
5. Sau khi connected, user gửi command/launch app/cast media.
6. Trạng thái và log được đẩy ngược lên UI qua StateFlow.

## Giới Hạn Hiện Tại

- Android TV remote2 TLS pairing chưa hoàn chỉnh, nên đường chính hiện tại là ADB.
- Muốn full control Android TV cần bật USB Debugging + Network Debugging.
- Roku/Fire TV chưa có full remote control end-to-end.

## Yêu Cầu Môi Trường

- Điện thoại Android và TV phải cùng Wi-Fi.
- Cấp quyền mạng/vị trí theo yêu cầu Android để discovery ổn định.
- Android TV: bật Developer Options + USB Debugging + Network Debugging nếu dùng ADB path.

## Build Nhanh

```powershell
.\gradlew.bat :app:assembleDebug
```

Sau khi build xong:

1. Cài APK lên điện thoại.
2. Mở tab Cast để scan thiết bị.
3. Kiểm tra kết nối, điều khiển và diagnostics trong app.

## Cấu Trúc Thư Mục Quan Trọng

```text
app/src/main/java/com/example/remote_tv/
    ui/
    data/
        repository/
        discovery/
        connection/
        protocol/
        casting/
        network/
        preferences/
        debug/
```

