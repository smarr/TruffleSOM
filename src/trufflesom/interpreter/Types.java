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
package trufflesom.interpreter;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.TypeSystem;

import trufflesom.vm.Universe;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


@TypeSystem({boolean.class,
    long.class,
    BigInteger.class,
    String.class,
    double.class,
    SClass.class,
    SObject.class,
    SBlock.class,
    SSymbol.class,
    SInvokable.class,
    SArray.class,
    SAbstractObject.class,
    Object[].class}) // Object[] is only for argument passing
public class Types {

  public static SClass getClassOf(final Object obj, final Universe universe) {
    CompilerAsserts.neverPartOfCompilation();
    assert obj != null;

    if (obj instanceof SAbstractObject) {
      return ((SAbstractObject) obj).getSOMClass(universe);
    } else if (obj instanceof Boolean) {
      if ((boolean) obj) {
        return universe.getTrueClass();
      } else {
        return universe.getFalseClass();
      }
    } else if (obj instanceof Long || obj instanceof BigInteger) {
      return universe.integerClass;
    } else if (obj instanceof String) {
      return universe.stringClass;
    } else if (obj instanceof Double) {
      return universe.doubleClass;
    }

    TruffleCompiler.transferToInterpreter("Should not be reachable");
    throw new RuntimeException(
        "We got an object that should be covered by the above check: " + obj.toString());
  }
}
