param(
    [string]$ApiUrl = 'http://localhost:8080',
    [string]$CustomerUrl = 'http://localhost:3000/ru',
    [string]$DashboardUrl = 'http://localhost:4200',
    [int]$ExpectedProducts = 12,
    [switch]$CheckImages
)

$ErrorActionPreference = 'Stop'
$api = $ApiUrl.TrimEnd('/')
$customerOrigin = ([uri]$CustomerUrl).GetLeftPart([System.UriPartial]::Authority)
$dashboardOrigin = ([uri]$DashboardUrl).GetLeftPart([System.UriPartial]::Authority)

function Assert-HttpOk([string]$url) {
    $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 20
    if ($response.StatusCode -ne 200) { throw "$url returned HTTP $($response.StatusCode)" }
}

Assert-HttpOk $CustomerUrl
Assert-HttpOk $DashboardUrl

$categories = Invoke-RestMethod -Uri "$api/api/v1/categories" -TimeoutSec 20
if ($categories.Count -lt 5) { throw "Expected at least 5 categories, got $($categories.Count)" }
$customerCategories = Invoke-RestMethod -Uri "$customerOrigin/api/v1/categories" -TimeoutSec 20
$dashboardCategories = Invoke-RestMethod -Uri "$dashboardOrigin/api/v1/categories" -TimeoutSec 20
if ($customerCategories.Count -ne $categories.Count -or $dashboardCategories.Count -ne $categories.Count) {
    throw 'Frontend API proxy returned different categories than backend'
}

$catalog = Invoke-RestMethod -Uri "$api/api/v1/products?size=20" -TimeoutSec 20
if ($catalog.totalElements -lt $ExpectedProducts) {
    throw "Expected at least $ExpectedProducts products, got $($catalog.totalElements)"
}
if (@($catalog.content | Where-Object { $null -eq $_.price -or $_.price -le 0 }).Count -gt 0) {
    throw 'Catalog contains a product without a positive price'
}
if ($CheckImages) {
    foreach ($product in $catalog.content) {
        $images = Invoke-RestMethod -Uri "$api/api/v1/stores/$($product.storeSlug)/products/$($product.slug)/images" -TimeoutSec 20
        if (-not $images -or $images.Count -eq 0) { throw "No image for $($product.slug)" }
        Assert-HttpOk $images[0].url
    }
}

$body = @{ username = 'demo-customer'; password = 'Customer123!' } | ConvertTo-Json
$login = Invoke-RestMethod -Uri "$customerOrigin/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 20
if ([string]::IsNullOrWhiteSpace($login.accessToken)) { throw 'Demo customer login returned no access token' }
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
$cart = Invoke-RestMethod -Uri "$customerOrigin/api/v1/cart" -Headers $headers -TimeoutSec 20
if ($null -eq $cart.items) { throw 'Demo customer cart was not returned' }

$sellerBody = @{ username = 'demo-seller'; password = 'Seller123!' } | ConvertTo-Json
$sellerLogin = Invoke-RestMethod -Uri "$dashboardOrigin/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $sellerBody -TimeoutSec 20
$sellerHeaders = @{ Authorization = "Bearer $($sellerLogin.accessToken)" }
$sellerProducts = Invoke-RestMethod -Uri "$dashboardOrigin/api/v1/products/me?size=20" -Headers $sellerHeaders -TimeoutSec 20
if ($sellerProducts.totalElements -lt $ExpectedProducts) { throw 'Seller dashboard cannot load demo products' }

$adminBody = @{ username = 'demo-admin'; password = 'Admin123!' } | ConvertTo-Json
$adminLogin = Invoke-RestMethod -Uri "$dashboardOrigin/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $adminBody -TimeoutSec 20
$adminHeaders = @{ Authorization = "Bearer $($adminLogin.accessToken)" }
$statistics = Invoke-RestMethod -Uri "$dashboardOrigin/api/v1/admin/statistics" -Headers $adminHeaders -TimeoutSec 20
if ($statistics.products -lt $ExpectedProducts) { throw 'Admin dashboard cannot load platform statistics' }

Write-Host "Smoke check passed: $($categories.Count) categories, $($catalog.totalElements) products, customer/seller/admin, cart, both frontends, images=$CheckImages." -ForegroundColor Green
