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

import java.util.HashMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.profiles.ValueProfile;

import trufflesom.interpreter.objectstorage.ObjectLayout;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SInvokable.SPrimitive;


public final class SClass extends SObject {

  private static final ValueProfile storageType = ValueProfile.createClassProfile();

  public SClass(final int numberOfFields) {
    // Initialize this class by calling the super constructor with the given
    // value
    super(numberOfFields);
    invokablesTable = new HashMap<SSymbol, SInvokable>();
    this.superclass = Nil.nilObject;

    layoutForInstances = new ObjectLayout(numberOfFields, this);
  }

  public SClass(final SClass clazz) {
    super(clazz);
    invokablesTable = new HashMap<SSymbol, SInvokable>();
    this.superclass = Nil.nilObject;
  }

  public SObject getSuperClass() {
    return superclass;
  }

  public void setSuperClass(final SClass value) {
    transferToInterpreterAndInvalidate("SClass.setSuperClass");
    superclass = value;
  }

  public boolean hasSuperClass() {
    return superclass != Nil.nilObject;
  }

  public SSymbol getName() {
    return name;
  }

  public void setName(final SSymbol value) {
    transferToInterpreterAndInvalidate("SClass.setName");
    name = value;
  }

  public SArray getInstanceFields() {
    return instanceFields;
  }

  public void setInstanceFields(final SArray fields) {
    transferToInterpreterAndInvalidate("SClass.setInstanceFields");
    instanceFields = fields;
    if (layoutForInstances == null ||
        instanceFields.getObjectStorage(
            storageType).length != layoutForInstances.getNumberOfFields()) {
      layoutForInstances = new ObjectLayout(
          fields.getObjectStorage(storageType).length, this);
    }
  }

  public SArray getInstanceInvokables() {
    return instanceInvokables;
  }

  public void setInstanceInvokables(final SArray value) {
    transferToInterpreterAndInvalidate("SClass.setInstanceInvokables");
    instanceInvokables = value;

    // Make sure this class is the holder of all invokables in the array
    for (int i = 0; i < getNumberOfInstanceInvokables(); i++) {
      ((SInvokable) instanceInvokables.getObjectStorage(storageType)[i]).setHolder(this);
    }
  }

  public int getNumberOfInstanceInvokables() {
    // Return the number of instance invokables in this class
    return instanceInvokables.getObjectStorage(storageType).length;
  }

  public SInvokable getInstanceInvokable(final int index) {
    return (SInvokable) instanceInvokables.getObjectStorage(storageType)[index];
  }

  public void setInstanceInvokable(final int index, final SInvokable value) {
    CompilerAsserts.neverPartOfCompilation();
    // Set this class as the holder of the given invokable
    value.setHolder(this);

    instanceInvokables.getObjectStorage(storageType)[index] = value;

    if (invokablesTable.containsKey(value.getSignature())) {
      invokablesTable.put(value.getSignature(), value);
    }
  }

  @TruffleBoundary
  public SInvokable lookupInvokable(final SSymbol selector) {
    SInvokable invokable;

    // Lookup invokable and return if found
    invokable = invokablesTable.get(selector);
    if (invokable != null) {
      return invokable;
    }

    // Lookup invokable with given signature in array of instance invokables
    for (int i = 0; i < getNumberOfInstanceInvokables(); i++) {
      // Get the next invokable in the instance invokable array
      invokable = getInstanceInvokable(i);

      // Return the invokable if the signature matches
      if (invokable.getSignature() == selector) {
        invokablesTable.put(selector, invokable);
        return invokable;
      }
    }

    // Traverse the super class chain by calling lookup on the super class
    if (hasSuperClass()) {
      invokable = ((SClass) getSuperClass()).lookupInvokable(selector);
      if (invokable != null) {
        invokablesTable.put(selector, invokable);
        return invokable;
      }
    }

    // Invokable not found
    return null;
  }

  public int lookupFieldIndex(final SSymbol fieldName) {
    // Lookup field with given name in array of instance fields
    for (int i = getNumberOfInstanceFields() - 1; i >= 0; i--) {
      // Return the current index if the name matches
      if (fieldName == getInstanceFieldName(i)) {
        return i;
      }
    }

    // Field not found
    return -1;
  }

  public boolean addInstanceInvokable(final SInvokable value) {
    CompilerAsserts.neverPartOfCompilation("SClass.addInstanceInvokable(.)");

    // Add the given invokable to the array of instance invokables
    for (int i = 0; i < getNumberOfInstanceInvokables(); i++) {
      // Get the next invokable in the instance invokable array
      SInvokable invokable = getInstanceInvokable(i);

      // Replace the invokable with the given one if the signature matches
      if (invokable.getSignature() == value.getSignature()) {
        setInstanceInvokable(i, value);
        return false;
      }
    }

    // Append the given method to the array of instance methods
    instanceInvokables = instanceInvokables.copyAndExtendWith(value);
    return true;
  }

  public SSymbol getInstanceFieldName(final int index) {
    return (SSymbol) instanceFields.getObjectStorage(storageType)[index];
  }

  public int getNumberOfInstanceFields() {
    return instanceFields.getObjectStorage(storageType).length;
  }

  private static boolean includesPrimitives(final SClass clazz) {
    CompilerAsserts.neverPartOfCompilation("SClass.includesPrimitives(.)");
    // Lookup invokable with given signature in array of instance invokables
    for (int i = 0; i < clazz.getNumberOfInstanceInvokables(); i++) {
      // Get the next invokable in the instance invokable array
      if (clazz.getInstanceInvokable(i) instanceof SPrimitive) {
        return true;
      }
    }
    return false;
  }

  public boolean hasPrimitives() {
    return includesPrimitives(this) || includesPrimitives(clazz);
  }

  public ObjectLayout getLayoutForInstances() {
    return layoutForInstances;
  }

  public ObjectLayout updateInstanceLayoutWithInitializedField(final long index,
      final Class<?> type) {
    ObjectLayout updated = layoutForInstances.withInitializedField(index, type);

    if (updated != layoutForInstances) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      layoutForInstances = updated;
    }
    return layoutForInstances;
  }

  public ObjectLayout updateInstanceLayoutWithGeneralizedField(final long index) {
    ObjectLayout updated = layoutForInstances.withGeneralizedField(index);

    if (updated != layoutForInstances) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      layoutForInstances = updated;
    }
    return layoutForInstances;
  }

  @Override
  public String toString() {
    return "Class(" + getName().getString() + ")";
  }

  // Mapping of symbols to invokables
  private final HashMap<SSymbol, SInvokable> invokablesTable;

  @CompilationFinal private SObject superclass;
  @CompilationFinal private SSymbol name;
  @CompilationFinal private SArray  instanceInvokables;
  @CompilationFinal private SArray  instanceFields;

  @CompilationFinal private ObjectLayout layoutForInstances;
}
