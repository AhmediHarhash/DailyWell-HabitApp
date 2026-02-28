$langPath = "C:\Users\PC\Desktop\moneygrinder\mobile\PART_2_HEALTH_APPS\03_HABIT_BASED_HEALTH\habit-health\androidApp\src\main\assets\vits-piper-en_US-lessac-medium\espeak-ng-data\lang"
Get-ChildItem -Path $langPath -Directory | Where-Object { $_.Name -ne "gmw" } | ForEach-Object {
    Remove-Item -Path $_.FullName -Recurse -Force
    Write-Host "Deleted: $($_.Name)"
}
# Also delete loose files (eu, ko, qu)
Get-ChildItem -Path $langPath -File | ForEach-Object {
    Remove-Item -Path $_.FullName -Force
    Write-Host "Deleted file: $($_.Name)"
}
Write-Host "Cleanup complete. Only gmw folder remains."
