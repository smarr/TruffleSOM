package bd.settings;

import java.lang.reflect.InvocationTargetException;


/**
 * VmSettings are determined based on Java properties. They are used to configure VM-wide
 * properties, for instance whether a tool is enabled or not.
 */
public class VmSettings {
  public static final boolean DYNAMIC_METRICS;

  static {
    Settings s = getSettings();

    DYNAMIC_METRICS = s.dynamicMetricsEnabled();
  }

  private static Settings getSettings() {
    String className = System.getProperty("bd.settings");
    if (className == null) {
      return new AllDisabled();
    }

    try {
      Class<?> clazz = Class.forName(className);
      return (Settings) clazz.getDeclaredConstructor().newInstance();
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
        | SecurityException e) {
      // Checkstyle: stop
      System.err.println("[BlackDiamonds] Could not load settings class: " + className);
      e.printStackTrace();
      return new AllDisabled();
      // Checkstyle: resume
    }
  }
}
