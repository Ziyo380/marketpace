param([string]$ApiUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
$api = $ApiUrl.TrimEnd('/') + '/api/v1'
$credentials = @{ username = 'demo-showcase-buyer'; password = 'Demo12345!' } | ConvertTo-Json
$session = Invoke-RestMethod "$api/auth/login" -Method Post -ContentType 'application/json' -Body $credentials
$headers = @{ Authorization = "Bearer $($session.accessToken)" }
$orders = Invoke-RestMethod "$api/orders?size=100" -Headers $headers
if ($orders.totalElements -lt 7) { throw 'Missing showcase orders' }
$shipmentCount = 0
$eventCount = 0
foreach ($order in $orders.content) {
    $shipments = Invoke-RestMethod "$api/orders/$($order.orderId)/shipments" -Headers $headers
    foreach ($shipment in $shipments) {
        $history = Invoke-RestMethod "$api/orders/$($order.orderId)/shipments/$($shipment.id)/history" -Headers $headers
        if ($history.Count -lt 2) { throw 'Shipment has no transition history' }
        if ($history[-1].toStatus -ne $shipment.status) { throw 'Last history event does not match shipment status' }
        $shipmentCount++
        $eventCount += $history.Count
    }
}
if ($shipmentCount -lt 6 -or $eventCount -lt 24) { throw 'Incomplete shipment scenarios' }
$catalog = Invoke-RestMethod "$api/products?size=100"
$imageCount = 0
foreach ($product in $catalog.content | Where-Object { $_.storeSlug -like 'demo-*' }) {
    $images = Invoke-RestMethod "$api/stores/$($product.storeSlug)/products/$($product.slug)/images"
    if ($images.Count -lt 1) { throw "No image for $($product.slug)" }
    $url = [uri]::new([uri]$ApiUrl, [string]$images[0].url)
    $response = Invoke-WebRequest $url -TimeoutSec 15
    if ($response.StatusCode -ne 200) { throw 'Image unavailable' }
    $imageCount++
}
Write-Host "Verified: showcaseOrders=$($orders.totalElements), shipments=$shipmentCount, historyEvents=$eventCount, newProductImages=$imageCount"
