param(
    [string]$EnvFile = (Join-Path $PSScriptRoot "..\.env"),
    [string]$JavaHome = "D:\develop\java21",
    [string]$Profile,
    [switch]$SkipEnv,
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$loadEnvScript = Join-Path $PSScriptRoot "load-env.ps1"

if (-not $SkipEnv) {
    . $loadEnvScript -EnvFile $EnvFile
}

if (-not (Test-Path -LiteralPath $JavaHome)) {
    throw "JAVA_HOME not found: $JavaHome"
}

$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome\bin;$env:PATH"

if (-not [string]::IsNullOrWhiteSpace($Profile)) {
    $env:SPRING_PROFILES_ACTIVE = $Profile
}

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "SPRING_PROFILES_ACTIVE=$env:SPRING_PROFILES_ACTIVE"
Write-Host "Starting Spring Boot from $projectRoot"

if ($DryRun) {
    Write-Host "Dry run enabled, startup command was not executed."
    return
}

Push-Location $projectRoot
try {
    & .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}
