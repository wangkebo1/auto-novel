param(
    [string]$SourceRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$OutputPath
)

$sourceRoot = [System.IO.Path]::GetFullPath($SourceRoot)

if (-not $OutputPath) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $OutputPath = Join-Path $sourceRoot "springAI-source-$timestamp.zip"
}

$outputPath = [System.IO.Path]::GetFullPath($OutputPath)

$excludedDirectoryNames = @(
    "node_modules",
    "dist",
    "target",
    "uploads",
    "tmp",
    "tmpclass",
    ".git",
    ".agent",
    ".claude",
    ".clinerules",
    ".codebuddy",
    ".codex",
    ".comate",
    ".cursor",
    ".gemini",
    ".kiro",
    ".lingma",
    ".qoder",
    ".qwen",
    ".roo",
    ".trae",
    ".vscode",
    ".windsurf",
    "Microsoft"
)

$excludedFileNames = @(
    "eslint-report.json"
)

function Get-NormalizedRelativePath {
    param(
        [string]$BasePath,
        [string]$FullPath
    )

    $baseFullPath = [System.IO.Path]::GetFullPath($BasePath)
    if (-not $baseFullPath.EndsWith([System.IO.Path]::DirectorySeparatorChar)) {
        $baseFullPath += [System.IO.Path]::DirectorySeparatorChar
    }

    $baseUri = New-Object System.Uri($baseFullPath)
    $fileUri = New-Object System.Uri(([System.IO.Path]::GetFullPath($FullPath)))
    return ([System.Uri]::UnescapeDataString($baseUri.MakeRelativeUri($fileUri).ToString()) -replace "\\", "/")
}

function Test-ExcludedPath {
    param(
        [string]$RelativePath
    )

    if ([string]::IsNullOrWhiteSpace($RelativePath)) {
        return $false
    }

    $segments = $RelativePath -split "[/\\]"
    foreach ($segment in $segments) {
        if ($excludedDirectoryNames -contains $segment) {
            return $true
        }
    }

    $leafName = [System.IO.Path]::GetFileName($RelativePath)
    if ($leafName -like "springAI-source-*.zip") {
        return $true
    }

    if ($excludedFileNames -contains $leafName) {
        return $true
    }

    return $false
}

$stagingRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("springAI-package-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $stagingRoot -Force | Out-Null

try {
    $files = Get-ChildItem -Path $sourceRoot -Recurse -File -Force | Where-Object {
        $relativePath = Get-NormalizedRelativePath -BasePath $sourceRoot -FullPath $_.FullName
        ($_.FullName -ne $outputPath) -and (-not (Test-ExcludedPath -RelativePath $relativePath))
    }

    foreach ($file in $files) {
        $relativePath = Get-NormalizedRelativePath -BasePath $sourceRoot -FullPath $file.FullName
        $destinationPath = Join-Path $stagingRoot $relativePath
        $destinationDirectory = Split-Path -Parent $destinationPath

        if (-not (Test-Path $destinationDirectory)) {
            New-Item -ItemType Directory -Path $destinationDirectory -Force | Out-Null
        }

        Copy-Item -Path $file.FullName -Destination $destinationPath -Force
    }

    if (Test-Path $outputPath) {
        Remove-Item -Path $outputPath -Force
    }

    Compress-Archive -Path (Join-Path $stagingRoot '*') -DestinationPath $outputPath -CompressionLevel Optimal -Force

    $archiveSizeMB = [math]::Round(((Get-Item $outputPath).Length / 1MB), 2)
    Write-Host "Created archive: $outputPath"
    Write-Host "Archive size: ${archiveSizeMB} MB"
    Write-Host "Excluded directories: $($excludedDirectoryNames -join ', ')"
}
finally {
    if (Test-Path $stagingRoot) {
        try {
            Remove-Item -Path $stagingRoot -Recurse -Force -ErrorAction Stop
        }
        catch {
            Write-Warning "Failed to remove temp directory: $stagingRoot"
        }
    }
}
