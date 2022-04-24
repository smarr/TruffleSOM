package bdt.settings;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import bdt.settings.VmSettings;


public class VmSettingsTests {

  @Test
  public void testDefaultValuesOfSettings() {
    assertFalse(VmSettings.DYNAMIC_METRICS);
  }
}
