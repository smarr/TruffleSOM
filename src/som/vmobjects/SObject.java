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

import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import java.util.Arrays;

import som.vm.constants.Nil;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public class SObject extends SAbstractObject {

  @CompilationFinal protected SClass clazz;

  private final Object[] objectFields;

  private final int numberOfFields;

  protected SObject(final SClass instanceClass) {
    this(instanceClass.getNumberOfInstanceFields());
    clazz = instanceClass;
  }

  protected SObject(final int numFields) {
    numberOfFields = numFields;
    objectFields = new Object[numberOfFields];
    Arrays.fill(objectFields, Nil.nilObject);
  }

  public final int getNumberOfFields() {
    return numberOfFields;
  }

  public final void setClass(final SClass value) {
    transferToInterpreterAndInvalidate("SObject.setClass");
    assert value != null;

    // Set the class of this object by writing to the field with class index
    clazz = value;
  }

  @Override
  public final SClass getSOMClass() {
    return clazz;
  }

  public final int getFieldIndex(final SSymbol fieldName) {
    return clazz.lookupFieldIndex(fieldName);
  }

  public static final SObject create(final SClass instanceClass) {
    return new SObject(instanceClass);
  }

  public static SObject create(final int numFields) {
    return new SObject(numFields);
  }

  public final Object getField(final int index) {
    return objectFields[index];
  }

  public final void setField(final int index, final Object value) {
    objectFields[index] = value;
  }
}
