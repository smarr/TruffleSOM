/**
 * Copyright (c) 2013 Stefan Marr, stefan.marr@vub.ac.be
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package trufflesom.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static trufflesom.tests.SomTests.readValue;

import java.util.Arrays;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Value;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import trufflesom.Launcher;
import trufflesom.compiler.SourcecodeCompiler.AstCompiler;
import trufflesom.compiler.SourcecodeCompiler.BcCompiler;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.objectstorage.StorageAnalyzer;
import trufflesom.vm.Classes;
import trufflesom.vm.Universe;
import trufflesom.vm.VmSettings;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SSymbol;


@RunWith(Parameterized.class)
public class BasicInterpreterTests {

  @Parameters(name = "{0}.{1} [{index}]")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][] {
        // TODO: add support for these two
        // {"Self", "testAssignSuper", 42, ParseError.class},
        // {"Self", "testAssignSelf", 42, ParseError.class},

        {"MethodCall", "test", 42, Long.class},
        {"MethodCall", "test2", 42, Long.class},

        {"NonLocalReturn", "test1", 42, Long.class},
        {"NonLocalReturn", "test2", 43, Long.class},
        {"NonLocalReturn", "test3", 3, Long.class},
        {"NonLocalReturn", "test4", 42, Long.class},
        {"NonLocalReturn", "test5", 22, Long.class},

        {"Blocks", "testArg1", 42, Long.class},
        {"Blocks", "testArg2", 77, Long.class},
        {"Blocks", "testArgAndLocal", 8, Long.class},
        {"Blocks", "testArgAndContext", 8, Long.class},
        {"Blocks", "testEmptyZeroArg", 1, Long.class},
        {"Blocks", "testEmptyOneArg", 1, Long.class},
        {"Blocks", "testEmptyTwoArg", 1, Long.class},

        {"Return", "testReturnSelf", "Return", SClass.class},
        {"Return", "testReturnSelfImplicitly", "Return", SClass.class},
        {"Return", "testNoReturnReturnsSelf", "Return", SClass.class},
        {"Return", "testBlockReturnsImplicitlyLastValue", 4, Long.class},

        {"IfTrueIfFalse", "test", 42, Long.class},
        {"IfTrueIfFalse", "test2", 33, Long.class},
        {"IfTrueIfFalse", "test3", 4, Long.class},

        {"IfTrueIfFalse", "testIfTrueTrueResult", "Integer", SClass.class},
        {"IfTrueIfFalse", "testIfTrueFalseResult", "Nil", SClass.class},
        {"IfTrueIfFalse", "testIfFalseTrueResult", "Nil", SClass.class},
        {"IfTrueIfFalse", "testIfFalseFalseResult", "Integer", SClass.class},

        {"CompilerSimplification", "testReturnConstantSymbol", "constant", SSymbol.class},
        {"CompilerSimplification", "testReturnConstantInt", 42, Long.class},
        {"CompilerSimplification", "testReturnSelf", "CompilerSimplification", SClass.class},
        {"CompilerSimplification", "testReturnSelfImplicitly", "CompilerSimplification",
            SClass.class},
        {"CompilerSimplification", "testReturnArgumentN", 55, Long.class},
        {"CompilerSimplification", "testReturnArgumentA", 44, Long.class},
        {"CompilerSimplification", "testSetField", "foo", SSymbol.class},
        {"CompilerSimplification", "testGetField", 40, Long.class},

        {"Hash", "testHash", 444, Long.class},

        {"Arrays", "testEmptyToInts", 3, Long.class},
        {"Arrays", "testPutAllInt", 5, Long.class},
        {"Arrays", "testPutAllNil", "Nil", SClass.class},
        {"Arrays", "testPutAllBlock", 3, Long.class},
        {"Arrays", "testNewWithAll", 1, Long.class},

        {"BlockInlining", "testNoInlining", 1, Long.class},
        {"BlockInlining", "testOneLevelInlining", 1, Long.class},
        {"BlockInlining", "testOneLevelInliningWithLocalShadowTrue", 2, Long.class},
        {"BlockInlining", "testOneLevelInliningWithLocalShadowFalse", 1, Long.class},

        {"BlockInlining", "testShadowDoesntStoreWrongLocal", 33, Long.class},
        {"BlockInlining", "testShadowDoesntReadUnrelated", "Nil", SClass.class},

        {"BlockInlining", "testBlockNestedInIfTrue", 2, Long.class},
        {"BlockInlining", "testBlockNestedInIfFalse", 42, Long.class},

        {"BlockInlining", "testStackDisciplineTrue", 1, Long.class},
        {"BlockInlining", "testStackDisciplineFalse", 2, Long.class},

        {"BlockInlining", "testDeepNestedInlinedIfTrue", 3, Long.class},
        {"BlockInlining", "testDeepNestedInlinedIfFalse", 42, Long.class},

        {"BlockInlining", "testDeepNestedBlocksInInlinedIfTrue", 5, Long.class},
        {"BlockInlining", "testDeepNestedBlocksInInlinedIfFalse", 43, Long.class},

        {"BlockInlining", "testDeepDeepNestedTrue", 9, Long.class},
        {"BlockInlining", "testDeepDeepNestedFalse", 43, Long.class},

        {"BlockInlining", "testToDoNestDoNestIfTrue", 2, Long.class},

        {"NonLocalVars", "testWriteDifferentTypes", 3.75, Double.class},

        {"ObjectCreation", "test", 1000000, Long.class},

        {"Regressions", "testSymbolEquality", 1, Long.class},
        {"Regressions", "testSymbolReferenceEquality", 1, Long.class},
        {"Regressions", "testUninitializedLocal", 1, Long.class},
        {"Regressions", "testUninitializedLocalInBlock", 1, Long.class},

        {"BinaryOperation", "test", 3 + 8, Long.class},

        {"NumberOfTests", "numberOfTests", 65, Long.class}
    });
  }

  private final String   testClass;
  private final String   testSelector;
  private final Object   expectedResult;
  private final Class<?> resultType;

  public BasicInterpreterTests(final String testClass,
      final String testSelector,
      final Object expectedResult,
      final Class<?> resultType) {
    this.testClass = testClass;
    this.testSelector = testSelector;
    this.expectedResult = expectedResult;
    this.resultType = resultType;
  }

  protected void assertEqualsSOMValue(final Object expectedResult, final Object actualResult) {
    if (resultType == Long.class) {
      if (actualResult instanceof Long) {
        long expected = (int) expectedResult;
        long actual = (long) actualResult;
        assertEquals(expected, actual);
      } else {
        fail("Expected integer result, but got: " + actualResult.toString());
      }
      return;
    }

    if (resultType == Double.class) {
      double expected = (double) expectedResult;
      double actual = (double) actualResult;
      assertEquals(expected, actual, 1e-15);
      return;
    }

    if (resultType == SClass.class) {
      String expected = (String) expectedResult;
      String actual = ((SClass) readValue((Value) actualResult)).getName().getString();
      assertEquals(expected, actual);
      return;
    }

    if (resultType == SSymbol.class) {
      String expected = (String) expectedResult;
      String actual = ((SSymbol) readValue((Value) actualResult)).getString();
      assertEquals(expected, actual);
      return;
    }
    fail("SOM Value handler missing");
  }

  @Test
  public void testBasicInterpreterBehavior() {
    StorageAnalyzer.initAccessors();
    Classes.reset();

    if (VmSettings.UseAstInterp) {
      Universe.setSourceCompiler(new AstCompiler(), true);
    } else {
      Universe.setSourceCompiler(new BcCompiler(), true);
    }

    Builder builder = Launcher.createContextBuilder();
    builder.option("som.CLASS_PATH", "Smalltalk:TestSuite/BasicInterpreterTests");
    builder.option("som.TEST_CLASS", testClass);
    builder.option("som.TEST_SELECTOR", testSelector);

    Context context = builder.build();
    Value actualResult = context.eval(SomLanguage.START);

    assertEqualsSOMValue(expectedResult, actualResult.as(Object.class));
  }
}
