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

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.api.vm.PolyglotEngine.Builder;
import com.oracle.truffle.api.vm.PolyglotEngine.Value;

import trufflesom.interpreter.SomLanguage;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SSymbol;


@RunWith(Parameterized.class)
public class BasicInterpreterTests {

  @Parameters
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"MethodCall", "test", 42, Long.class},
        {"MethodCall", "test2", 42, Long.class},

        {"NonLocalReturn", "test", "NonLocalReturn", SClass.class},
        {"NonLocalReturn", "test1", 42, Long.class},
        {"NonLocalReturn", "test2", 43, Long.class},
        {"NonLocalReturn", "test3", 3, Long.class},
        {"NonLocalReturn", "test4", 42, Long.class},
        {"NonLocalReturn", "test5", 22, Long.class},

        {"Blocks", "arg1", 42, Long.class},
        {"Blocks", "arg2", 77, Long.class},
        {"Blocks", "argAndLocal", 8, Long.class},
        {"Blocks", "argAndContext", 8, Long.class},

        {"Return", "returnSelf", "Return", SClass.class},
        {"Return", "returnSelfImplicitly", "Return", SClass.class},
        {"Return", "noReturnReturnsSelf", "Return", SClass.class},
        {"Return", "blockReturnsImplicitlyLastValue", 4, Long.class},

        {"IfTrueIfFalse", "test", 42, Long.class},
        {"IfTrueIfFalse", "test2", 33, Long.class},
        {"IfTrueIfFalse", "test3", 4, Long.class},

        {"CompilerSimplification", "returnConstantSymbol", "constant", SSymbol.class},
        {"CompilerSimplification", "returnConstantInt", 42, Long.class},
        {"CompilerSimplification", "returnSelf", "CompilerSimplification", SClass.class},
        {"CompilerSimplification", "returnSelfImplicitly", "CompilerSimplification",
            SClass.class},
        {"CompilerSimplification", "testReturnArgumentN", 55, Long.class},
        {"CompilerSimplification", "testReturnArgumentA", 44, Long.class},
        {"CompilerSimplification", "testSetField", "foo", SSymbol.class},
        {"CompilerSimplification", "testGetField", 40, Long.class},

        {"Arrays", "testEmptyToInts", 3, Long.class},
        {"Arrays", "testPutAllInt", 5, Long.class},
        {"Arrays", "testPutAllNil", "Nil", SClass.class},
        {"Arrays", "testNewWithAll", 1, Long.class},

        {"BlockInlining", "testNoInlining", 1, Long.class},
        {"BlockInlining", "testOneLevelInlining", 1, Long.class},
        {"BlockInlining", "testOneLevelInliningWithLocalShadowTrue", 2, Long.class},
        {"BlockInlining", "testOneLevelInliningWithLocalShadowFalse", 1, Long.class},

        {"BlockInlining", "testBlockNestedInIfTrue", 2, Long.class},
        {"BlockInlining", "testBlockNestedInIfFalse", 42, Long.class},

        {"BlockInlining", "testDeepNestedInlinedIfTrue", 3, Long.class},
        {"BlockInlining", "testDeepNestedInlinedIfFalse", 42, Long.class},

        {"BlockInlining", "testDeepNestedBlocksInInlinedIfTrue", 5, Long.class},
        {"BlockInlining", "testDeepNestedBlocksInInlinedIfFalse", 43, Long.class},

        {"BlockInlining", "testDeepDeepNestedTrue", 9, Long.class},
        {"BlockInlining", "testDeepDeepNestedFalse", 43, Long.class},

        {"BlockInlining", "testToDoNestDoNestIfTrue", 2, Long.class},

        {"NonLocalVars", "writeDifferentTypes", 3.75, Double.class},

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
      long expected = (int) expectedResult;
      long actual = (long) actualResult;
      assertEquals(expected, actual);
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
      String actual = ((SClass) actualResult).getName().getString();
      assertEquals(expected, actual);
      return;
    }

    if (resultType == SSymbol.class) {
      String expected = (String) expectedResult;
      String actual = ((SSymbol) actualResult).getString();
      assertEquals(expected, actual);
      return;
    }
    fail("SOM Value handler missing");
  }

  @Test
  public void testBasicInterpreterBehavior() {
    Builder builder = PolyglotEngine.newBuilder();
    builder.config(SomLanguage.MIME_TYPE, SomLanguage.CLASS_PATH,
        "Smalltalk:TestSuite/BasicInterpreterTests");
    builder.config(SomLanguage.MIME_TYPE, SomLanguage.TEST_CLASS, testClass);
    builder.config(SomLanguage.MIME_TYPE, SomLanguage.TEST_SELECTOR, testSelector);

    PolyglotEngine engine = builder.build();
    Value actualResult = engine.eval(SomLanguage.START);

    assertEqualsSOMValue(expectedResult, actualResult.as(Object.class));
  }
}
