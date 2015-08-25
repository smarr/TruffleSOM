package som.tests;

import som.vm.MateUniverse;
import som.vm.Universe;

public class MateSOMTests extends SomTests {

  public MateSOMTests(String testName) {
    super(testName);
  }
  
  protected Universe getUniverse(){
    if (u == null) {
      u = MateUniverse.current();
    }
    return u;
  }
  
  protected String[] getArguments(){
    String[] args = {"-cp", "Smalltalk:Smalltalk/Mate/MOP:", "TestSuite/TestHarness.som", testName};
    return args;
  }
}
