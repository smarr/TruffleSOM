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

package trufflesom.vmobjects;

import static trufflesom.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import java.lang.reflect.Field;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import trufflesom.interpreter.objectstorage.ObjectLayout;
import trufflesom.interpreter.objectstorage.StorageLocation;
import trufflesom.interpreter.objectstorage.StorageLocation.AbstractObjectStorageLocation;
import trufflesom.vm.Universe;
import trufflesom.vm.constants.Nil;


public class SObject extends SAbstractObject {

  @CompilationFinal protected SClass clazz;

  public static final int NUM_PRIMITIVE_FIELDS = 5;
  public static final int NUM_OBJECT_FIELDS    = 5;

  @SuppressWarnings("unused") private long primField1;
  @SuppressWarnings("unused") private long primField2;
  @SuppressWarnings("unused") private long primField3;
  @SuppressWarnings("unused") private long primField4;
  @SuppressWarnings("unused") private long primField5;

  @SuppressWarnings("unused") private Object field1;
  @SuppressWarnings("unused") private Object field2;
  @SuppressWarnings("unused") private Object field3;
  @SuppressWarnings("unused") private Object field4;
  @SuppressWarnings("unused") private Object field5;

  @CompilationFinal(dimensions = 0) private long[]   extensionPrimFields;
  @CompilationFinal(dimensions = 0) private Object[] extensionObjFields;

  // we manage the layout entirely in the class, but need to keep a copy here
  // to know in case the layout changed that we can update the instances lazily
  @CompilationFinal private ObjectLayout objectLayout;

  private int primitiveUsedMap;

  private final int numberOfFields;

  protected SObject(final SClass instanceClass) {
    numberOfFields = instanceClass.getNumberOfInstanceFields();
    clazz = instanceClass;
    setLayoutInitially(instanceClass.getLayoutForInstances());
  }

  protected SObject(final int numFields) {
    numberOfFields = numFields;
    setLayoutInitially(new ObjectLayout(numFields, null));
  }

  private void setLayoutInitially(final ObjectLayout layout) {
    field1 = field2 = field3 = field4 = field5 = Nil.nilObject;

    objectLayout = layout;
    // Can't check this cheaply
    // assert objectLayout.getNumberOfFields() == numberOfFields
    // || !Universe.current().isObjectSystemInitialized();

    extensionPrimFields = getExtendedPrimStorage();
    extensionObjFields = getExtendedObjectStorage();
  }

  public final int getNumberOfFields() {
    return numberOfFields;
  }

  public final ObjectLayout getObjectLayout() {
    // TODO: should I really remove it, or should I update the layout?
    // assert clazz.getLayoutForInstances() == objectLayout;
    return objectLayout;
  }

  public final long[] getExtendedPrimFields() {
    return extensionPrimFields;
  }

  public final Object[] getExtensionObjFields() {
    return extensionObjFields;
  }

  public final void setClass(final SClass value) {
    transferToInterpreterAndInvalidate("SObject.setClass");
    assert value != null;

    // Set the class of this object by writing to the field with class index
    clazz = value;
    setLayoutInitially(value.getLayoutForInstances());
  }

  private long[] getExtendedPrimStorage() {
    return new long[objectLayout.getNumberOfUsedExtendedPrimStorageLocations()];
  }

  private Object[] getExtendedObjectStorage() {
    Object[] storage =
        new Object[objectLayout.getNumberOfUsedExtendedObjectStorageLocations()];
    Arrays.fill(storage, Nil.nilObject);
    return storage;
  }

  @ExplodeLoop
  private Object[] getAllFields() {
    Object[] fieldValues = new Object[numberOfFields];
    for (int i = 0; i < numberOfFields; i++) {
      if (isFieldSet(i)) {
        fieldValues[i] = getField(i);
      }
    }
    return fieldValues;
  }

  @ExplodeLoop
  private void setAllFields(final Object[] fieldValues) {
    field1 = field2 = field3 = field4 = field5 = null;
    primField1 = primField2 = primField3 = primField4 = primField5 = Long.MIN_VALUE;

    assert fieldValues.length == numberOfFields;

    for (int i = 0; i < numberOfFields; i++) {
      if (fieldValues[i] != null) {
        setField(i, fieldValues[i]);
      } else if (getLocation(i) instanceof AbstractObjectStorageLocation) {
        setField(i, Nil.nilObject);
      }
    }
  }

  public final boolean updateLayoutToMatchClass() {
    ObjectLayout layoutAtClass = clazz.getLayoutForInstances();
    assert layoutAtClass.getNumberOfFields() == numberOfFields;

    if (objectLayout != layoutAtClass) {
      assert !objectLayout.isValid();
      assert layoutAtClass.isValid();
      setLayoutAndTransferFields(layoutAtClass);
      return true;
    } else {
      return false;
    }
  }

  private void setLayoutAndTransferFields(final ObjectLayout layout) {
    CompilerDirectives.transferToInterpreterAndInvalidate();

    Object[] fieldValues = getAllFields();

    objectLayout = layout;

    primitiveUsedMap = 0;
    extensionPrimFields = getExtendedPrimStorage();
    extensionObjFields = getExtendedObjectStorage();

    setAllFields(fieldValues);
  }

  protected final void updateLayoutWithInitializedField(final long index,
      final Class<?> type) {
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
  public final SClass getSOMClass(final Universe universe) {
    return clazz;
  }

  public final long getFieldIndex(final SSymbol fieldName) {
    return clazz.lookupFieldIndex(fieldName);
  }

  public static final SObject create(final SClass instanceClass) {
    return new SObject(instanceClass);
  }

  public static SObject create(final int numFields) {
    return new SObject(numFields);
  }

  private static final long FIRST_OBJECT_FIELD_OFFSET = getFirstObjectFieldOffset();
  private static final long FIRST_PRIM_FIELD_OFFSET   = getFirstPrimFieldOffset();
  private static final long OBJECT_FIELD_LENGTH       = getObjectFieldLength();
  private static final long PRIM_FIELD_LENGTH         = getPrimFieldLength();

  public static long getObjectFieldOffset(final int fieldIndex) {
    assert 0 <= fieldIndex && fieldIndex < NUM_OBJECT_FIELDS;
    return FIRST_OBJECT_FIELD_OFFSET + fieldIndex * OBJECT_FIELD_LENGTH;
  }

  public static long getPrimitiveFieldOffset(final int fieldIndex) {
    assert 0 <= fieldIndex && fieldIndex < NUM_PRIMITIVE_FIELDS;
    return FIRST_PRIM_FIELD_OFFSET + fieldIndex * PRIM_FIELD_LENGTH;
  }

  public static int getPrimitiveFieldMask(final int fieldIndex) {
    assert 0 <= fieldIndex && fieldIndex < 32; // this limits the number of object fields for
                                               // the moment...
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
    return location.isSet(this);
  }

  public final Object getField(final long index) {
    CompilerAsserts.neverPartOfCompilation("getField");
    StorageLocation location = getLocation(index);
    return location.read(this);
  }

  public final void setUninitializedField(final long index, final Object value) {
    CompilerAsserts.neverPartOfCompilation("setUninitializedField");
    updateLayoutWithInitializedField(index, value.getClass());
    setFieldAfterLayoutChange(index, value);
  }

  public final void setFieldAndGeneralize(final long index, final Object value) {
    CompilerAsserts.neverPartOfCompilation("setFieldAndGeneralize");
    updateLayoutWithGeneralizedField(index);
    setFieldAfterLayoutChange(index, value);
  }

  public final void setField(final long index, final Object value) {
    CompilerAsserts.neverPartOfCompilation("setField");
    StorageLocation location = getLocation(index);

    location.write(this, value);
  }

  private void setFieldAfterLayoutChange(final long index, final Object value) {
    CompilerAsserts.neverPartOfCompilation("SObject.setFieldAfterLayoutChange(..)");

    StorageLocation location = getLocation(index);
    location.write(this, value);
  }

  private static long getFirstObjectFieldOffset() {
    CompilerAsserts.neverPartOfCompilation("SObject.getFirstObjectFieldOffset()");
    try {
      final Field firstField = SObject.class.getDeclaredField("field1");
      return StorageLocation.getFieldOffset(firstField);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  private static long getFirstPrimFieldOffset() {
    CompilerAsserts.neverPartOfCompilation("SObject.getFirstPrimFieldOffset()");
    try {
      final Field firstField = SObject.class.getDeclaredField("primField1");
      return StorageLocation.getFieldOffset(firstField);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  private static long getObjectFieldLength() {
    CompilerAsserts.neverPartOfCompilation("getObjectFieldLength()");

    try {
      long dist = getFieldDistance("field1", "field2");
      // this can go wrong if the VM rearranges fields to fill holes in the
      // memory layout of the object structure
      assert dist == 4
          || dist == 8 : "We expect these fields to be adjecent and either 32 or 64bit appart.";
      return dist;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static long getPrimFieldLength() {
    CompilerAsserts.neverPartOfCompilation("getPrimFieldLength()");

    try {
      long dist = getFieldDistance("primField1", "primField2");
      // this can go wrong if the VM rearranges fields to fill holes in the
      // memory layout of the object structure
      assert dist == 8 : "We expect these fields to be adjecent and 64bit appart.";
      return dist;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static long getFieldDistance(final String field1, final String field2)
      throws NoSuchFieldException,
      IllegalAccessException {
    final Field firstField = SObject.class.getDeclaredField(field1);
    final Field secondField = SObject.class.getDeclaredField(field2);
    return StorageLocation.getFieldOffset(secondField)
        - StorageLocation.getFieldOffset(firstField);
  }
}
