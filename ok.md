# Giai thich cac phan trong data

Tai lieu nay chi giai thich package data cua du an, tap trung vao tung module lam gi va cach chung ket hop voi nhau.

## 1. Tong quan package data

Thu muc data duoc tach thanh cac nhom sau:

- repository: cua vao duy nhat de ViewModel goi.
- discovery: tim TV trong mang LAN.
- connection: quan ly ket noi, retry, fallback protocol.
- protocol: chi tiet cach gui lenh den tung loai TV.
- casting: Google Cast va media server noi bo.
- network: Wake-on-LAN va lay danh sach app tren TV.
- preferences: luu cai dat app, last device, macro.
- model: cac data class dung chung.
- debug: log noi bo de hien ngay trong app.

Noi gon: data la tang xu ly nghiep vu ket noi TV va truyen thong mang, khong phai tang UI.

## 2. Repository

File chinh:

- app/src/main/java/com/example/remote_tv/data/repository/TVRepository.kt
- app/src/main/java/com/example/remote_tv/data/repository/TVRepositoryImpl.kt

Nhiem vu:

- Cung cap API cho tang tren: start scan, stop scan, connect, disconnect, send command, launch app.
- Expose StateFlow: discoveredDevices, currentDevice, isScanning, playbackState, diagnosticLogs.
- Noi cac module con lai voi nhau (discovery + connection + preferences + network).

Ban chat cua TVRepositoryImpl:

- Tao TVDiscoveryService de tim thiet bi.
- Tao TVConnectionManager de ket noi va gui lenh.
- Doc/ghi AppPreferencesRepository de auto reconnect.
- Goi WakeOnLanSender de danh thuc TV.
- Goi TVAppListFetcher de lay danh sach app.

## 3. Discovery (tim TV trong LAN)

File chinh:

- app/src/main/java/com/example/remote_tv/data/discovery/TVDiscoveryService.kt
- app/src/main/java/com/example/remote_tv/data/discovery/LocalSubnetScanner.kt

### 3.1 TVDiscoveryService lam gi

Service nay tim thiet bi bang 2 cach song song:

1. NSD (DNS-SD)
- Quet cac service type: _googlecast._tcp, _androidtvremote2._tcp, _samsungmsf._tcp, _roku-ecp._tcp.
- Resolve service thanh ip + port + ten thiet bi.

2. Quet subnet noi bo
- Goi LocalSubnetScanner de scan cac host trong mang noi bo.

Ket qua cua 2 nguon duoc merge theo ip, tranh trung lap.

### 3.2 LocalSubnetScanner lam gi

LocalSubnetScanner:

- Lay local IPv4 cua dien thoai trong Wi-Fi hien tai.
- Suy ra subnet base, vi du 192.168.1.x.
- Quet host tu .1 den .254.
- Thu mo TCP vao tung port de biet host nao co dich vu.

Scan theo 2 stage:

- Quick stage: 8008, 8009, 5555, 6466, 6467.
- Full stage: 8002, 8060, 3000, 7236 va cac port con lai.

Vi du ban noi "Cast quet ip va ket noi":

- Dung, Cast tab goi discovery.
- Discovery quet ip LAN va tim service.
- Sau do hien danh sach TV de user bam connect.

## 4. Connection (quan ly ket noi)

File chinh:

- app/src/main/java/com/example/remote_tv/data/connection/TVConnectionManager.kt
- app/src/main/java/com/example/remote_tv/data/connection/ConnectionPolicy.kt

Nhiem vu cua TVConnectionManager:

- Nhan yeu cau connect voi 1 TVDevice.
- Chon protocol phu hop theo brand va port.
- Thu nhieu round ket noi (retry + backoff).
- Giu activeProtocol de gui lenh ve sau.
- Tao message loi ro rang neu that bai.

Co che chon protocol:

- Samsung: uu tien SamsungProtocol (8001/8002), that bai thi fallback ADB 5555.
- LG: dung LGProtocol.
- Android TV: uu tien duong ADB 5555 de dam bao dieu khien duoc.
- UNKNOWN: doan theo port (8009 la truong hop mo ho, thu Android truoc roi Samsung).

ConnectionPolicy quy dinh:

- CONNECT_TIMEOUT_MS.
- MAX_CONNECT_ROUNDS.
- BACKOFF_BASE_MS.

## 5. Protocol (gui lenh thuc te)

File contract:

- app/src/main/java/com/example/remote_tv/data/protocol/TVProtocol.kt

Moi protocol phai co 4 ham:

- connect
- disconnect
- sendCommand
- launchApp

### 5.1 SamsungProtocol

File:

- app/src/main/java/com/example/remote_tv/data/protocol/SamsungProtocol.kt

Lam gi:

- Mo WebSocket den Samsung remote API.
- Gui remote key bang JSON frame (Click/Press/Release).
- Gui TEXT theo chunk base64 de on dinh hon.
- Launch app qua event frame va HTTP fallback.

### 5.2 AdbProtocol

File:

- app/src/main/java/com/example/remote_tv/data/protocol/AdbProtocol.kt
- app/src/main/java/com/example/remote_tv/data/AdbKeyManager.kt

Lam gi:

- Ket noi ADB over TCP port 5555.
- Chay handshake CNXN/AUTH day du.
- Neu TV chua trust key, gui public key va cho user bam Allow tren TV.
- Sau khi mo shell, gui input keyevent, text, search query, open url, launch app.

Text input trong ADB co nhieu fallback:

- input text
- clipboard + paste
- cmd input

Muc dich: giam loi mat dau cach va loi ky tu Unicode.

### 5.3 LGProtocol

File:

- app/src/main/java/com/example/remote_tv/data/protocol/LGProtocol.kt

Lam gi:

- Ket noi WebSocket WebOS (SSAP) port 3000.
- Register session, co the can user xac nhan tren TV.
- Gui command qua URI SSAP (volume, channel, home, launch app, text input).

### 5.4 AndroidTVRemoteProtocol

File:

- app/src/main/java/com/example/remote_tv/data/protocol/AndroidTVRemoteProtocol.kt
- app/src/main/java/com/example/remote_tv/data/protocol/AndroidTVCertificateManager.kt

Lam gi:

- Thu dieu khien qua remote2 (6466/6467) voi TLS.
- Tuy nhien trong project nay pairing remote2 chua hoan chinh, nen duong dieu khien on dinh van la ADB.

## 6. Casting (Google Cast media)

File chinh:

- app/src/main/java/com/example/remote_tv/data/casting/CastManager.kt
- app/src/main/java/com/example/remote_tv/data/casting/LocalMediaServer.kt
- app/src/main/java/com/example/remote_tv/data/casting/CastOptionsProvider.kt

Can phan biet ro 2 viec:

1. Discovery/connect TV trong LAN (o discovery + connection).
2. Cast media len TV (o casting).

CastManager lam gi:

- Quan ly Cast session (started, resumed, failed, ended).
- Tao MediaInfo va load qua remoteMediaClient.
- Cap nhat castStatus va castError.

LocalMediaServer lam gi:

- Copy media tu Uri vao cache.
- Chay HTTP server noi bo (NanoHTTPD).
- Tao URL local de TV truy cap media.
- Ho tro range request cho video stream.

## 7. Network helpers

File:

- app/src/main/java/com/example/remote_tv/data/network/WakeOnLanSender.kt
- app/src/main/java/com/example/remote_tv/data/network/TVAppListFetcher.kt

WakeOnLanSender:

- Tao magic packet (6 byte FF + MAC lap 16 lan).
- Gui UDP broadcast de danh thuc TV dang standby.

TVAppListFetcher:

- Samsung: goi REST API lay app list.
- Android TV: thu ADB shell de lay package list.
- LG: hien tai co fallback danh sach app pho bien.

## 8. Preferences va macro

File:

- app/src/main/java/com/example/remote_tv/data/preferences/AppPreferencesRepository.kt
- app/src/main/java/com/example/remote_tv/data/preferences/MacroRepository.kt
- app/src/main/java/com/example/remote_tv/data/preferences/LocaleManager.kt

AppPreferencesRepository:

- Luu app settings (theme, language, auto reconnect, auto scan).
- Luu user profile.
- Luu last device de auto reconnect.
- Luu macros JSON.

MacroRepository:

- Doc/ghi danh sach macro qua DataStore.
- Encode/decode JSON cho macro commands.

LocaleManager:

- Ap dung ngon ngu app qua AppCompatDelegate locales.

## 9. Model va debug

Model:

- app/src/main/java/com/example/remote_tv/data/model/TVDevice.kt
- va cac model khac trong data/model

Vai tro:

- Chuan hoa du lieu trao doi giua cac module data.

Debug:

- app/src/main/java/com/example/remote_tv/data/debug/InAppDiagnostics.kt

Vai tro:

- Ghi log noi bo theo muc I/W/E.
- Gioi han so dong log de tranh phinh bo nho.
- Cho phep UI doc log real-time khi debug ket noi.

## 10. Luong tong ket chi trong data

Khi user bam scan/connect/send command, luong trong data la:

1. TVRepositoryImpl nhan yeu cau.
2. Discovery tim thiet bi (NSD + quet subnet).
3. ConnectionManager chon protocol va ket noi.
4. Protocol gui goi tin thuc te toi TV.
5. Diagnostics ghi log, state duoc day nguoc len repository.

Do do, package data la phan xuong song cua app remote TV: tim TV, ket noi TV, gui lenh TV, va xu ly loi mang.