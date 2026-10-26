param([string]$JavaHome = "$PSScriptRoot/.tools/jdk21/jdk-21.0.12.1+1")
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
function Invoke-Checked([string]$Program, [string[]]$Arguments) {
    & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Program failed ($LASTEXITCODE)" }
}
$javac = Join-Path $JavaHome 'bin/javac.exe'
$java = Join-Path $JavaHome 'bin/java.exe'
$jar = Join-Path $JavaHome 'bin/jar.exe'
New-Item -ItemType Directory -Force build/patch-classes,build/runtime-classes,build/runtime-dex,build/patch-dex,build/bundle/extensions,dist | Out-Null
Invoke-Checked $javac @('--release','17','-proc:none','-cp','.tools/morphe-desktop.jar','-d','build/patch-classes','src/main/java/usfgam/ChatStoriesPatch.java')
Invoke-Checked $javac @('--release','8','-Xlint:-options','-d','build/runtime-classes','src/runtime/java/usfgam/runtime/ChatStories.java')
Invoke-Checked $jar @('cf','build/runtime.jar','-C','build/runtime-classes','.')
Invoke-Checked $java @('-cp','.tools/r8.jar','com.android.tools.r8.D8','--min-api','28','--output','build/runtime-dex','build/runtime.jar')
Copy-Item build/runtime-dex/classes.dex build/bundle/extensions/chat-stories.dex
Invoke-Checked $jar @('cf','build/patch-jvm.jar','-C','build/patch-classes','.')
Invoke-Checked $java @('-cp','.tools/r8.jar','com.android.tools.r8.D8','--min-api','28','--classpath','.tools/morphe-desktop.jar','--output','build/patch-dex','build/patch-jvm.jar')
Copy-Item build/patch-dex/classes.dex build/bundle/classes.dex
@'
Manifest-Version: 1.0
Name: USFGAM Chat and Stories
Description: Instagram chat and Stories focus patch
Version: 0.1.0
Author: USFGAM
License: GNU General Public License v3.0
Patcher-Version: 2.0.1

'@ | Set-Content build/MANIFEST.MF -Encoding ascii
Invoke-Checked $jar @('cfm','dist/chat-stories-0.1.0.mpp','build/MANIFEST.MF','-C','build/patch-classes','.','-C','build/bundle','.','LICENSE','NOTICE')
Write-Output 'Built dist/chat-stories-0.1.0.mpp'
