$ErrorActionPreference = 'Stop'

$dockerExe = 'C:\Program Files\Docker\Docker\Docker Desktop.exe'
$dockerCliDir = 'C:\Program Files\Docker\Docker\resources\bin'
$dockerProfileRoot = 'D:\DockerProfile'
$dockerLocalAppData = Join-Path $dockerProfileRoot 'Local'
$dockerRoamingAppData = Join-Path $dockerProfileRoot 'Roaming'
$projectRoot = Split-Path -Parent $PSScriptRoot

if (-not (Test-Path -LiteralPath $dockerExe)) {
    throw "Docker Desktop not found: $dockerExe"
}

New-Item -ItemType Directory -Force -Path $dockerLocalAppData, $dockerRoamingAppData | Out-Null

$env:LOCALAPPDATA = $dockerLocalAppData
$env:APPDATA = $dockerRoamingAppData
$env:Path = "$dockerCliDir;$env:Path"

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Start-Process -FilePath $dockerExe -WindowStyle Hidden

    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        Start-Sleep -Seconds 2
        docker info *> $null
        if ($LASTEXITCODE -eq 0) {
            $ready = $true
            break
        }
    }

    if (-not $ready) {
        throw 'Docker Engine did not become ready within 120 seconds.'
    }
}

docker compose --project-directory $projectRoot up -d minio
if ($LASTEXITCODE -ne 0) {
    throw 'Docker started, but MinIO could not be started.'
}

Write-Host 'Docker Desktop and marketplace-minio are running.' -ForegroundColor Green
