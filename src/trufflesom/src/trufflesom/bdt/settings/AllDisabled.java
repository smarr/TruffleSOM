package trufflesom.bdt.settings;

public class AllDisabled implements Settings {

  @Override
  public boolean dynamicMetricsEnabled() {
    return false;
  }
}
