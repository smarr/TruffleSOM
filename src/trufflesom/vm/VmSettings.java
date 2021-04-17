package trufflesom.vm;

import bd.settings.Settings;


public class VmSettings implements Settings {

  public static final boolean UseAstInterp;
  public static final boolean UseBcInterp;
  public static final boolean UseJitCompiler;
  public static final boolean UseInterpreterOnly;
  public static final boolean PrintStackTraceOnDNU;

  static {
    String val = System.getProperty("truffle.TruffleRuntime");
    UseInterpreterOnly = "com.oracle.truffle.api.impl.DefaultTruffleRuntime".equals(val);

    val = System.getProperty("som.interp", "AST").toUpperCase();
    UseAstInterp = "AST".equals(val);
    UseBcInterp = "BC".equals(val);

    if (!UseAstInterp && !UseBcInterp) {
      throw new IllegalStateException("The Java property -Dsom.interp=" + val
          + " was set, which is not supported. Currently, only the values BC and AST are supported.");
    }

    String useJit = System.getProperty("som.jitCompiler", "unset");
    UseJitCompiler = "true".equals(useJit) || "unset".equals(useJit);

    if ("true".equals(useJit) && UseInterpreterOnly) {
      throw new IllegalStateException(
          "TruffleSOM was asked to use the default truffle runtime, "
              + "but also to use a JIT compiler. The default truffle runtime does not support JIT compilation.");
    }

    val = System.getProperty("som.printStackTraceOnDNU", "false");
    PrintStackTraceOnDNU = "true".equals(val);
  }

  @Override
  public boolean dynamicMetricsEnabled() {
    return false;
  }

}
