import java.util.*;
import java.util.zip.*;
import com.android.apksig.ApkVerifier;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;

public class VerifyApk {
    public static void main(String[] args) throws Exception {
        var signature = new ApkVerifier.Builder(new java.io.File(args[0])).setMinCheckedPlatformVersion(28).build().verify();
        if (!signature.isVerified()) throw new AssertionError("APK signature: " + signature.getErrors());
        Map<String,ClassDef> patched = new HashMap<>();
        Set<String> wanted = Set.of("LX/02pg;", "LX/004E;", "LX/04QD;",
                "Linstagram/features/clips/viewer/ui/ClipsSwipeRefreshLayout;",
                "Lcom/instagram/api/tigon/TigonServiceLayer;", "Lusfgam/runtime/ChatStories;");
        try (var zip = new ZipFile(args[0])) {
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                var dex = new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(zip.getInputStream(entry).readAllBytes()));
                for (var c : dex.getClasses()) if (wanted.contains(c.getType())) {
                    if (patched.put(c.getType(), c) != null) throw new AssertionError("Duplicate class: " + c.getType());
                }
            }
        }
        if (!patched.keySet().equals(wanted)) throw new AssertionError("Missing modified classes");
        requireCalls(patched.get("LX/02pg;"), "unsafeParseFromJson", "Ljava/util/List;->clear()V", 2);
        requireCalls(patched.get("LX/004E;"), "A04", "Lusfgam/runtime/ChatStories;->tabs(Ljava/util/List;)Ljava/util/List;", 1);
        requireCalls(patched.get("LX/04QD;"), "E1w", "Landroidx/viewpager2/widget/ViewPager2;->setUserInputEnabled(Z)V", 1);
        requireCalls(patched.get("Lcom/instagram/api/tigon/TigonServiceLayer;"), "startRequestInternal", "Lusfgam/runtime/ChatStories;->checkRequest(Ljava/net/URI;)V", 1);
        for (var m : patched.get("LX/004E;").getMethods()) if (m.getName().equals("A02")) {
            var it=m.getImplementation().getInstructions().iterator();
            var first=it.next();
            if (!(first instanceof ReferenceInstruction r) || !r.getReference().toString().equals("LX/08kM;->A0C:LX/08kM;")
                    || it.next().getOpcode()!=Opcode.RETURN_OBJECT) throw new AssertionError("Startup fallback");
        }
        for(var m:patched.get("Linstagram/features/clips/viewer/ui/ClipsSwipeRefreshLayout;").getMethods())
            if(m.getName().equals("onInterceptTouchEvent")) {
                var it=m.getImplementation().getInstructions().iterator();
                if(it.next().getOpcode()!=Opcode.CONST_4 || it.next().getOpcode()!=Opcode.RETURN)
                    throw new AssertionError("Reel pull-to-refresh not disabled");
            }
        System.out.println("PASS: APK signature, six unique patched/runtime classes, feed clears, navigation, startup, Reel gesture hooks, Explore hook");
    }
    static void requireCalls(ClassDef c, String method, String ref, int expected) {
        int count=0;
        for(var m:c.getMethods())if(m.getName().equals(method))for(var i:m.getImplementation().getInstructions())
            if(i instanceof ReferenceInstruction r && r.getReference().toString().equals(ref))count++;
        if(count!=expected)throw new AssertionError(method+": "+ref+" count="+count);
    }
}
