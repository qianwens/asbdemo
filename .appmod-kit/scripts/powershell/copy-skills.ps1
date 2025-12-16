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
            # Check if it's a directory
            if (Test-Path $skillFileName -PathType Container) {
                # It's a folder - copy the entire folder to the task folder
                $taskFolder = Split-Path -Parent $TaskFilePath
                $skillFolderName = Split-Path -Leaf $skillFileName
                $destinationPath = Join-Path -Path $taskFolder -ChildPath $skillFolderName
                
                Write-Host "Copying folder: $skillFileName -> $destinationPath" -ForegroundColor Yellow
                Copy-Item -Path $skillFileName -Destination $destinationPath -Recurse -Force
                
                # Check if there's a skill.md in the folder to include in content
                $skillMdPath = Join-Path -Path $skillFileName -ChildPath "skill.md"
                if (Test-Path $skillMdPath) {
                    $content = Get-Content -Path $skillMdPath -Raw
                    
                    # Extract description from YAML front matter
                    $description = ""
                    if ($content -match '(?s)^---\s*\n(.*?)\n---') {
                        $yamlContent = $Matches[1]
                        foreach ($line in $yamlContent -split '\n') {
                            if ($line -match '^\s*description:\s*(.+)$') {
                                $description = $Matches[1].Trim()
                                break
                            }
                        }
                    }
                    
                    # Add instruction to use the skill.md in the task folder
                    $relativeSkillPath = "$skillFolderName/skill.md"
                    $allContent += "### Skill: $skillFolderName`n"
                    if ($description) {
                        $allContent += "**Description:** $description`n`n"
                    }
                    $allContent += "Use the skill instructions from: ``$relativeSkillPath```n"
                    $allContent += "---`n"
                }
                
                $copiedCount++
            }
            else {
                # It's a file - read and append content
                $content = Get-Content -Path $skillFileName -Raw
                $allContent += "### Skill: $skillFileName`n"
                $allContent += $content + "`n"
                $allContent += "---`n"
                $copiedCount++
            }
        }
        catch {
            Write-Error "Failed to process skill '$skillFileName': $_"
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
