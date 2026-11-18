param(
    [string]$ApiUrl = 'http://localhost:8080',
    [string]$SellerUsername = 'demo-seller',
    [string]$SellerPassword = 'Seller123!'
)

$ErrorActionPreference = 'Stop'
$api = $ApiUrl.TrimEnd('/')
$curl = if ($IsWindows) { 'curl.exe' } else { 'curl' }
$imagesDirectory = Join-Path (Split-Path $PSScriptRoot -Parent) 'frontend-v2/apps/client/public/products'
$imageBySlug = @{
    'nova-x1' = 'electronics.png'
    'airbuds-flow' = 'audio.png'
    'luma-desk' = 'home.png'
    'sahar-no-04' = 'beauty.png'
    'move-bottle' = 'sport.png'
    'orbit-watch-s' = 'watch.png'
    'studio-headphones' = 'audio.png'
    'mizu-ceramic-set' = 'ceramic.png'
    'aura-skin-serum' = 'beauty.png'
    'flex-yoga-mat' = 'sport.png'
    'halo-desk-lamp' = 'home.png'
    'pulse-mini-speaker' = 'audio.png'
}

$loginBody = @{ username = $SellerUsername; password = $SellerPassword } | ConvertTo-Json
$login = Invoke-RestMethod -Uri "$api/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $loginBody
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
$products = Invoke-RestMethod -Uri "$api/api/v1/products/me?size=100" -Headers $headers
$uploaded = 0

foreach ($product in $products.content) {
    $filename = $imageBySlug[$product.slug]
    if (-not $filename) { continue }
    $existing = Invoke-RestMethod -Uri "$api/api/v1/products/me/$($product.id)/images" -Headers $headers
    if ($existing -and $existing.Count -gt 0) { continue }

    $imagePath = Join-Path $imagesDirectory $filename
    if (-not (Test-Path -LiteralPath $imagePath)) { throw "Missing image: $imagePath" }
    $raw = & $curl --silent --show-error --fail-with-body `
        -H "Authorization: Bearer $($login.accessToken)" `
        -F "file=@$imagePath;type=image/png" `
        "$api/api/v1/products/me/$($product.id)/images"
    if ($LASTEXITCODE -ne 0) { throw "Upload failed for $($product.slug): $raw" }
    $image = $raw | ConvertFrom-Json
    $response = Invoke-WebRequest -Uri $image.url -UseBasicParsing -TimeoutSec 20
    if ($response.StatusCode -ne 200) { throw "Image for $($product.slug) is not readable" }
    $uploaded++
}

Write-Host "Demo images ready: uploaded $uploaded new images." -ForegroundColor Green
