[CmdletBinding()]
param(
    [switch]$SkipInstall
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $root 'frontend'
$backend = Join-Path $root 'backend'
$static = Join-Path $backend 'src\main\resources\static'
$browserDist = Join-Path $frontend 'dist\frontend\browser'

Push-Location $frontend
try {
    if (-not $SkipInstall) {
        & npm ci
        if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' }
    }
    else {
        Write-Output 'Skipping npm ci; using the existing node_modules.'
    }

    & npm run build
    if ($LASTEXITCODE -ne 0) { throw 'Angular production build failed' }
}
finally {
    Pop-Location
}

if (-not (Test-Path -LiteralPath $browserDist)) {
    throw "Angular build output not found: $browserDist"
}

if (Test-Path -LiteralPath $static) {
    Remove-Item -LiteralPath $static -Recurse -Force
}
New-Item -ItemType Directory -Path $static | Out-Null
Copy-Item -Path (Join-Path $browserDist '*') -Destination $static -Recurse -Force

Push-Location $backend
try {
    & mvn -DskipTests clean package
    if ($LASTEXITCODE -ne 0) { throw 'Maven package failed' }
}
finally {
    Pop-Location
}

Write-Output "Built single deployable jar: $backend\target\payroll-service-0.0.1-SNAPSHOT.jar"
