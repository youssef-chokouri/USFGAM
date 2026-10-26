import java.util.*;
import java.util.zip.*;
import java.io.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

public class InspectDex {
 public static void main(String[] args) throws Exception {
  try (ZipFile z = new ZipFile(args[0])) {
   for (var e : Collections.list(z.entries())) {
    if (!e.getName().matches("classes[0-9]*\\.dex")) continue;
    var dex = new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(z.getInputStream(e).readAllBytes()));
    for(var c:dex.getClasses()) { if(args.length>1 && c.getType().equals(args[1]))for(var f:c.getFields())System.out.println("FIELD "+f); for(var m:c.getMethods()) {
     if(m.getImplementation()==null)continue;
     var ss=new HashSet<String>();
     for(var i:m.getImplementation().getInstructions()) if(i instanceof ReferenceInstruction r && r.getReference() instanceof StringReference s)ss.add(s.getString());
     boolean match=ss.contains("ClipsViewPagerImpl_getViewAtIndex") || ss.contains("feed_items") || (m.getReturnType().equals("Ljava/util/List;") && m.getParameterTypes().toString().equals("[Lcom/instagram/common/session/UserSession;, Z]")) || c.getType().equals("Linstagram/features/clips/viewer/ui/ClipsSwipeRefreshLayout;");
     if(args.length>1)match=c.getType().equals(args[1]);
     if(!match)continue;
     System.out.println("METHOD "+m+" flags="+m.getAccessFlags()+" regs="+m.getImplementation().getRegisterCount()+" STRINGS "+ss);
     int k=0;
     for(var i:m.getImplementation().getInstructions()) {
      System.out.print((k++)+" "+i.getOpcode());
      if(i instanceof OneRegisterInstruction a)System.out.print(" v"+a.getRegisterA());
      if(i instanceof TwoRegisterInstruction a)System.out.print(",v"+a.getRegisterB());
      if(i instanceof ReferenceInstruction r)System.out.print(" "+r.getReference());
      System.out.println();
     }
    }}
   }
  }
 }
}
