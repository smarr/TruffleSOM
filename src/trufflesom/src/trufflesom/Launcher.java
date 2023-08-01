package trufflesom;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Value;

import trufflesom.interpreter.SomLanguage;
import trufflesom.vm.VmSettings;


public class Launcher {
  public static void main(final String[] arguments) {
    Value returnCode = eval(arguments);
    if (returnCode.isNumber()) {
      System.exit(returnCode.asInt());
    } else {
      System.exit(0);
    }
  }

  public static Value eval(final String[] arguments) {
    Builder builder = createContextBuilder();
    builder.arguments(SomLanguage.LANG_ID, arguments);
    builder.logHandler(System.err);

    if (!VmSettings.UseJitCompiler) {
      builder.option("engine.Compilation", "false");
    }

    Context context = builder.build();

    Value returnCode = context.eval(SomLanguage.START);
    return returnCode;
  }

  public static Builder createContextBuilder() {
    Builder builder = Context.newBuilder(SomLanguage.LANG_ID)
                             .in(System.in)
                             .out(System.out)
                             .allowAllAccess(true);
    return builder;
  }
}
