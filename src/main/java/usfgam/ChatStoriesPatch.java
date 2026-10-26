package usfgam;

import java.util.*;
import kotlin.Unit;
import app.morphe.patcher.patch.*;
import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Deliberately restricted to the inspected ARM64 Instagram build. */
public final class ChatStoriesPatch {
    private static final InstructionExtensions INS = InstructionExtensions.INSTANCE;
    private static final String HELPER = "Lusfgam/runtime/ChatStories;";
    public static final BytecodePatch chatStories = create();

    private static BytecodePatch create() {
        var builder = new BytecodePatchBuilder("Chat and Stories only",
                "Keep Stories and messages, remove home posts and discovery tabs, and disable swiping between shared Reels.", true);
        builder.compatibleWith(new Compatibility("com.instagram.android", "Instagram", null,
                ApkFileType.APKM, Integer.valueOf(0xFC483C), Collections.emptySet(),
                List.of(new AppTarget("452.1.0.53.64", Map.of(SupportedAbi.ARM64_V8A, 385811006),
                        true, 28, "APK checked; phone testing required")), FormFactor.MOBILE));
        builder.extendWith(() -> {
            var stream = ChatStoriesPatch.class.getClassLoader().getResourceAsStream("extensions/chat-stories.dex");
            if (stream == null) throw new IllegalStateException("Missing chat-stories extension");
            return stream;
        });
        builder.execute(context -> {
            patchFeed(context);
            patchNavigation(context);
            patchReels(context);
            patchExplore(context);
            return Unit.INSTANCE;
        });
        return builder.build$morphe_patcher();
    }

    private static MutableMethod method(BytecodePatchContext ctx, String type, String name) {
        var matches = ctx.mutableClassDefBy(type).getMethods().stream()
                .filter(m -> m.getName().equals(name)).collect(java.util.stream.Collectors.toList());
        if (matches.size() != 1) throw new IllegalStateException("Expected unique method: " + type + "->" + name);
        return matches.get(0);
    }

    private static Set<String> strings(MutableMethod method) {
        Set<String> result = new HashSet<>();
        for (var instruction : INS.getInstructions(method)) {
            if (instruction instanceof ReferenceInstruction ref && ref.getReference() instanceof StringReference s)
                result.add(s.getString());
        }
        return result;
    }

    private static void patchFeed(BytecodePatchContext ctx) {
        var parser = method(ctx, "LX/02pg;", "unsafeParseFromJson");
        if (!strings(parser).containsAll(List.of("feed_items", "items", "pagination_source", "is_shell_response")))
            throw new IllegalStateException("Home feed parser changed");
        int changed = 0;
        // Both lists are freshly allocated ArrayLists inside this parser. Clear before storing,
        // preserving all response metadata and the independently loaded Stories tray.
        var code = INS.getInstructions(parser);
        for (int i = code.size() - 1; i >= 0; i--) {
            var instruction = code.get(i);
            if (instruction.getOpcode() != Opcode.IPUT_OBJECT || !(instruction instanceof ReferenceInstruction ref)
                    || !(ref.getReference() instanceof FieldReference field)
                    || !field.getDefiningClass().equals("LX/08hk;")
                    || !Set.of("A0U", "A0W").contains(field.getName())
                    || !field.getType().equals("Ljava/util/List;")) continue;
            int register = ((TwoRegisterInstruction) instruction).getRegisterA();
            // Replacement preserves branch labels on the original field assignment.
            INS.replaceInstruction(parser, i, "invoke-interface/range {v" + register + " .. v" + register
                    + "}, Ljava/util/List;->clear()V");
            INS.addInstruction(parser, i + 1, instruction);
            changed++;
        }
        if (changed != 2) throw new IllegalStateException("Expected both home feed lists, found " + changed);
        System.out.println("USFGAM: cleared both home feed response lists; Stories endpoints untouched");
    }

    private static void patchNavigation(BytecodePatchContext ctx) {
        var nav = method(ctx, "LX/004E;", "A04");
        if (!nav.getReturnType().equals("Ljava/util/List;")
                || !nav.getParameterTypes().toString().equals("[Lcom/instagram/common/session/UserSession;, Z]"))
            throw new IllegalStateException("Navigation signature changed");
        var code = INS.getInstructions(nav);
        var returns = new ArrayList<Integer>();
        for (int i = 0; i < code.size(); i++) if (code.get(i).getOpcode() == Opcode.RETURN_OBJECT) returns.add(i);
        if (returns.size() != 1) throw new IllegalStateException("Navigation return flow changed");
        int index = returns.get(0);
        int register = ((OneRegisterInstruction) code.get(index)).getRegisterA();
        // p1 is UserSession; resolve all native candidates so Direct is available even when
        // the account's server-side layout omits its tab. Preserve FEED first for Stories.
        INS.replaceInstruction(nav, index, "invoke-static {p1}, LX/004E;->A01(Lcom/instagram/common/session/UserSession;)Ljava/util/List;");
        INS.addInstructions(nav, index + 1, "move-result-object v" + register + "\n"
                + "invoke-static/range {v" + register + " .. v" + register + "}, " + HELPER + "->tabs(Ljava/util/List;)Ljava/util/List;\n"
                + "move-result-object v" + register + "\nreturn-object v" + register);
        // Some accounts start on CLIPS or FEED_SWITCHER. Both tabs are removed here;
        // the native fallback must resolve to the remaining Stories/Home tab.
        var startup = method(ctx, "LX/004E;", "A02");
        if (!startup.getReturnType().equals("LX/08kM;"))
            throw new IllegalStateException("Default startup tab signature changed");
        INS.addInstructions(startup, 0, "sget-object v0, LX/08kM;->A0C:LX/08kM;\nreturn-object v0");
        System.out.println("USFGAM: navigation restricted to Home (Stories) and Direct");
    }

    private static void patchReels(BytecodePatchContext ctx) {
        var pager = method(ctx, "LX/04QD;", "E1w");
        if (!strings(pager).contains("ClipsViewPagerImpl_getViewAtIndex"))
            throw new IllegalStateException("Reel pager changed");
        var fields = ctx.mutableClassDefBy("LX/04QD;").getFields().stream()
                .filter(f -> f.getType().equals("Landroidx/viewpager2/widget/ViewPager2;")).collect(java.util.stream.Collectors.toList());
        if (fields.size() != 1 || pager.getImplementation().getRegisterCount() < 4)
            throw new IllegalStateException("Reel pager field/registers changed");
        INS.addInstructions(pager, 0, "iget-object v0, p0, " + fields.get(0) + "\n"
                + "if-eqz v0, :resume\nconst/4 v1, 0x0\n"
                + "invoke-virtual {v0, v1}, Landroidx/viewpager2/widget/ViewPager2;->setUserInputEnabled(Z)V\n:resume\nnop");
        var refresh = method(ctx, "Linstagram/features/clips/viewer/ui/ClipsSwipeRefreshLayout;", "onInterceptTouchEvent");
        INS.addInstructions(refresh, 0, "const/4 v0, 0x0\nreturn v0");
        System.out.println("USFGAM: Reel paging and pull-to-refresh disabled; opening shared media retained");
    }

    private static void patchExplore(BytecodePatchContext ctx) {
        var request = method(ctx, "Lcom/instagram/api/tigon/TigonServiceLayer;", "startRequestInternal");
        var code = INS.getInstructions(request);
        int boundary = -1, uriIndex = -1, register = -1;
        for (int i = 0; i < code.size(); i++) if (code.get(i).getOpcode() == Opcode.IF_EQZ) { boundary = i; break; }
        for (int i = 0; i < boundary; i++) {
            if (code.get(i).getOpcode() == Opcode.IGET_OBJECT && code.get(i) instanceof ReferenceInstruction ref
                    && ref.getReference() instanceof FieldReference field && field.getType().equals("Ljava/net/URI;")) {
                uriIndex = i; register = ((TwoRegisterInstruction) code.get(i)).getRegisterA();
            }
        }
        if (uriIndex < 0) throw new IllegalStateException("Request URI load changed");
        INS.addInstruction(request, uriIndex + 1, "invoke-static/range {v" + register + " .. v" + register
                + "}, " + HELPER + "->checkRequest(Ljava/net/URI;)V");
        System.out.println("USFGAM: Explore recommendation requests blocked");
    }
}
