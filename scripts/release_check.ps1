$ErrorActionPreference = "Stop"

Write-Host "=================================================="
Write-Host "Offline Transcriber v1.0 Production Release Checks"
Write-Host "=================================================="

Write-Host "`n[1/4] Running Unit Tests..."
cmd /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& gradlew.bat testDebugUnitTest"

Write-Host "`n[2/4] Building Debug APK..."
cmd /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& gradlew.bat assembleDebug"

Write-Host "`n[3/4] Building Release APK (R8 Minification & Resource Shrinking)..."
cmd /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& gradlew.bat assembleRelease"

Write-Host "`n[4/4] Building Production Android App Bundle (.aab)..."
cmd /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& gradlew.bat bundleRelease"

$bundle = "app\build\outputs\bundle\release\app-release.aab"

if (!(Test-Path $bundle)) {
    throw "Release AAB not found at expected path: $bundle"
}

$bundleSize = (Get-Item $bundle).Length
if ($bundleSize -le 0) {
    throw "Release AAB is empty."
}

Write-Host "`n=================================================="
Write-Host "ALL RELEASE CHECKS PASSED!"
Write-Host "Production AAB: $bundle ($([math]::Round($bundleSize / 1MB, 2)) MB)"
Write-Host "Ready for Google Play Console upload."
Write-Host "=================================================="
