# Instagram Chat and Stories for Morphe

Experimental, standalone Morphe patch for **Instagram 452.1.0.53.64**, version code
**385811006**, **arm64-v8a**. This is the APKMirror `480–640dpi` APKM supplied for
the Galaxy S25 Ultra. Other versions deliberately fail compatibility checks.

## Behavior

- Keep Home for the Stories tray and Direct for messages.
- Remove Reels, Explore, Profile, Create, and Notifications from bottom navigation.
- Clear both home feed post lists after parsing, leaving Stories requests intact.
- Disable user paging in the Reel viewer and its pull-to-refresh gesture, so a Reel
  opened from a message can play without swiping to another Reel.
- Block the topical Explore recommendation endpoints.
- Use Home as the fallback startup tab instead of a removed tab.

The Home icon remains the native Instagram Home icon. The patch does not add a new
Stories screen. Individual profile screens, account controls and media opened from
messages are still native Instagram screens. This is a focus patch, not a security
boundary against every deep link, cached item or future server-side UI change.

## Files and use

`dist/chat-stories-0.1.0.mpp` is the patch bundle. Load it into Morphe, select the
original supplied APKM, and enable **Chat and Stories only**. The bundle requires
Morphe's **patcher 2.0.1** API; an older Manager using patcher 1.x cannot load it.
Use the patch on the original app bundle, without stacking Piko's navigation,
Reel-scrolling or feed patches on top of it.

`dist/instagram-chat-stories-452.1.0.53.64.apk` is the locally patched, merged and
signed APK, provided for phone testing. Android will normally require uninstalling
the Play Store version before installing an APK signed with a different key.
Save local drafts before uninstalling. No app was installed on a phone by this task.

## Validation and limits

The patch was applied locally using the official Morphe Desktop 1.19.1 release
(patcher 2.0.1). See `dist/patch-result.json` and `dist/validation.txt`.
Runtime helper tests check tab ordering and identity, rejection of missing native
tabs, Explore blocking, and preservation of Stories, DM and shared-media requests.
The final APK is checked for a valid signature, unique patched class definitions,
and the expected bytecode hooks.

**No Android device or logged-in session was available.** Stories loading, sending
and receiving messages, opening a shared Reel, blocking vertical Reel swipes,
startup, back navigation and notification links still need an S25 Ultra test.
Instagram server-side experiments or alternate feed paths can affect behavior.
Compatibility with the Android Manager itself also needs a device test; the bundle
includes both JVM classes and Android DEX for that purpose.

## Build from source

Run `./setup-tools.ps1` once, then `./build.ps1`. Downloads are workspace-local:
Temurin Java 21, official Morphe Desktop 1.19.1 and Google's D8 8.3.37.
There are no GitHub Packages credentials or Gradle plugin dependencies.

Example application with the local desktop tool (PowerShell):

```powershell
& '.tools/jdk21/jdk-21.0.12.1+1/bin/java.exe' -Xmx3g -jar .tools/morphe-desktop.jar patch `
  build/instagram-original.apkm -p dist/chat-stories-0.1.0.mpp `
  --exclusive -e 'Chat and Stories only' --validate `
  -o dist/instagram-chat-stories-452.1.0.53.64.apk -r dist/patch-result.json
```

Copy the original APKM into `build/instagram-original.apkm` first, so the merger's
temporary files remain in the workspace. The `tools/` directory contains the DEX
inspection utility and verification tests. Private signing keys and proprietary
APK files are excluded from Git. Keep your signing key if you want to install
future local builds as updates.

GPL-3.0; see LICENSE and NOTICE. The public Piko sources informed the native hook
locations, but the standalone patch has no runtime dependency on Piko.
