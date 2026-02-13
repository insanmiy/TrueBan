Write-Host "Starting upload to GitHub..."

Write-Host "Step 1: Initialize git"
git init
git branch -M main

Write-Host "Step 2: Add remote"
git remote add origin https://github.com/insanmiy/TrueBan.git

Write-Host "Step 3: Add files"
git add .

Write-Host "Step 4: Commit"
git commit -m "TrueBan v1.0.0"

Write-Host "Step 5: Push to GitHub"
git push -u origin main

Write-Host "Upload complete!"
Write-Host "Check: https://github.com/insanmiy/TrueBan"

Read-Host "Press Enter to exit"
