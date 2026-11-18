param(
    [string]$EnvFile = '.env',
    [string]$ProjectName = 'marketplace-backend',
    [string]$OutputDirectory = 'backups'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$envPath = if ([IO.Path]::IsPathRooted($EnvFile)) { $EnvFile } else { Join-Path $projectRoot $EnvFile }
$backupPath = if ([IO.Path]::IsPathRooted($OutputDirectory)) { $OutputDirectory } else { Join-Path $projectRoot $OutputDirectory }
if (-not (Test-Path -LiteralPath $envPath)) { throw "Environment file not found: $envPath" }
New-Item -ItemType Directory -Path $backupPath -Force | Out-Null
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$compose = @('--env-file', $envPath, '-p', $ProjectName)

& docker compose @compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/marketplace-backup.dump'
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL backup failed' }
& docker compose @compose cp 'postgres:/tmp/marketplace-backup.dump' (Join-Path $backupPath "marketplace-$timestamp.dump")
if ($LASTEXITCODE -ne 0) { throw 'Could not copy PostgreSQL backup' }

& docker compose @compose exec -T minio sh -c 'tar -czf /tmp/marketplace-objects.tgz -C /data .'
if ($LASTEXITCODE -ne 0) { throw 'Object storage backup failed' }
& docker compose @compose cp 'minio:/tmp/marketplace-objects.tgz' (Join-Path $backupPath "marketplace-objects-$timestamp.tgz")
if ($LASTEXITCODE -ne 0) { throw 'Could not copy object storage backup' }

Write-Host "Backups saved in $backupPath" -ForegroundColor Green
