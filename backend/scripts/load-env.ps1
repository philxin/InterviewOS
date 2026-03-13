param(
    [string]$EnvFile = (Join-Path $PSScriptRoot "..\.env"),
    [string]$Run
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-EnvPath {
    param([string]$PathValue)

    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }

    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $PathValue))
}

function Normalize-EnvValue {
    param([string]$Value)

    if ($null -eq $Value) {
        return ""
    }

    $trimmed = $Value.Trim()
    if (
        $trimmed.Length -ge 2 -and
        (
            ($trimmed.StartsWith('"') -and $trimmed.EndsWith('"')) -or
            ($trimmed.StartsWith("'") -and $trimmed.EndsWith("'"))
        )
    ) {
        return $trimmed.Substring(1, $trimmed.Length - 2)
    }

    return $trimmed
}

function Import-DotEnv {
    param([string]$FilePath)

    if (-not (Test-Path -LiteralPath $FilePath)) {
        throw "Env file not found: $FilePath"
    }

    foreach ($line in Get-Content -LiteralPath $FilePath -Encoding UTF8) {
        $text = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($text) -or $text.StartsWith("#")) {
            continue
        }

        $parts = $text -split "=", 2
        if ($parts.Count -ne 2) {
            throw "Invalid .env line: $line"
        }

        $name = $parts[0].Trim()
        if ([string]::IsNullOrWhiteSpace($name)) {
            throw "Invalid env key in line: $line"
        }

        $value = Normalize-EnvValue -Value $parts[1]
        [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
        Write-Host "Loaded $name"
    }
}

$resolvedEnvFile = Resolve-EnvPath -PathValue $EnvFile
Import-DotEnv -FilePath $resolvedEnvFile

if (-not [string]::IsNullOrWhiteSpace($Run)) {
    Write-Host "Running: $Run"
    Invoke-Expression $Run
} else {
    Write-Host "Environment loaded from $resolvedEnvFile"
    Write-Host "Tip: dot-source this script to keep variables in the current shell."
    Write-Host "Example: . .\scripts\load-env.ps1"
}
