# REMOTE TV - App Flow Analysis

Tai lieu nay mo ta cach app dang hoat dong theo ma nguon hien tai.

## 1) Tong quan kien truc

- UI layer: Jetpack Compose trong `ui/screens` va `ui/components`
- State layer: `ui/viewmodel/TVViewModel`
- Data layer: `data/repository/TVRepositoryImpl`
- Connection layer: `data/connection/TVConnectionManager`
- Discovery layer: `data/discovery/TVDiscoveryService`
- Protocol layer: `data/protocol/*`

## 2) Diem vao app

- Android launcher activity: `MainActivity`
- `MainActivity` set Compose content -> `RemoteScreen()`

## 3) So do cay hoat dong cua app

```text
App Launch
|
+-- AndroidManifest
|   +-- MainActivity (LAUNCHER)
|
+-- MainActivity.onCreate
|   +-- setContent(REMOTE_TVTheme)
|       +-- RemoteScreen(viewModel = TVViewModel)
|
+-- TVViewModel.init
|   +-- repository.startDiscovery()
|       +-- TVDiscoveryService.startDiscovery()
|           +-- NSD discover _googlecast._tcp
|           +-- NSD discover _androidtvremote2._tcp
|           +-- update discoveredDevices StateFlow
|
+-- RemoteScreen (Bottom Tabs)
|   +-- Tab 0: HOME -> MainRemoteTab
|   |   +-- TopBar
|   |   |   +-- Power -> viewModel.powerToggle() -> KEY_POWER
|   |   +-- DPad
|   |   |   +-- UP/DOWN/LEFT/RIGHT -> sendDirection(...)
|   |   |   +-- OK -> sendOk() -> "OK"
|   |   +-- ControlButtons
|   |   |   +-- BACK -> KEY_BACK
|   |   |   +-- HOME -> KEY_HOME
|   |   |   +-- MENU -> KEY_MENU
|   |   |   +-- SEARCH -> KEY_SEARCH
|   |   |   +-- VOICE -> KEY_VOICE
|   |   |   +-- MUTE -> KEY_MUTE
|   |   +-- QuickLaunch
|   |       +-- Netflix / YouTube / Disney+ -> launchApp(appId)
|   |
|   +-- Tab 1: CHANNELS -> ChannelsScreen
|   |   +-- Hien thi danh sach trending/channels (UI mock)
|   |
|   +-- Tab 2: CAST -> Coming Soon
|   |
|   +-- Tab 3: SETTINGS -> SettingsScreen
|       +-- Hien thi thong tin giao dien, network, support (UI mock)
|
+-- Device Selection Dialog (khi uiState.showDeviceDialog = true)
|   +-- Chon thiet bi duoc discover
|   +-- Hoac nhap IP/port thu cong
|   +-- connectToDevice(device)
|       +-- repository.connectToDevice(device)
|           +-- TVConnectionManager.connect(device)
|               +-- brand = SAMSUNG -> SamsungProtocol
|               +-- brand = LG -> LGProtocol
|               +-- brand = ANDROID_TV -> AndroidTVRemoteProtocol
|               |   +-- neu that bai -> fallback AdbProtocol (port 5555)
|               +-- set currentDevice = connected
|
+-- Command Execution Flow
	+-- UI action
		+-- TVViewModel.sendCommand/launchApp
			+-- TVRepositoryImpl
				+-- TVConnectionManager
					+-- activeProtocol.sendCommand(...) / launchApp(...)

```

## 4) Ghi chu hanh vi hien tai

- Luong dieu huong chinh hien tai la theo selectedTab (khong dung NavHost).
- ChannelsScreen va SettingsScreen chu yeu la UI tinh/mock, chua day du logic backend.
- AndroidTVRemoteProtocol da map keycode co ban; `launchApp` tren protocol nay hien tra ve false.
- Fallback ADB duoc uu tien cho truong hop AndroidTVRemote khong ket noi duoc.

## 5) Tam tat nhanh runtime

1. Mo app -> vao RemoteScreen.
2. ViewModel bat dau discovery TV tren LAN.
3. Nguoi dung chon/nhap TV -> ConnectionManager tao protocol phu hop.
4. Moi thao tac remote tren UI duoc doi thanh command va gui qua protocol dang active.
5. Trang thai thiet bi va danh sach thiet bi duoc quan ly bang StateFlow.
