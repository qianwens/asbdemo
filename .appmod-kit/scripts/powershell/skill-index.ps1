#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Generate skill index from markdown files with YAML front matter.

.DESCRIPTION
    Scans .appmod-kit/skills folder for .md files, extracts YAML front matter
    (name and description), and generates skill-index.json in .github/modernization folder.

.EXAMPLE
    .\skill-index.ps1
#>

param(
    [string]$SkillsPath = ".appmod-kit/skills",
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

# Check if skills directory exists
if (-not (Test-Path -Path $SkillsPath)) {
    Write-Warning "Skills directory not found: $SkillsPath"
    Write-Host "Creating empty skill index..."
    $skills = @()
}
else {
    # Get all .md files from skills folder
    $markdownFiles = @(Get-ChildItem -Path $SkillsPath -Filter "*.md" -File)
    
    if ($markdownFiles.Count -eq 0) {
        Write-Warning "No markdown files found"
        $skills = @()
    }
    else {
        # Extract skills from each file
        $skills = @()
        foreach ($file in $markdownFiles) {
            Write-Host "Processing: $($file.Name)"
            
            $relativePath = "$SkillsPath/$($file.Name)".Replace("\\", "/")
            $skill = Extract-YamlFrontMatter -FilePath $file.FullName -RelativePath $relativePath
            
            if ($skill) {
                $skills += $skill
                Write-Host "Extracted: $($skill.name)" -ForegroundColor Green
            }
            else {
                Write-Warning "No valid YAML front matter found in: $($file.Name)"
            }
        }
    }
}

# Ensure output directory exists
$outputDir = Split-Path -Path $OutputPath -Parent
if ($outputDir -and -not (Test-Path -Path $outputDir)) {
    Write-Host "Creating output directory: $outputDir"
    New-Item -Path $outputDir -ItemType Directory -Force | Out-Null
}

# Generate JSON output
$jsonOutput = $skills | ConvertTo-Json -Depth 10
if ($skills.Count -eq 1) {
    # ConvertTo-Json doesn't wrap single items in array, so wrap it manually
    $jsonOutput = "[$jsonOutput]"
}

# Write to file
Set-Content -Path $OutputPath -Value $jsonOutput -Encoding UTF8
Write-Host "`nSkill index generated successfully!" -ForegroundColor Green
Write-Host "Output: $OutputPath"
Write-Host "Total skills: $($skills.Count)"
