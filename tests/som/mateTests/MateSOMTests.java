package som.mateTests;

import som.tests.SomTests;
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
    if (!(Universe.getCurrent() instanceof MateUniverse)){
      Universe.setCurrent(new MateUniverse());
    }
    SomTests.u = Universe.current();
  }
}
