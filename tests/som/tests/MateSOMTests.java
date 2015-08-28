package som.tests;

import som.vm.MateUniverse;
import som.vm.Universe;

public class MateSOMTests extends SomTests {

  public MateSOMTests(String testName) {
    super(testName);
  }
  
  @Override
  protected String[] getArguments(){
    String[] args = {"-cp", "Smalltalk:Smalltalk/Mate:Smalltalk/Mate/MOP:", "TestSuite/TestHarness.som", testName};
    return args;
  }
  
  static{
    Universe.setCurrent(new MateUniverse());
    SomTests.u = Universe.current();
  }
}
