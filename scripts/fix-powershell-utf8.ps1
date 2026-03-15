param(
    [switch]$PersistProfile,
    [switch]$EnableCurrentUserScriptExecution,
    [switch]$ConfigureJavaTools
)

$ErrorActionPreference = "Stop"

function Set-Utf8Console {
    # Align the console input/output encoding to UTF-8 for the current session.
    chcp 65001 > $null
    [Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
    [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
    $global:OutputEncoding = [Console]::OutputEncoding

    # Make common cmdlets default to UTF-8 when they write files.
    $PSDefaultParameterValues["Out-File:Encoding"] = "utf8"
    $PSDefaultParameterValues["Set-Content:Encoding"] = "utf8"
    $PSDefaultParameterValues["Add-Content:Encoding"] = "utf8"
}

function Enable-ProfileExecution {
    $policies = Get-ExecutionPolicy -List
    $effectivePolicy = Get-ExecutionPolicy
    $machinePolicy = ($policies | Where-Object { $_.Scope -eq "MachinePolicy" }).ExecutionPolicy
    $userPolicy = ($policies | Where-Object { $_.Scope -eq "UserPolicy" }).ExecutionPolicy
    $processPolicy = ($policies | Where-Object { $_.Scope -eq "Process" }).ExecutionPolicy
    $currentUserPolicy = ($policies | Where-Object { $_.Scope -eq "CurrentUser" }).ExecutionPolicy

    if ($machinePolicy -and $machinePolicy -ne "Undefined") {
        Write-Host "MachinePolicy is overriding execution policy: $machinePolicy"
        Write-Host "Skip updating CurrentUser policy. Ask the administrator if profile scripts must be enabled."
        return
    }

    if ($userPolicy -and $userPolicy -ne "Undefined") {
        Write-Host "UserPolicy is overriding execution policy: $userPolicy"
        Write-Host "Skip updating CurrentUser policy. Ask the administrator if profile scripts must be enabled."
        return
    }

    if ($currentUserPolicy -eq "RemoteSigned") {
        Write-Host "CurrentUser execution policy is already RemoteSigned."
    } else {
        try {
            Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned -Force -ErrorAction Stop
            Write-Host "CurrentUser execution policy set to RemoteSigned."
        } catch {
            Write-Host "Unable to update CurrentUser execution policy automatically."
            Write-Host "Reason: $($_.Exception.Message)"
            Write-Host "You can continue using this script with -ExecutionPolicy Bypass, or open an elevated PowerShell and retry."
            return
        }
    }

    if ($processPolicy -and $processPolicy -ne "Undefined" -and $processPolicy -ne $effectivePolicy) {
        Write-Host "Process execution policy is currently overriding this session: $processPolicy"
        Write-Host "Open a new PowerShell window later to verify profile auto-loading."
    }
}

function Add-ProfileBlock {
    $profilePath = $PROFILE
    $profileDir = Split-Path -Parent $profilePath
    $startMarker = "# >>> interviewos utf8 setup >>>"
    $endMarker = "# <<< interviewos utf8 setup <<<"
    $block = @'
# >>> interviewos utf8 setup >>>
chcp 65001 > $null
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
$PSDefaultParameterValues["Out-File:Encoding"] = "utf8"
$PSDefaultParameterValues["Set-Content:Encoding"] = "utf8"
$PSDefaultParameterValues["Add-Content:Encoding"] = "utf8"
# <<< interviewos utf8 setup <<<
'@

    if (-not (Test-Path $profileDir)) {
        New-Item -ItemType Directory -Path $profileDir -Force | Out-Null
    }

    if (-not (Test-Path $profilePath)) {
        New-Item -ItemType File -Path $profilePath -Force | Out-Null
    }

    $existing = Get-Content -Path $profilePath -Raw
    if ($existing -match [regex]::Escape($startMarker)) {
        Write-Host "Profile UTF-8 block already exists: $profilePath"
        return
    }

    $separator = ""
    if ($existing.Length -gt 0 -and -not $existing.EndsWith([Environment]::NewLine)) {
        $separator = [Environment]::NewLine
    }

    Add-Content -Path $profilePath -Value ($separator + $block)
    Write-Host "Profile updated: $profilePath"
}

function Set-JavaToolEncoding {
    # Keep Java and Maven output aligned with the terminal encoding.
    [Environment]::SetEnvironmentVariable("JAVA_TOOL_OPTIONS", "-Dfile.encoding=UTF-8", "User")
    [Environment]::SetEnvironmentVariable("MAVEN_OPTS", "-Dfile.encoding=UTF-8", "User")
    $env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
    $env:MAVEN_OPTS = "-Dfile.encoding=UTF-8"
}

Set-Utf8Console
Write-Host "Current session switched to UTF-8."

if ($EnableCurrentUserScriptExecution) {
    Enable-ProfileExecution
}

if ($PersistProfile) {
    Add-ProfileBlock
}

if ($ConfigureJavaTools) {
    Set-JavaToolEncoding
    Write-Host "JAVA_TOOL_OPTIONS and MAVEN_OPTS set to UTF-8."
}

Write-Host ""
Write-Host "Recommended validation:"
Write-Host "  Get-ChildItem .\docs\plan"
Write-Host "  Get-Content -Encoding UTF8 '.\docs\plan\<your-doc>.md' | Select-Object -First 5"
Write-Host "  `$PSVersionTable.PSVersion"
