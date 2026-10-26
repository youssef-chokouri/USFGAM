$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force .tools | Out-Null
if (!(Test-Path .tools/morphe-desktop.jar)) {
    Invoke-WebRequest 'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.19.1/morphe-desktop-1.19.1-all.jar' -OutFile .tools/morphe-desktop.jar
}
if (!(Test-Path .tools/r8.jar)) {
    Invoke-WebRequest 'https://storage.googleapis.com/r8-releases/raw/8.3.37/r8.jar' -OutFile .tools/r8.jar
}
if (!(Test-Path '.tools/jdk21/jdk-21.0.12.1+1/bin/java.exe')) {
    Invoke-WebRequest 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1.zip' -OutFile .tools/jdk21.zip
    Expand-Archive -LiteralPath .tools/jdk21.zip -DestinationPath .tools/jdk21 -Force
}
Write-Output 'Tools ready. Run ./build.ps1'
