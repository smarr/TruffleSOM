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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import som.interpreter.objectstorage.ObjectLayout;
import som.interpreter.objectstorage.StorageLocation;
import som.interpreter.objectstorage.StorageLocation.AbstractObjectStorageLocation;
import som.interpreter.objectstorage.StorageLocation.GeneralizeStorageLocationException;
import som.interpreter.objectstorage.StorageLocation.UninitalizedStorageLocationException;
import som.vm.Universe;
import som.vm.constants.Nil;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeFieldAccessor;
import com.oracle.truffle.api.nodes.NodeUtil.FieldOffsetProvider;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.impl.*;
import com.oracle.truffle.api.object.Layout;

public class SObjectOSM extends SAbstractObject { //implements TruffleObject {

  @CompilationFinal protected SClass clazz;
  @CompilationFinal private SOMDynamicObject dynamicObject;
  
  public static final Layout LAYOUT = Layout.createLayout(Layout.INT_TO_LONG);
  
  public static final int NUM_PRIMITIVE_FIELDS = 5;
  public static final int NUM_OBJECT_FIELDS    = 5;
  
  private int primitiveUsedMap;

  //private final int numberOfFields;

  protected SObjectOSM(final SClass instanceClass) {
    clazz = instanceClass;
  }

  protected SObjectOSM() {}

  public final int getNumberOfFields() {
    return dynamicObject.size();
  }
  
  private SOMDynamicObject getDynamicObject() {
    return this.dynamicObject;
  }

  /*public final ObjectLayout getObjectLayout() {
    // TODO: should I really remove it, or should I update the layout?
    // assert clazz.getLayoutForInstances() == objectLayout;
    return objectLayout;
  }*/

  public final void setClass(final SClass value) {
    transferToInterpreterAndInvalidate("SObject.setClass");
    assert value != null;
    // Set the class of this object by writing to the field with class index
    clazz = value;
    //setLayoutInitially(value.getLayoutForInstances());
  }

  private long[] getExtendedPrimitiveStorage() {
    return new long[objectLayout.getNumberOfUsedExtendedPrimStorageLocations()];
  }

  private Object[] getExtendedObjectStorage() {
    Object[] storage = new Object[objectLayout.getNumberOfUsedExtendedObjectStorageLocations()];
    Arrays.fill(storage, Nil.nilObject);
    return storage;
  }

  private List<Object> getAllFields() {
   return this.getDynamicObject().getValues();
  }

  @ExplodeLoop
  private void setAllFields(final List<Object> fieldValues) {
    //field1 = field2 = field3 = field4 = field5 = null;
    //primField1 = primField2 = primField3 = primField4 = primField5 = Long.MIN_VALUE;
    assert fieldValues.size() == this.getNumberOfFields();
    for (int i = 0; i < this.getNumberOfFields(); i++) {
      if (fieldValues.get(i) != null) {
        this.getDynamicObject().set(i, fieldValues.get(i));
      } else {
        this.getDynamicObject().set(i, Nil.nilObject);
      }
    }
  }

  public final boolean updateLayoutToMatchClass() {
    ObjectLayout layoutAtClass = clazz.getLayoutForInstances();
    assert layoutAtClass.getNumberOfFields() == numberOfFields;

    if (objectLayout != layoutAtClass) {
      setLayoutAndTransferFields(layoutAtClass);
      return true;
    } else {
      return false;
    }
  }

  private void setLayoutAndTransferFields(final ObjectLayout layout) {
    CompilerDirectives.transferToInterpreterAndInvalidate();

    List<Object> fieldValues = this.getAllFields();
    objectLayout        = layout;
    primitiveUsedMap    = 0;
    //extensionPrimFields = getExtendedPrimStorage();
    //extensionObjFields  = getExtendedObjectStorage();
    this.setAllFields(fieldValues);
  }

  protected final void updateLayoutWithInitializedField(final long index, final Class<?> type) {
    ObjectLayout layout = clazz.updateInstanceLayoutWithInitializedField(index, type);

    assert objectLayout != layout;
    assert layout.getNumberOfFields() == numberOfFields;

    setLayoutAndTransferFields(layout);
  }

  protected final void updateLayoutWithGeneralizedField(final long index) {
    ObjectLayout layout = clazz.updateInstanceLayoutWithGeneralizedField(index);

    assert objectLayout != layout;
    assert layout.getNumberOfFields() == numberOfFields;

    setLayoutAndTransferFields(layout);
  }

  @Override
  public final SClass getSOMClass() {
    return clazz;
  }

  public final long getFieldIndex(final SSymbol fieldName) {
    return clazz.lookupFieldIndex(fieldName);
  }

  public static SObjectOSM create(final SClass instanceClass) {
    return new SObjectOSM(instanceClass);
  }

  public static SObjectOSM create(final int numFields) {
    return new SObjectOSM(numFields);
  }

  private static final long FIRST_OBJECT_FIELD_OFFSET = getFirstObjectFieldOffset();
  private static final long FIRST_PRIM_FIELD_OFFSET   = getFirstPrimFieldOffset();
  private static final long OBJECT_FIELD_LENGTH = getObjectFieldLength();
  private static final long PRIM_FIELD_LENGTH   = getPrimFieldLength();

  public static long getObjectFieldOffset(final int fieldIndex) {
    //assert 0 <= fieldIndex && fieldIndex < NUM_OBJECT_FIELDS;
    return FIRST_OBJECT_FIELD_OFFSET + fieldIndex * OBJECT_FIELD_LENGTH;
  }

  public static long getPrimitiveFieldOffset(final int fieldIndex) {
    //assert 0 <= fieldIndex && fieldIndex < NUM_PRIMITIVE_FIELDS;
    return FIRST_PRIM_FIELD_OFFSET + fieldIndex * PRIM_FIELD_LENGTH;
  }

  public static int getPrimitiveFieldMask(final int fieldIndex) {
    assert 0 <= fieldIndex && fieldIndex < 32; // this limits the number of object fields for the moment...
    return 1 << fieldIndex;
  }

  public final boolean isPrimitiveSet(final int mask) {
    return (primitiveUsedMap & mask) != 0;
  }

  public final void markPrimAsSet(final int mask) {
    primitiveUsedMap |= mask;
  }

  private StorageLocation getLocation(final long index) {
    StorageLocation location = objectLayout.getStorageLocation(index);
    assert location != null;
    return location;
  }

  private boolean isFieldSet(final long index) {
    CompilerAsserts.neverPartOfCompilation("isFieldSet");
    StorageLocation location = getLocation(index);
    //return location.isSet(this, true);
    return true;
  }

  public final Object getField(final long index) {
    CompilerAsserts.neverPartOfCompilation("getField");
    StorageLocation location = getLocation(index);
    //return location.read(this, true);
    return true;
  }

  public final void setField(final long index, final Object value) {
    CompilerAsserts.neverPartOfCompilation("setField");
    StorageLocation location = getLocation(index);

    /*try {
      location.write(this, value);
    } catch (UninitalizedStorageLocationException e) {
      updateLayoutWithInitializedField(index, value.getClass());
      setFieldAfterLayoutChange(index, value);
    } catch (GeneralizeStorageLocationException e) {
      updateLayoutWithGeneralizedField(index);
      setFieldAfterLayoutChange(index, value);
    }*/
  }

  private void setFieldAfterLayoutChange(final long index, final Object value) {
    CompilerAsserts.neverPartOfCompilation("SObject.setFieldAfterLayoutChange(..)");

    StorageLocation location = getLocation(index);
    /*try {
      location.write(this, value);
    } catch (GeneralizeStorageLocationException
        | UninitalizedStorageLocationException e) {
      throw new RuntimeException("This should not happen, we just prepared this field for the new value.");
    }*/
  }

  private static long getFirstObjectFieldOffset() {
    CompilerAsserts.neverPartOfCompilation("SObject.getFirstObjectFieldOffset()");
    try {
      final FieldOffsetProvider fieldOffsetProvider = getFieldOffsetProvider();

      final Field firstField = SObjectOSM.class.getDeclaredField("field1");
      return fieldOffsetProvider.objectFieldOffset(firstField);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static long getFirstPrimFieldOffset() {
    CompilerAsserts.neverPartOfCompilation("SObject.getFirstPrimFieldOffset()");
    try {
      final FieldOffsetProvider fieldOffsetProvider = getFieldOffsetProvider();

      final Field firstField = SObjectOSM.class.getDeclaredField("primField1");
      return fieldOffsetProvider.objectFieldOffset(firstField);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static FieldOffsetProvider getFieldOffsetProvider()
      throws NoSuchFieldException, IllegalAccessException {
    final Field fieldOffsetProviderField =
        NodeFieldAccessor.class.getDeclaredField("unsafeFieldOffsetProvider");
    fieldOffsetProviderField.setAccessible(true);
    final FieldOffsetProvider fieldOffsetProvider =
        (FieldOffsetProvider) fieldOffsetProviderField.get(null);
    return fieldOffsetProvider;
  }

  private static long getObjectFieldLength() {
    CompilerAsserts.neverPartOfCompilation("getObjectFieldLength()");

    try {
      return getFieldDistance("field1", "field2");
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static long getPrimFieldLength() {
    CompilerAsserts.neverPartOfCompilation("getPrimFieldLength()");

    try {
      return getFieldDistance("primField1", "primField2");
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static long getFieldDistance(final String field1, final String field2) throws NoSuchFieldException,
      IllegalAccessException {
    final FieldOffsetProvider fieldOffsetProvider = getFieldOffsetProvider();

    final Field firstField  = SObjectOSM.class.getDeclaredField(field1);
    final Field secondField = SObjectOSM.class.getDeclaredField(field2);
    return fieldOffsetProvider.objectFieldOffset(secondField) - fieldOffsetProvider.objectFieldOffset(firstField);
  }
  
  
  /*
    Todo: Implement interop
   * 
  @Override
  public ForeignAccess getForeignAccess() {
    return new ForeignAccess(getContext());
  }*/
 }
