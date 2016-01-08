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

import java.util.List;

import som.interpreter.objectstorage.MateLocationFactory;
import som.vm.constants.Nil;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Layout;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.object.basic.DynamicObjectBasic;

public class SObject extends SAbstractObject {

  @CompilationFinal protected SClass clazz;
  @CompilationFinal private DynamicObject dynamicObject;

  public static final Layout LAYOUT = Layout.createLayout();

  protected SObject(final SClass instanceClass) {
    clazz          = instanceClass;
    dynamicObject  = new DynamicObjectBasic(instanceClass.getLayoutForInstances());
    this.initializeFields();
  }

  protected SObject(final int numFields) {
    dynamicObject = new DynamicObjectBasic(LAYOUT.createShape(new MateObjectType()));
    for (int i = 0; i < numFields; i++) {
       this.getDynamicObject().define(i, Nil.nilObject , 0, new MateLocationFactory());
    }
  }

  public final int getNumberOfFields() {
    return dynamicObject.size();
  }

  public DynamicObject getDynamicObject() {
    return this.dynamicObject;
  }

  public final Shape getObjectLayout() {
    // TODO: should I really remove it, or should I update the layout?
    return this.dynamicObject.getShape();
  }

  public final void setClass(final SClass value) {
    transferToInterpreterAndInvalidate("SObject.setClass");
    assert value != null;
    // Set the class of this object by writing to the field with class index
    clazz = value;
  }

  // @ExplodeLoop
  // TODO: make sure we have a compilation constant for the number of fields
  //        and reenable @ExplodeLoop, or better, avoid preinitializing fields completely.
  private void initializeFields() {
    for (int i = 0; i < this.getNumberOfFields(); i++) {
      this.getDynamicObject().define(i, Nil.nilObject , 0, new MateLocationFactory());
    }
  }

  @ExplodeLoop
  private void setAllFields(final List<Object> fieldValues) {
    assert fieldValues.size() == this.getNumberOfFields();
    for (int i = 0; i < this.getNumberOfFields(); i++) {
      if (fieldValues.get(i) != null) {
        this.getDynamicObject().set(i, fieldValues.get(i));
      } else {
        this.getDynamicObject().set(i, Nil.nilObject);
      }
    }
  }

  /*
  public final boolean updateLayoutToMatchClass() {
    Shape layoutAtClass = clazz.getLayoutForInstances();
    assert layoutAtClass.getPropertyCount() == this.dynamicObject.size();

    if (this.dynamicObject.getShape() != layoutAtClass) {
      //setLayoutAndTransferFields(layoutAtClass);
      return true;
    } else {
      return false;
    }
  }
  */

  @Override
  public final SClass getSOMClass() {
    return clazz;
  }

  public final int getFieldIndex(final SSymbol fieldName) {
    return clazz.lookupFieldIndex(fieldName);
  }

  public static SObject create(final SClass instanceClass) {
    return new SObject(instanceClass);
  }

  public static SObject create(final int numFields) {
    return new SObject(numFields);
  }

  public final Object getField(final int index) {
    return this.dynamicObject.get(index, Nil.nilObject);
  }

  public final void setField(final int index, final Object value) {
    this.dynamicObject.set(index, value);
  }

  /*
    Todo: Implement interop
   *
  @Override
  public ForeignAccess getForeignAccess() {
    return new ForeignAccess(getContext());
  }*/
 }