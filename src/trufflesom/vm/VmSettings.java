package trufflesom.vm;

import bd.settings.Settings;


public class VmSettings implements Settings {

  public static final boolean UseAstInterp;
  public static final boolean UseBcInterp;
  public static final boolean UseJitCompiler;
  public static final boolean PrintStackTraceOnDNU;

  public static final boolean UseInstrumentation;

  static {
    String val = System.getProperty("som.interp", "AST").toUpperCase();
    UseAstInterp = "AST".equals(val);
    UseBcInterp = "BC".equals(val);

    if (!UseAstInterp && !UseBcInterp) {
      throw new IllegalStateException("The Java property -Dsom.interp=" + val
          + " was set, which is not supported. Currently, only the values BC and AST are supported.");
    }

    val = System.getProperty("som.jitCompiler", "true");
    UseJitCompiler = "true".equals(val);

    val = System.getProperty("polyglot.nodestats", "false");
    String val2 = System.getProperty("polyglot.coverage", "false");
    UseInstrumentation = "true".equals(val) || "true".equals(val2);

    val = System.getProperty("som.printStackTraceOnDNU", "false");
    PrintStackTraceOnDNU = "true".equals(val);
  }

  @Override
  public boolean dynamicMetricsEnabled() {
    return false;
  }

}
