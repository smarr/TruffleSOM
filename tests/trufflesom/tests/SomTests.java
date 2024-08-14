/*
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

import java.lang.reflect.Field;
import java.util.Arrays;

import org.graalvm.polyglot.Value;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import trufflesom.Launcher;
import trufflesom.vm.Classes;
import trufflesom.vmobjects.SObject;


@RunWith(Parameterized.class)
public class SomTests {

  @Parameters(name = "{0} [{index}]")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"Array"},
        {"Block"},
        {"Boolean"},
        {"ClassLoading"},
        {"ClassStructure"},

        {"Closure"},
        {"Coercion"},
        {"CompilerReturn"},
        {"Dictionary"},
        {"DoesNotUnderstand"},
        {"Double"},

        {"Empty"},
        {"Global"},
        {"Hash"},
        {"Integer"},

        {"Preliminary"},
        {"Reflection"},
        {"SelfBlock"},
        {"SpecialSelectors"},
        {"Super"},

        {"Set"},
        {"String"},
        {"Symbol"},
        {"System"},
        {"Vector"}
    });
  }

  private String testName;

  public SomTests(final String testName) {
    this.testName = testName;
  }

  public static Object readValue(final Value val) {
    Field f;
    try {
      f = val.getClass().getSuperclass().getDeclaredField("receiver");
    } catch (NoSuchFieldException | SecurityException e) {
      throw new RuntimeException(e);
    }
    f.setAccessible(true);
    try {
      return f.get(val);
    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testSomeTest() {
    Classes.reset();

    Value returnCode = Launcher.eval(
        new String[] {"-cp", "Smalltalk", "TestSuite/TestHarness.som", testName});
    if (returnCode.isNumber()) {
      assertEquals(0, returnCode.asInt());
    } else {
      SObject obj = (SObject) readValue(returnCode);
      assertEquals("System", obj.getSOMClass().getName().getString());
    }
  }
}
