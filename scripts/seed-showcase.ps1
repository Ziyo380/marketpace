param([string]$ApiUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
if (-not ([uri]$ApiUrl).IsLoopback) { throw 'This script is for a local demo instance only.' }
$api = $ApiUrl.TrimEnd('/') + '/api/v1'
$data = Get-Content (Join-Path $PSScriptRoot 'showcase-data.json') -Raw | ConvertFrom-Json
$demoPassword = 'Demo12345!'
$imageDirectory = Join-Path (Split-Path $PSScriptRoot -Parent) 'frontend-v2/apps/client/public/products'
$createdProducts = 0
$createdZones = 0
$createdOrders = 0
$sellerTokens = @{}
$demoProducts = [System.Collections.Generic.List[object]]::new()
function Send-Api([string]$path, [string]$method = 'GET', $body = $null, $headers = @{}, [int[]]$allow = @()) {
    $arguments = @{ Uri = "$api$path"; Method = $method; Headers = $headers; TimeoutSec = 30 }
    if ($null -ne $body) { $arguments.ContentType = 'application/json'; $arguments.Body = ConvertTo-Json $body -Depth 10 }
    try {
        $result = Invoke-RestMethod @arguments
        foreach ($item in $result) { Write-Output $item }
    }
    catch {
        if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -in $allow) { return $null }
        throw
    }
}
function Login([string]$username, [string]$password, [bool]$create = $false) {
    $credentials = @{ username = $username; password = $password }
    $session = Send-Api '/auth/login' 'POST' $credentials @{} @(401)
    if (-not $session -and $create) {
        Send-Api '/auth/register' 'POST' $credentials | Out-Null
        $session = Send-Api '/auth/login' 'POST' $credentials
    }
    if (-not $session) { throw "Cannot authenticate $username; existing credentials will not be changed." }
    return @{ Authorization = "Bearer $($session.accessToken)" }
}
$admin = Login 'demo-admin' 'Admin123!'
$categories = @(Send-Api '/categories')
foreach ($category in $data.categories) {
    if ($categories | Where-Object slug -eq $category.slug) { continue }
    $parent = $categories | Where-Object slug -eq $category.parent | Select-Object -First 1
    if (-not $parent) { throw "Missing parent category $($category.parent)" }
    $new = Send-Api '/categories' 'POST' @{ name = $category.name; slug = $category.slug; parentId = $parent.id; description = $category.name } $admin
    $categories += $new
}
$regions = @(Send-Api '/delivery/regions')
foreach ($definition in $data.stores) {
    $username = "demo-showcase-$($definition.key)"
    $sellerAuth = Login $username $demoPassword $true
    $seller = Send-Api '/sellers/me' 'GET' $null $sellerAuth @(404)
    if (-not $seller) { $seller = Send-Api '/sellers/apply' 'POST' $null $sellerAuth }
    if ($seller.status -eq 'PENDING') { $seller = Send-Api "/sellers/$($seller.id)/approve" 'PATCH' $null $admin }
    if ($seller.status -ne 'APPROVED') { throw "Demo seller $username is not approved" }
    # Re-login to obtain the newly granted SELLER role.
    $sellerAuth = Login $username $demoPassword
    $store = Send-Api '/stores/me' 'GET' $null $sellerAuth @(404)
    if (-not $store) { $store = Send-Api '/stores' 'POST' @{ name = $definition.name; slug = $definition.slug; description = $definition.description } $sellerAuth }
    if ($store.slug -ne $definition.slug) { throw "Unexpected existing store for $username" }
    if (-not $store.approved) { $store = Send-Api "/admin/stores/$($store.id)/approve" 'PATCH' $null $admin }
    $sellerTokens[$store.id] = $sellerAuth
    $zones = @(Send-Api "/seller/stores/$($store.id)/delivery-zones" 'GET' $null $sellerAuth)
    foreach ($region in $regions) {
        if ($zones | Where-Object regionCode -eq $region.code) { continue }
        $nearby = $region.code -in @('TASHKENT_CITY', 'TASHKENT_REGION')
        $fee = if ($region.code -eq 'TASHKENT_CITY') { 15000 } elseif ($nearby) { 22000 } else { 35000 }
        Send-Api "/seller/stores/$($store.id)/delivery-zones" 'POST' @{ regionCode = $region.code; deliveryFee = $fee; minDeliveryDays = $(if ($nearby) { 1 } else { 3 }); maxDeliveryDays = $(if ($nearby) { 2 } else { 5 }) } $sellerAuth | Out-Null
        $createdZones++
    }
    $own = (Send-Api '/products/me?size=100' 'GET' $null $sellerAuth).content
    $index = 0
    foreach ($spec in $definition.products) {
        $product = $own | Where-Object slug -eq $spec[1] | Select-Object -First 1
        if (-not $product) {
            $category = $categories | Where-Object slug -eq $spec[3] | Select-Object -First 1
            $product = Send-Api '/products' 'POST' @{ categoryId = $category.id; name = $spec[0]; slug = $spec[1]; description = "$($spec[0]). $($definition.description)"; price = $spec[2] } $sellerAuth
            Send-Api "/inventory/products/$($product.id)/add-stock" 'POST' @{ amount = 30 + $index * 5 } $sellerAuth | Out-Null
            $product = Send-Api "/products/me/$($product.id)/publish" 'PATCH' $null $sellerAuth
            $createdProducts++
        }
        $images = @(Send-Api "/products/me/$($product.id)/images" 'GET' $null $sellerAuth)
        if ($images.Count -eq 0) {
            $imagePath = Join-Path $imageDirectory $spec[4]
            $raw = & curl.exe --silent --show-error --fail-with-body -H "Authorization: $($sellerAuth.Authorization)" -F "file=@$imagePath;type=image/png" "$api/products/me/$($product.id)/images"
            if ($LASTEXITCODE -ne 0) { throw "Image upload failed for $($product.slug)" }
        }
        $variants = @(Send-Api "/products/me/$($product.id)/variants" 'GET' $null $sellerAuth)
        if ($index -lt 2) {
            foreach ($suffix in @('STANDARD', 'PLUS')) {
                $sku = "SHOWCASE-$($definition.key)-$index-$suffix".ToUpperInvariant()
                if ($variants | Where-Object sku -eq $sku) { continue }
                $price = [decimal]$spec[2] + $(if ($suffix -eq 'PLUS') { 50000 } else { 0 })
                $variant = Send-Api "/products/me/$($product.id)/variants" 'POST' @{ name = $suffix; sku = $sku; price = $price } $sellerAuth
                Send-Api "/products/me/$($product.id)/variants/$($variant.id)/add-stock" 'POST' @{ amount = 20 } $sellerAuth | Out-Null
            }
        }
        $demoProducts.Add($product)
        $index++
    }
    Write-Host "Ready: $($definition.slug), products=$index, regions=$($regions.Count)"
}
# Add only missing zones to the original demonstration store.
$novaAuth = Login 'demo-seller' 'Seller123!'
$nova = Send-Api '/stores/me' 'GET' $null $novaAuth
$novaZones = @(Send-Api "/seller/stores/$($nova.id)/delivery-zones" 'GET' $null $novaAuth)
foreach ($region in $regions) {
    if ($novaZones | Where-Object regionCode -eq $region.code) { continue }
    Send-Api "/seller/stores/$($nova.id)/delivery-zones" 'POST' @{ regionCode = $region.code; deliveryFee = $(if ($region.code -eq 'TASHKENT_CITY') { 0 } else { 25000 }); minDeliveryDays = 1; maxDeliveryDays = 4 } $novaAuth | Out-Null
    $createdZones++
}
$promos = (Send-Api '/admin/promocodes?size=100' 'GET' $null $admin).content
foreach ($promo in @(
    @{ code = 'DEMO10'; discountType = 'PERCENT'; discountValue = 10; minimumAmount = 100000; maxUses = 1000 },
    @{ code = 'DEMO50000'; discountType = 'FIXED'; discountValue = 50000; minimumAmount = 300000; maxUses = 500 },
    @{ code = 'DEMOSPORT'; discountType = 'PERCENT'; discountValue = 15; minimumAmount = 200000; maxUses = 500 }
)) {
    if (-not ($promos | Where-Object code -eq $promo.code)) { Send-Api '/admin/promocodes' 'POST' $promo $admin | Out-Null }
}
# Orders belong exclusively to this new demonstration customer.
# A repeat run never changes any existing orders.
$buyer = Login 'demo-showcase-buyer' $demoPassword $true
$existingOrders = Send-Api '/orders?size=100' 'GET' $null $buyer
if ($true) {
    $stages = @('PENDING', 'PACKING', 'READY_FOR_DISPATCH', 'IN_TRANSIT', 'DELIVERED', 'DELIVERY_FAILED', 'CANCELLED')
    $scenario = 0
    foreach ($stage in $stages) {
        $chosen = @($demoProducts[($scenario * 3) % $demoProducts.Count])
        if ($stage -eq 'DELIVERED') { $chosen += $demoProducts[19] }
        $alreadyCreated = @($existingOrders.content | Where-Object {
            @($_.sellerOrders.items.productId) -contains $chosen[0].id
        })
        if ($alreadyCreated.Count -gt 0) { $scenario++; continue }
        $cart = Send-Api '/cart' 'GET' $null $buyer
        if (@($cart.items | Where-Object { $_.productId -notin @($chosen.id) }).Count -gt 0) {
            throw 'Unexpected items in showcase cart; they will not be removed.'
        }
        foreach ($product in $chosen) {
            if ($cart.items | Where-Object productId -eq $product.id) { continue }
            $body = @{ productId = $product.id; quantity = 1 }
            $variants = @(Send-Api "/products/$($product.id)/variants")
            if ($variants.Count -gt 0) { $body.variantId = ($variants | Where-Object { $_.active -and $_.availableQuantity -gt 0 } | Select-Object -First 1).id }
            Send-Api '/cart/items' 'POST' $body $buyer | Out-Null
        }
        $checkoutHeaders = @{ Authorization = $buyer.Authorization; 'Idempotency-Key' = "showcase-$stage-v1" }
        $checkout = @{ delivery = @{ recipientName = 'Demo Customer'; recipientPhone = '+998900000000'; region = 'TASHKENT_CITY'; city = 'Tashkent'; addressLine = 'Demo address, 1' } }
        if ($stage -eq 'PENDING') { $checkout.promocode = 'DEMO10' }
        $order = Send-Api '/orders' 'POST' $checkout $checkoutHeaders
        $createdOrders++
        if ($stage -eq 'CANCELLED') { Send-Api "/orders/$($order.orderId)/cancel" 'PATCH' $null $buyer | Out-Null }
        elseif ($stage -ne 'PENDING') {
            foreach ($part in $order.sellerOrders) {
                $sellerAuth = $sellerTokens[$part.storeId]
                Send-Api "/seller/orders/$($part.sellerOrderId)/confirm" 'PATCH' $null $sellerAuth | Out-Null
                $shipmentPath = "/seller/orders/$($part.sellerOrderId)/shipment"
                Send-Api $shipmentPath 'PATCH' @{ targetStatus = 'PACKING' } $sellerAuth | Out-Null
                if ($stage -ne 'PACKING') { Send-Api $shipmentPath 'PATCH' @{ targetStatus = 'READY_FOR_DISPATCH' } $sellerAuth | Out-Null }
                if ($stage -in @('IN_TRANSIT', 'DELIVERED', 'DELIVERY_FAILED')) {
                    Send-Api "$shipmentPath/dispatch" 'POST' @{ carrierName = 'Demo Delivery'; trackingNumber = "DEMO-$scenario-$($part.storeId.Substring(0,8))" } $sellerAuth | Out-Null
                }
                if ($stage -eq 'DELIVERED') { Send-Api "$shipmentPath/confirm-delivery" 'POST' $null $sellerAuth | Out-Null }
                if ($stage -eq 'DELIVERY_FAILED') { Send-Api "$shipmentPath/delivery-failed" 'POST' @{ failureReason = 'Demo: recipient unavailable' } $sellerAuth | Out-Null }
            }
        }
        if ($stage -eq 'DELIVERED') {
            foreach ($product in $chosen) {
                $review = Send-Api '/reviews' 'POST' @{ productId = $product.id; rating = 5; comment = 'Demo review: item received, delivery completed.' } $buyer
                Send-Api "/reviews/$($review.id)/approve" 'PATCH' $null $admin | Out-Null
            }
        }
        Write-Host "New demonstration order: $stage"
        $scenario++
    }
}
$favorites = (Send-Api '/favorites?size=100' 'GET' $null $buyer).content
foreach ($product in @($demoProducts[2], $demoProducts[10], $demoProducts[20])) {
    if (-not ($favorites | Where-Object { $_.product.id -eq $product.id })) { Send-Api "/favorites/$($product.id)" 'POST' $null $buyer | Out-Null }
}
$total = Send-Api '/products?size=1'
Write-Host "Complete: newProducts=$createdProducts, newDeliveryZones=$createdZones, newOrders=$createdOrders, publicProducts=$($total.totalElements)"
