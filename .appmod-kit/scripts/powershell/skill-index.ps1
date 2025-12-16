#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Generate skill index from markdown files with YAML front matter.

.DESCRIPTION
    Scans .appmod-kit/skills folder for built-in skills and .appmod-kit/custom/skills
    for custom skills, extracts YAML front matter (name and description), and generates
    skill-index.json with separate built-in and custom arrays.

.EXAMPLE
    .\skill-index.ps1
#>

param(
    [string]$BuiltInSkillsPath = ".appmod-kit/skills",
    [string]$CustomSkillsPath = ".appmod-kit/custom/skills",
    [string]$OutputPath = ".appmod-kit/skills/skill-index.json"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Extract-YamlFrontMatter {
    param(
        [string]$FilePath,
        [string]$RelativePath
    )
    
    $content = Get-Content -Path $FilePath -Raw
    
    # Match YAML front matter between --- delimiters
    if ($content -match '(?s)^---\s*\n(.*?)\n---') {
        $yamlContent = $Matches[1]
        
        # Extract name and description
        $name = $null
        $description = $null
        
        foreach ($line in $yamlContent -split '\n') {
            if ($line -match '^\s*name:\s*(.+)$') {
                $name = $Matches[1].Trim()
            }
            elseif ($line -match '^\s*description:\s*(.+)$') {
                $description = $Matches[1].Trim()
            }
        }
        
        if ($name -and $description) {
            return @{
                name = $name
                description = $description
                path = $RelativePath
            }
        }
    }
    
    return $null
}

function Get-BuiltInSkills {
    param([string]$Path)
    
    $skills = @()
    
    if (-not (Test-Path -Path $Path)) {
        Write-Warning "Built-in skills directory not found: $Path"
        return $skills
    }
    
    $markdownFiles = @(Get-ChildItem -Path $Path -Filter "*.md" -File)
    
    if ($markdownFiles.Count -eq 0) {
        Write-Warning "No markdown files found in built-in skills"
        return $skills
    }
    
    foreach ($file in $markdownFiles) {
        Write-Host "Processing built-in skill: $($file.Name)"
        
        $relativePath = "$Path/$($file.Name)".Replace("\\", "/")
        $skill = Extract-YamlFrontMatter -FilePath $file.FullName -RelativePath $relativePath
        
        if ($skill) {
            $skills += $skill
            Write-Host "  Extracted: $($skill.name)" -ForegroundColor Green
        }
        else {
            Write-Warning "  No valid YAML front matter found in: $($file.Name)"
        }
    }
    
    return $skills
}

function Get-CustomSkills {
    param([string]$Path)
    
    $skills = @()
    
    # Check if custom folder exists first
    $customFolder = Split-Path -Path $Path -Parent
    if (-not (Test-Path -Path $customFolder)) {
        Write-Host "Custom folder not found, skipping custom skills" -ForegroundColor DarkGray
        return $skills
    }
    
    # Check if custom skills directory exists
    if (-not (Test-Path -Path $Path)) {
        Write-Host "Custom skills directory not found, skipping custom skills" -ForegroundColor DarkGray
        return $skills
    }
    
    $folders = @(Get-ChildItem -Path $Path -Directory)
    
    if ($folders.Count -eq 0) {
        Write-Host "No custom skill folders found"
        return $skills
    }
    
    foreach ($folder in $folders) {
        $skillMdPath = Join-Path -Path $folder.FullName -ChildPath "skill.md"
        
        if (Test-Path -Path $skillMdPath) {
            Write-Host "Processing custom skill: $($folder.Name)"
            
            $content = Get-Content -Path $skillMdPath -Raw
            
            # Match YAML front matter between --- delimiters
            if ($content -match '(?s)^---\s*\n(.*?)\n---') {
                $yamlContent = $Matches[1]
                
                # Extract name and description
                $name = $null
                $description = $null
                
                foreach ($line in $yamlContent -split '\n') {
                    if ($line -match '^\s*name:\s*(.+)$') {
                        $name = $Matches[1].Trim()
                    }
                    elseif ($line -match '^\s*description:\s*(.+)$') {
                        $description = $Matches[1].Trim()
                    }
                }
                
                if ($name -and $description) {
                    $relativePath = "$CustomSkillsPath/$($folder.Name)".Replace("\\", "/")
                    $skill = @{
                        path = $relativePath
                        name = $name
                        description = $description
                    }
                    $skills += $skill
                    Write-Host "  Extracted: $name" -ForegroundColor Green
                }
                else {
                    Write-Warning "  Incomplete YAML front matter in: $($folder.Name)/skill.md"
                }
            }
            else {
                Write-Warning "  No valid YAML front matter found in: $($folder.Name)/skill.md"
            }
        }
        else {
            Write-Host "  Skipping folder (no skill.md): $($folder.Name)" -ForegroundColor DarkGray
        }
    }
    
    return $skills
}

# Process built-in skills
Write-Host "`n=== Processing Built-In Skills ===" -ForegroundColor Cyan
$builtInSkills = @(Get-BuiltInSkills -Path $BuiltInSkillsPath)

# Process custom skills
Write-Host "`n=== Processing Custom Skills ===" -ForegroundColor Cyan
$customSkills = @(Get-CustomSkills -Path $CustomSkillsPath)

# Ensure output directory exists
$outputDir = Split-Path -Path $OutputPath -Parent
if ($outputDir -and -not (Test-Path -Path $outputDir)) {
    Write-Host "Creating output directory: $outputDir"
    try {
        New-Item -Path $outputDir -ItemType Directory -Force -ErrorAction Stop | Out-Null
    }
    catch {
        Write-Error "Failed to create output directory: $outputDir. Error: $_"
        exit 1
    }
}

# Generate JSON output with custom and built-in structure
# Convert each array separately to ensure proper formatting
$customJson = ConvertTo-Json -InputObject $customSkills -Depth 10 -Compress:$false
$builtInJson = ConvertTo-Json -InputObject $builtInSkills -Depth 10 -Compress:$false

# Handle single-item arrays that ConvertTo-Json doesn't wrap
if ($customSkills.Count -eq 1) {
    $customJson = "[$customJson]"
}
if ($builtInSkills.Count -eq 1) {
    $builtInJson = "[$builtInJson]"
}
if ($customSkills.Count -eq 0) {
    $customJson = "[]"
}
if ($builtInSkills.Count -eq 0) {
    $builtInJson = "[]"
}

# Indent the JSON arrays properly
$customJsonIndented = ($customJson -split "`n" | ForEach-Object { "  $_" }) -join "`n"
$builtInJsonIndented = ($builtInJson -split "`n" | ForEach-Object { "  $_" }) -join "`n"

# Build the final JSON structure
$jsonOutput = @"
{
  "custom": $customJsonIndented,
  "built-in": $builtInJsonIndented
}
"@

# Write to file
Set-Content -Path $OutputPath -Value $jsonOutput -Encoding UTF8
Write-Host "`n=== Skill Index Generated Successfully! ===" -ForegroundColor Green
Write-Host "Output: $OutputPath"
Write-Host "Built-in skills: $(@($builtInSkills).Count)"
Write-Host "Custom skills: $(@($customSkills).Count)"
