Write-Host "Uploading TrueBan..." -ForegroundColor Cyan

try {
    if (-not (Test-Path ".git")) {
        git init
        git branch -M main
        git remote add origin https://github.com/insanmiy/TrueBan.git
    }

    git add -A
    
    # Using "." as a minimal message to avoid the "requires a value" error
    git commit -m "."
    
    git push -u origin main --force
}
catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "Done!" -ForegroundColor Green