/**
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
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

import som.vm.NotYetImplementedException;
import som.vm.Universe;
import som.vm.constants.Nil;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.Layout;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Shape;


public final class SObject {

  protected static final SSymbol CLASS = Universe.current().symbolFor("class");
  private static final SObjectObjectType SOBJECT_TYPE = new SObjectObjectType();

  protected static final Layout LAYOUT = Layout.createLayout();

  // Object shape with property for a class
  protected static final Shape SOBJECT_SHAPE = LAYOUT.
      createShape(SOBJECT_TYPE).defineProperty(CLASS, Nil.nilObject, 0);
  private static final Location CLASS_LOCATION = SOBJECT_SHAPE.getProperty(CLASS).getLocation();

  private SObject() { } // this class cannot be instantiated, it provides only static helpers

  public static DynamicObject create(final DynamicObject instanceClass) {
    DynamicObject obj = LAYOUT.newInstance(SOBJECT_SHAPE);
    try {
      CLASS_LOCATION.set(obj, instanceClass);
    } catch (IncompatibleLocationException | FinalLocationException e) {
      CompilerDirectives.transferToInterpreter();
      throw new RuntimeException("This should never happen!");
    }
    return obj;
  }

  public static DynamicObject create(final int numFields) {
    return LAYOUT.newInstance(SOBJECT_SHAPE);
  }

  public static boolean isSObject(final DynamicObject obj) {
    return obj.getShape().getObjectType() == SOBJECT_TYPE;
  }

  private static final class SObjectObjectType extends ObjectType {
    @Override
    public String toString() {
      return "SObject";
    }
  }

  public static DynamicObject getSOMClass(final DynamicObject obj) {
    CompilerAsserts.neverPartOfCompilation("Caller needs to be optimized");
    return (DynamicObject) obj.get(CLASS);
  }

  public static final void setClass(final DynamicObject obj, final DynamicObject value) {
    CompilerAsserts.neverPartOfCompilation("SObject.setClass");
    assert value != null;
    obj.set(CLASS, value);
  }

  public static final long getFieldIndex(final DynamicObject obj, final SSymbol fieldName) {
    return SClass.lookupFieldIndex(getSOMClass(obj), fieldName);
  }

  public static final int getNumberOfFields(final DynamicObject obj) {
    throw new NotYetImplementedException();
  }
}
