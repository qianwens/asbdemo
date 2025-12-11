param(
    [Parameter(Mandatory=$true)]
    [string[]]$SkillFileNames,
    
    [Parameter(Mandatory=$true)]
    [string]$TaskFilePath
)

# Get the repository root (assuming script is in .appmod-kit/scripts/powershell/)
$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))

# Create parent directory if it doesn't exist
$parentDir = Split-Path -Parent $TaskFilePath
if ($parentDir -and -not (Test-Path $parentDir)) {
    Write-Host "Creating directory: $parentDir" -ForegroundColor Yellow
    New-Item -Path $parentDir -ItemType Directory -Force | Out-Null
}

# Process each skill and collect content
$copiedCount = 0
$failedCount = 0
$allContent = ""

foreach ($skillFileName in $SkillFileNames) {
    
    if (Test-Path $skillFileName) {
        try {
            $content = Get-Content -Path $skillFileName -Raw
            $allContent += "### Skill: $skillFileName`n"
            $allContent += $content + "`n"
            $allContent += "---`n"
            $copiedCount++
        }
        catch {
            Write-Error "Failed to read skill '$skillFileName': $_"
            $failedCount++
        }
    }
    else {
        Write-Warning "Skill file not found: $skillFileName"
        $failedCount++
    }
}

# Write all content to the task file
if ($allContent.Count -gt 0) {
    try {
        $allContent | Out-File -FilePath $TaskFilePath -Encoding UTF8 -Force -Append
        Write-Host "`nWrote all skills to: $TaskFilePath" -ForegroundColor Cyan
    }
    catch {
        Write-Error "Failed to write to task file: $_"
        exit 1
    }
}

# Summary
Write-Host "`nSummary:" -ForegroundColor Cyan
Write-Host "  Copied: $copiedCount" -ForegroundColor Green
Write-Host "  Failed: $failedCount" -ForegroundColor $(if ($failedCount -gt 0) { "Red" } else { "Gray" })

if ($failedCount -gt 0) {
    exit 1
}
