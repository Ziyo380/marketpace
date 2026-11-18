param(
    [string]$DbHost = $(if ($env:DB_HOST) { $env:DB_HOST } else { 'localhost' }),
    [int]$DbPort = $(if ($env:DB_PORT) { [int]$env:DB_PORT } else { 5432 }),
    [string]$DbName = $(if ($env:DB_NAME) { $env:DB_NAME } else { 'marketplace' }),
    [string]$DbUser = $(if ($env:DB_USERNAME) { $env:DB_USERNAME } else { 'postgres' }),
    [string]$DbPassword = $env:DB_PASSWORD
)

$ErrorActionPreference = 'Stop'
$scriptPath = Join-Path $PSScriptRoot 'demo-data.sql'

if ([string]::IsNullOrWhiteSpace($DbPassword)) {
    $securePassword = Read-Host 'PostgreSQL password' -AsSecureString
    $DbPassword = [System.Net.NetworkCredential]::new('', $securePassword).Password
}

$previousPassword = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $DbPassword
    & psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -v ON_ERROR_STOP=1 -f $scriptPath
    if ($LASTEXITCODE -ne 0) {
        throw "psql exited with code $LASTEXITCODE"
    }
    Write-Host 'Demo data loaded successfully.' -ForegroundColor Green
}
finally {
    $env:PGPASSWORD = $previousPassword
}
