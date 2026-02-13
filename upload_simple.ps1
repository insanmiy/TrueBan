# Simple GitHub Upload Script for TrueBan
# =====================================

Write-Host "Uploading TrueBan to GitHub..." -ForegroundColor Cyan
Write-Host "Starting in 3 seconds..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

try {
    # Initialize git repository
    if (-not (Test-Path ".git")) {
        Write-Host "Initializing git repository..."
        git init
        git branch -M main
    } else {
        Write-Host "Git repository already exists"
    }

    # Add remote repository
    Write-Host "Adding remote repository..."
    git remote add origin https://github.com/insanmiy/TrueBan.git

    # Add all files
    Write-Host "Adding files..."
    git add .

    # Commit changes
    Write-Host "Committing changes..."
    git commit -m "Initial commit: TrueBan v1.0.0 - Production-ready Minecraft moderation plugin"

    # Push to GitHub
    Write-Host "Pushing to GitHub..."
    git push -u origin main --force

    Write-Host "Done! Your plugin is now at: https://github.com/insanmiy/TrueBan" -ForegroundColor Green
}
catch {
    Write-Host "Error occurred: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Check if Git is installed and you have GitHub access" -ForegroundColor Yellow
}

Write-Host "Waiting 5 seconds before closing..." -ForegroundColor Yellow
Start-Sleep -Seconds 5
Write-Host "Finished!" -ForegroundColor Green
