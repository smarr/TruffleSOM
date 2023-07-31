package trufflesom.tests;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vm.Universe;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SInvokable.SMethod;


public class SplittingTests extends AstTestSetup {

  @BeforeClass
  public static void init() {
    Universe.setupClassPath("Smalltalk");

    reinitTruffleAndEnterContext();
    Universe.initializeObjectSystem();
  }

  @AfterClass
  public static void close() {
    closeContext();
  }

  @Test
  public void testCorrectFrameDescriptorUpdatesInBlocks() {

    ExpressionNode body = parseMethod("""
        methodToBeSplit = (
          | local |
          [ local := #sym ] value.
          ^ local
        )
        """);

    SMethod method = assembleLastMethod(body);
    Invokable mOrg = method.getInvokable();
    Object sym = mOrg.getCallTarget().call(Nil.nilObject);

    Invokable splitM = (Invokable) mOrg.deepCopy();
    Object sym2 = splitM.getCallTarget().call(Nil.nilObject);

    assertNotSame("Expect the split method to return #sym, and not nil", Nil.nilObject, sym2);
    assertSame(sym, sym2);
  }
}
