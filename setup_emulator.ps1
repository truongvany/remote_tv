# setup_emulator.ps1
# Thiet lap ket noi giua Phone Emulator va TV Emulator
# Chay moi khi restart Android Studio / Emulator
# Usage: .\setup_emulator.ps1

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path $adb)) {
    Write-Host "[ERROR] Khong tim thay adb tai: $adb" -ForegroundColor Red
    exit 1
}

Write-Host "[INFO] Dang kiem tra emulator..." -ForegroundColor Cyan
$rawDevices = & $adb devices
$devices = $rawDevices | Select-Object -Skip 1 | Where-Object { $_ -match "emulator" }

$phoneEmulator = $null
$tvEmulator = $null

foreach ($line in $devices) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    $serial = ($line -split "\s+")[0]
    $model = (& $adb -s $serial shell getprop ro.product.model 2>&1).Trim()
    Write-Host "  Found: $serial -> $model"
    if ($model -like "*TV*") {
        $tvEmulator = $serial
    } else {
        $phoneEmulator = $serial
    }
}

if (-not $tvEmulator) {
    Write-Host "[ERROR] Khong tim thay TV emulator!" -ForegroundColor Red
    exit 1
}
if (-not $phoneEmulator) {
    Write-Host "[ERROR] Khong tim thay Phone emulator!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[OK] Phone : $phoneEmulator" -ForegroundColor Green
Write-Host "[OK] TV    : $tvEmulator" -ForegroundColor Green
Write-Host ""

Write-Host "[INFO] Thiet lap port forwarding..." -ForegroundColor Yellow
& $adb -s $tvEmulator forward tcp:6466 tcp:6466 | Out-Null
& $adb -s $tvEmulator forward tcp:8009 tcp:8009 | Out-Null
& $adb -s $phoneEmulator reverse tcp:6466 tcp:6466 | Out-Null
& $adb -s $phoneEmulator reverse tcp:8009 tcp:8009 | Out-Null

Write-Host "[OK] Port forwarding hoan tat!" -ForegroundColor Green
Write-Host ""
Write-Host "[INFO] Trong app, chon 'Enter IP Manually':" -ForegroundColor Cyan
Write-Host "       IP   : 10.0.2.2" -ForegroundColor White
Write-Host "       Port : 6466" -ForegroundColor White
Write-Host ""

Write-Host "[TEST] Kiem tra ket noi TCP den TV..." -ForegroundColor Yellow
$tcp = New-Object System.Net.Sockets.TcpClient
try {
    $tcp.Connect("127.0.0.1", 6466)
    Write-Host "[OK] Ket noi thanh cong den TV emulator port 6466!" -ForegroundColor Green
    $tcp.Close()
} catch {
    Write-Host "[FAIL] Khong ket noi duoc port 6466: $_" -ForegroundColor Red
}
