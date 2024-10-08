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
package trufflesom.interpreter;

import static trufflesom.vm.Classes.doubleClass;
import static trufflesom.vm.Classes.falseClass;
import static trufflesom.vm.Classes.integerClass;
import static trufflesom.vm.Classes.stringClass;
import static trufflesom.vm.Classes.trueClass;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.TypeSystem;

import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SClass;


@TypeSystem({boolean.class,
    long.class,
    double.class,
    SAbstractObject.class,
    Object[].class}) // Object[] is only for argument passing
public class Types {

  public static SClass getClassOf(final Object obj) {
    CompilerAsserts.neverPartOfCompilation();
    assert obj != null;

    if (obj instanceof SAbstractObject) {
      return ((SAbstractObject) obj).getSOMClass();
    } else if (obj instanceof Boolean) {
      if ((boolean) obj) {
        return trueClass;
      } else {
        return falseClass;
      }
    } else if (obj instanceof Long || obj instanceof BigInteger) {
      return integerClass;
    } else if (obj instanceof String) {
      return stringClass;
    } else if (obj instanceof Double) {
      return doubleClass;
    }

    CompilerDirectives.transferToInterpreter();
    throw new RuntimeException(
        "We got an object that should be covered by the above check: " + obj.toString());
  }
}
