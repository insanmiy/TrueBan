# TrueBan - Upload to GitHub PowerShell Script
# =============================================

Write-Host "TrueBan - Upload to GitHub" -ForegroundColor Cyan
Write-Host "============================" -ForegroundColor Cyan
Write-Host ""

# Function to write colored messages
function Write-Status {
    param([string]$Message, [string]$Color = "White")
    Write-Host $Message -ForegroundColor $Color
}

# Function to handle errors gracefully
function Invoke-WithErrorHandling {
    param(
        [string]$Operation,
        [scriptblock]$ScriptBlock,
        [string]$ContinueMessage = "Continuing anyway..."
    )
    
    try {
        $result = & $ScriptBlock
        if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne $null) {
            throw "Command failed with exit code $LASTEXITCODE"
        }
        return $result
    }
    catch {
        Write-Status "WARNING: $Operation failed" "Yellow"
        Write-Status "Error: $($_.Exception.Message)" "Yellow"
        Write-Status $ContinueMessage "Yellow"
        return $null
    }
}

# Check if git is installed
Write-Status "Checking if Git is installed..."
$gitVersion = & git --version 2>$null
if (-not $gitVersion) {
    Write-Status "WARNING: Git is not installed or not in PATH" "Yellow"
    Write-Status "Please install Git from https://git-scm.com/" "Yellow"
    Write-Host ""
    $continue = Read-Host "Press Enter to continue anyway (may cause errors) or Ctrl+C to exit"
}
else {
    Write-Status "Git found: $gitVersion" "Green"
}

# Initialize git repository if not already done
if (-not (Test-Path ".git")) {
    Write-Status "Initializing Git repository..."
    Invoke-WithErrorHandling -Operation "Git initialization" -ScriptBlock {
        & git init
        & git branch -M main
    }
}
else {
    Write-Status "Git repository already exists" "Green"
}

# Add remote repository if not already added
$remotes = & git remote -v 2>$null
if ($remotes -notmatch "origin") {
    Write-Status "Adding remote repository..."
    Invoke-WithErrorHandling -Operation "Adding remote" -ScriptBlock {
        & git remote add origin https://github.com/insanmiy/TrueBan.git
    }
}
else {
    Write-Status "Remote repository already exists" "Green"
}

# Create .gitignore file
Write-Status "Creating .gitignore..."
$gitignoreContent = @"
# Compiled class file
*.class

# Log file
*.log

# BlueJ files
*.ctxt

# Mobile Tools for Java (J2ME)
.mtj.tmp/

# Package Files #
*.jar
*.war
*.nar
*.ear
*.zip
*.tar.gz
*.rar

# virtual machine crash logs, see http://www.java.com/en/download/help/error_hotspot.xml
hs_err_pid*

# Maven
target/
.mvn/
mvnw
mvnw.cmd

# Gradle
.gradle/
build/
!gradle-wrapper.jar
!gradle-wrapper.properties
gradlew
gradlew.bat

# IDE
.idea/
*.iws
*.iml
*.ipr
.vscode/

# OS
.DS_Store
Thumbs.db

# Eclipse
.project
.classpath
.factorypath
.settings/
.springBeans
.st4j/
.metadata/
bin/
tmp/
temp/
.recommenders/

# NetBeans
nbproject/private/
build/
nbbuild/
dist/
nbdist/
.nb-gradle/

# VS Code
.vscode/
.factorypath
.classpath
.project
.settings/
.springBeans
.st4j/
.metadata/
bin/
tmp/
temp/
.recommenders/
"@

Set-Content -Path ".gitignore" -Value $gitignoreContent -Force
Write-Status ".gitignore created successfully" "Green"

# Add all files to Git
Write-Status "Adding all files to Git..."
Invoke-WithErrorHandling -Operation "Git add" -ScriptBlock {
    & git add .
}

# Commit changes
Write-Status "Committing changes..."
$commitMessage = @"
Initial commit: TrueBan v1.0.0 - Production-ready Minecraft moderation plugin

Features:
- Multiple storage support (JSON, SQLite, MySQL)
- All punishment types (Ban, TempBan, IPBan, Mute, TempMute, Kick)
- Async database operations
- Thread-safe design
- Auto-expiration of temporary punishments
- Offline player support
- Complete command set with permissions
- Tab completion
- Configurable messages
- Production-ready code structure

Built for PaperMC 1.21.4 with Java 21
"@

Invoke-WithErrorHandling -Operation "Git commit" -ScriptBlock {
    & git commit -m $commitMessage
} -ContinueMessage "This might be normal if there are no changes to commit."

# Push to GitHub
Write-Status "Pushing to GitHub..."
$pushResult = Invoke-WithErrorHandling -Operation "Git push" -ScriptBlock {
    & git push -u origin main
}

if ($pushResult -eq $null) {
    Write-Status "Push failed. This could be due to:" "Yellow"
    Write-Status "  - Authentication issues (need to login to GitHub)" "Yellow"
    Write-Status "  - Network connectivity problems" "Yellow"
    Write-Status "  - Repository access permissions" "Yellow"
    Write-Host ""
    Write-Status "You may need to manually push later with: git push -u origin main" "Cyan"
}
else {
    Write-Host ""
    Write-Status "=================================" "Green"
    Write-Status "Upload completed successfully!" "Green"
    Write-Status "=================================" "Green"
    Write-Host ""
    Write-Status "Your TrueBan plugin is now available at:" "Cyan"
    Write-Status "https://github.com/insanmiy/TrueBan" "Cyan"
    Write-Host ""
}

Write-Host ""
Write-Status "Script finished. Check the messages above for any warnings." "White"
Write-Status "If there were warnings, you may need to resolve them manually." "White"
Write-Host ""

# Keep window open
Write-Host "Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
