<#
  doi-may-chu-pc.ps1 — đổi địa chỉ máy chủ của client PC (bản Unity "pb189").

  Chạy trên Windows, không cần cài thêm gì: PowerShell có sẵn trong mọi bản Windows.

      .\doi-may-chu-pc.ps1                     hỏi từng bước
      .\doi-may-chu-pc.ps1 -May 100.x.y.z -Cong 14444
      .\doi-may-chu-pc.ps1 -Xem                chỉ xem địa chỉ hiện tại, không sửa gì
      .\doi-may-chu-pc.ps1 -May ... -KhongHoi  không hỏi lại trước khi sửa
      .\doi-may-chu-pc.ps1 -KhoiPhuc           trả lại bản trước khi script này đụng vào

  VÌ SAO PHẢI SỬA TRONG DLL: client không có tệp cấu hình nào chứa địa chỉ, cũng không có ô cho
  người chơi tự gõ. PlayerPrefs chỉ nhớ *chỉ số* máy chủ đã chọn (lastServer, indServer). Danh
  sách thật nằm cứng trong ba chuỗi của Assembly-CSharp.dll. Các tệp asset của Unity (level0,
  globalgamemanagers, sharedassets0) không chứa địa chỉ nào — đã dò.

  CÁI BẪY CHÍNH: không được đổi ĐỘ DÀI chuỗi.

  String literal của .NET nằm trong heap #US, mỗi chuỗi là [độ dài nén][UTF-16LE][1 byte cuối],
  và lệnh ldstr trong IL trỏ tới chuỗi bằng OFFSET TUYỆT ĐỐI vào heap đó. Viết chuỗi dài hơn là
  đè lên chuỗi kế tiếp; viết ngắn hơn rồi dịch phần sau lên là mọi offset phía sau lệch hết.
  Nên script này giữ Y NGUYÊN số ký tự, chỉ đệm/cắt phần TÊN máy chủ cho vừa. Tên chỉ để hiện ở
  màn chọn máy chủ, thừa vài dấu cách không ảnh hưởng gì.

  Cách này không phải suy đoán: bản trong nso.zip đã được người khác vá đúng kiểu này —
  Assembly-CSharp.dll.bak ghi vps.nso.id.vn còn bản đang dùng ghi 103.28.32.204, hai chuỗi cùng
  13 ký tự, hai tệp cùng 1.025.024 byte, chỉ khác đúng 36 byte.
#>
[CmdletBinding()]
param(
    [string]$Dll,
    [string]$May,
    [string]$Cong,
    [string]$Ten = 'NSO',
    [switch]$Xem,
    [switch]$KhoiPhuc,
    [switch]$KhongHoi
)

$ErrorActionPreference = 'Stop'
# Thiếu dòng này thì tiếng Việt in ra cửa sổ lệnh thành dấu hỏi, dù tệp script đã đúng mã.
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }

$ENC = [System.Text.Encoding]::Unicode   # UTF-16LE, đúng kiểu mà .NET cất string literal

# Một mục: TÊN:MÁYCHỦ:CỔNG:cờ:cờ — nối nhau bằng dấu phẩy. Bắt cả dạng tên miền lẫn dạng IP:
# client gốc dùng nj*.teamobi.com, còn các bản vá lại thì dùng IP.
$MOT_MUC = '[^:,]+:[A-Za-z0-9.\-]+:\d+:\d+:\d+'
$MAU = '^' + $MOT_MUC + '(,' + $MOT_MUC + ')*$'


function Tim-Dll {
    param([string]$Duong)

    if ($Duong) {
        if (-not (Test-Path -LiteralPath $Duong)) { throw "không thấy tệp: $Duong" }
        return (Resolve-Path -LiteralPath $Duong).Path
    }

    # Đặt script cạnh NinjaSchool_189.exe là chạy được ngay, không phải gõ đường dẫn.
    $goc = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
    $thu = @(
        (Join-Path $goc 'NinjaSchool_189_Data\Managed\Assembly-CSharp.dll'),
        (Join-Path $goc 'Assembly-CSharp.dll'),
        (Join-Path $goc 'pb189\NinjaSchool_189_Data\Managed\Assembly-CSharp.dll')
    )
    foreach ($t in $thu) {
        if (Test-Path -LiteralPath $t) { return (Resolve-Path -LiteralPath $t).Path }
    }
    throw "không tự tìm được Assembly-CSharp.dll — đặt script cạnh NinjaSchool_189.exe, hoặc gọi kèm -Dll <đường\dẫn.dll>"
}


function Doc-DanhSach {
    param([byte[]]$B)

    $ra = New-Object System.Collections.ArrayList

    # Quét CẢ HAI thế căn chẵn/lẻ. Chuỗi trong heap #US không đảm bảo bắt đầu ở offset chẵn —
    # trong bản này danh sách thứ hai nằm ở offset 922001, là số lẻ. Giải mã từ offset 0 thì nó
    # thành rác và không bao giờ khớp.
    foreach ($lech in 0, 1) {
        $dai = [int]([math]::Floor(($B.Length - $lech) / 2)) * 2
        if ($dai -le 0) { continue }
        $s = $ENC.GetString($B, $lech, $dai)
        foreach ($m in [regex]::Matches($s, '[\x20-\x7e]{20,}')) {
            if ($m.Value -match $MAU) {
                $null = $ra.Add([pscustomobject]@{
                    Offset = $lech + $m.Index * 2
                    Chuoi  = $m.Value
                })
            }
        }
    }
    return @($ra | Sort-Object Offset)
}


function Dung-Lai {
    param([string]$Cu, [string]$MayMoi, [string]$CongMoi, [string]$TenMoi)

    $muc = @($Cu -split ',')

    # Giữ nguyên số mục và hai cờ cuối của từng mục — không biết chúng để làm gì (mục cuối của
    # mọi danh sách đều là :0:1), nên chép lại thay vì tự đặt.
    $duoi = @()
    foreach ($m in $muc) {
        $p = $m -split ':'
        $duoi += ":${MayMoi}:${CongMoi}:$($p[3]):$($p[4])"
    }

    $tong = 0
    foreach ($d in $duoi) { $tong += $d.Length }
    $conLai = $Cu.Length - $tong - ($muc.Count - 1)
    if ($conLai -lt $muc.Count) {
        throw "địa chỉ quá dài: ${MayMoi}:${CongMoi} không nhét vừa $($Cu.Length) ký tự với $($muc.Count) mục"
    }

    # Chia đều số ký tự còn lại cho các tên; phần dư dồn vào những tên đầu.
    $day = [int]([math]::Floor($conLai / $muc.Count))
    $du = $conLai % $muc.Count
    $ra = @()
    for ($i = 0; $i -lt $muc.Count; $i++) {
        $n = $day
        if ($i -lt $du) { $n = $n + 1 }
        if ($TenMoi.Length -gt $n) { $script:tenBiCat = $true }
        $ra += $TenMoi.PadRight($n).Substring(0, $n) + $duoi[$i]
    }

    $moi = $ra -join ','
    if ($moi.Length -ne $Cu.Length) {
        throw "lỗi nội bộ: chuỗi mới dài $($moi.Length), chuỗi cũ dài $($Cu.Length)"
    }
    return $moi
}


# ---------------------------------------------------------------------------

# Bọc cả phần thân: `throw` để trần thì PowerShell in ra khối đỏ nhiều dòng kèm cả dòng mã,
# người không quen sẽ tưởng script hỏng. Ở đây mọi lỗi đều là lỗi người dùng đoán được.
$script:tenBiCat = $false
try {

$duongDll = Tim-Dll -Duong $Dll
$sao = "$duongDll.goc"

Write-Host ""
Write-Host "Tệp: $duongDll"

# Bản .bak đi kèm gói tải về là của người phát hành (trỏ vps.nso.id.vn). Script này KHÔNG bao giờ
# đụng tới nó — điểm khôi phục riêng là .goc, tạo đúng một lần ở lần chạy đầu.
if ($KhoiPhuc) {
    if (-not (Test-Path -LiteralPath $sao)) { throw "chưa có bản lưu $sao — script chưa từng sửa tệp này" }
    Copy-Item -LiteralPath $sao -Destination $duongDll -Force
    Write-Host "đã trả lại bản trước khi script này đụng vào."
    exit 0
}

$byte = [System.IO.File]::ReadAllBytes($duongDll)
$ds = Doc-DanhSach -B $byte
if ($ds.Count -eq 0) { throw "không tìm thấy chuỗi danh sách máy chủ nào trong tệp" }

Write-Host "Địa chỉ hiện tại:"
foreach ($d in $ds) {
    $dau = ($d.Chuoi -split ',')[0] -split ':'
    Write-Host ("  offset {0,-8} {1} mục  ->  {2}:{3}" -f $d.Offset, ($d.Chuoi -split ',').Count, $dau[1], $dau[2])
}
Write-Host ""

if ($Xem) { exit 0 }

# Tệp đang bị game mở thì ghi đè sẽ hỏng nửa chừng.
$dangChay = @(Get-Process -Name 'NinjaSchool_189' -ErrorAction SilentlyContinue)
if ($dangChay.Count -gt 0) { throw "NinjaSchool_189.exe đang chạy — tắt game rồi chạy lại script" }

# Thiếu tham số thì hỏi; Enter là giữ nguyên giá trị đang có. Với -KhongHoi thì không hỏi gì
# cả, kể cả những ô chưa truyền -- để gọi được từ script khác mà không bị treo chờ gõ phím.
$dauTien = ($ds[0].Chuoi -split ',')[0] -split ':'
if (-not $May) {
    if ($KhongHoi) { $May = $dauTien[1] }
    else {
        $May = Read-Host "Máy chủ mới (IP hoặc tên miền) [$($dauTien[1])]"
        if (-not $May) { $May = $dauTien[1] }
    }
}
if (-not $Cong) {
    if ($KhongHoi) { $Cong = $dauTien[2] }
    else {
        $Cong = Read-Host "Cổng [$($dauTien[2])]"
        if (-not $Cong) { $Cong = $dauTien[2] }
    }
}

if ($May -notmatch '^[A-Za-z0-9.\-]+$') { throw "địa chỉ chỉ được gồm chữ, số, dấu chấm và dấu gạch: $May" }
if ($Cong -notmatch '^\d+$' -or [int]$Cong -lt 1 -or [int]$Cong -gt 65535) { throw "cổng phải là số 1..65535: $Cong" }

Write-Host ""
Write-Host "Sẽ đổi cả $($ds.Count) danh sách về  ${May}:${Cong}  (tên hiển thị: $Ten)"
if (-not $KhongHoi) {
    $traLoi = Read-Host "Gõ 'c' để tiếp tục, Enter để huỷ"
    if ($traLoi -ne 'c') { Write-Host "đã huỷ."; exit 1 }
}

# Vá HẾT, không riêng chuỗi đang dùng. Client còn hai danh sách gốc của teamobi làm đường lui
# (và một đường NJlink tải danh sách qua HTTP); để nguyên là có lúc nó lại nối ra ngoài.
foreach ($d in $ds) {
    $moi = Dung-Lai -Cu $d.Chuoi -MayMoi $May -CongMoi $Cong -TenMoi $Ten
    $bMoi = $ENC.GetBytes($moi)
    [Array]::Copy($bMoi, 0, $byte, $d.Offset, $bMoi.Length)
}

if (-not (Test-Path -LiteralPath $sao)) {
    Copy-Item -LiteralPath $duongDll -Destination $sao
    Write-Host "đã lưu bản cũ: $sao"
}

[System.IO.File]::WriteAllBytes($duongDll, $byte)

# Đọc lại từ đĩa để kiểm, không tin vào mảng byte trong bộ nhớ.
$kiem = [System.IO.File]::ReadAllBytes($duongDll)
if ($kiem.Length -ne $byte.Length) { throw "kích thước tệp đã đổi — chạy lại với -KhoiPhuc" }
$dsSau = Doc-DanhSach -B $kiem
foreach ($d in $dsSau) {
    $dau = ($d.Chuoi -split ',')[0] -split ':'
    if ($dau[1] -ne $May -or $dau[2] -ne $Cong) { throw "vá xong nhưng đọc lại vẫn thấy $($dau[1]):$($dau[2]) — chạy lại với -KhoiPhuc" }
}

Write-Host ""
Write-Host "Xong. $($dsSau.Count) danh sách đều trỏ về ${May}:${Cong}, tệp vẫn $($kiem.Length) byte."
if ($script:tenBiCat) {
    Write-Host "(tên hiển thị bị cắt cho vừa chỗ — đặt tên ngắn hơn bằng -Ten nếu muốn)"
}
Write-Host "Mở lại NinjaSchool_189.exe. Muốn quay về như cũ: .\doi-may-chu-pc.ps1 -KhoiPhuc"
Write-Host ""

} catch {
    Write-Host ""
    Write-Host "Lỗi: $($_.Exception.Message)"
    Write-Host ""
    exit 1
}
