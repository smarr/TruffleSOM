/**
 * Copyright (c) 2015 Guido Chari, gchari@dc.uba.ar
 * LaFHIS lab, Universidad de Buenos Aires, Buenos Aires, Argentina
 * http://www.lafhis.dc.uba.ar
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

package som.vmobjects;

import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;
import som.vm.Universe;
import som.vm.constants.Nil;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.Shape;

public class SReflectiveObject extends SObject {
  protected static final SSymbol ENVIRONMENT = Universe.current().symbolFor("environment");
  protected static final Shape SREFLECTIVE_OBJECT_SHAPE = 
      SOBJECT_SHAPE.createSeparateShape(SOBJECT_SHAPE.getData()).defineProperty(ENVIRONMENT, Nil.nilObject, 0);
      
  private static final DynamicObjectFactory SREFLECTIVE_OBJECT_FACTORY = SREFLECTIVE_OBJECT_SHAPE.createFactory();
  
  public static DynamicObject create(final DynamicObject instanceClass) {
    return SREFLECTIVE_OBJECT_FACTORY.newInstance(instanceClass);
  }

  public static DynamicObject create(final int numFields) {
    return SREFLECTIVE_OBJECT_FACTORY.newInstance(Nil.nilObject);
  }
  
  public static final SMateEnvironment getEnvironment(final DynamicObject obj) {
    CompilerAsserts.neverPartOfCompilation("Caller needs to be optimized");
    return (SMateEnvironment) obj.get(ENVIRONMENT);
  }

  public static final void setEnvironment(final DynamicObject obj, final SMateEnvironment value) {
    transferToInterpreterAndInvalidate("SReflectiveObject.setEnvironment");
    obj.set(ENVIRONMENT, value);
  }
}