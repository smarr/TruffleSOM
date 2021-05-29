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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.compiler.Field;
import trufflesom.interpreter.objectstorage.ObjectLayout;
import trufflesom.vm.constants.Nil;


public final class SClass extends SObject {

  private SourceSection sourceSection;
  private boolean       hasPrimitives;

  public SClass(final int numberOfFields) {
    // Initialize this class by calling the super constructor with the given
    // value
    super(numberOfFields);
    invokablesTable = null;
    this.superclass = Nil.nilObject;

    layoutForInstances = new ObjectLayout(numberOfFields, this);
  }

  public SClass(final SClass clazz) {
    super(clazz);
    invokablesTable = null;
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

  public SourceSection getSourceSection() {
    return sourceSection;
  }

  public void setSourceSection(final SourceSection source) {
    sourceSection = source;
  }

  public Field[] getInstanceFieldDefinitions() {
    return instanceFieldDefinitions;
  }

  public SArray getInstanceFields() {
    return instanceFields;
  }

  public void setInstanceFields(final List<Field> fields) {
    instanceFieldDefinitions = fields.toArray(new Field[0]);

    // collect field names
    Object[] fieldNames = new Object[fields.size()];
    for (Field f : fields) {
      fieldNames[f.getIndex()] = f.getName();
    }

    instanceFields = SArray.create(fieldNames);

    // Check and possibly update layout
    if (layoutForInstances == null
        || fields.size() != layoutForInstances.getNumberOfFields()) {
      if (layoutForInstances != null) {
        layoutForInstances.invalidate();
      }
      layoutForInstances = new ObjectLayout(fields.size(), this);
    }
  }

  @TruffleBoundary
  public SArray getInstanceInvokables() {
    if (invokablesTable == null) {
      return SArray.create(0);
    }

    ArrayList<SInvokable> invokables = new ArrayList<>();

    for (SInvokable i : invokablesTable.values()) {
      if (i.getHolder() == this) {
        invokables.add(i);
      }
    }

    return SArray.create(invokables.toArray(new Object[0]));
  }

  public void setInstanceInvokables(final LinkedHashMap<SSymbol, SInvokable> value,
      final boolean hasPrimitives) {
    this.hasPrimitives = hasPrimitives;

    transferToInterpreterAndInvalidate("SClass.setInstanceInvokables");
    if (value == null || value.isEmpty()) {
      assert invokablesTable == null;
      return;
    }

    invokablesTable = value;

    // Make sure this class is the holder of all invokables
    for (SInvokable i : invokablesTable.values()) {
      i.setHolder(this);
    }
  }

  public int getNumberOfInstanceInvokables() {
    // Return the number of instance invokables in this class
    if (invokablesTable == null) {
      return 0;
    }
    return invokablesTable.size();
  }

  public Collection<SInvokable> getInstanceInvokablesForDisassembler() {
    if (invokablesTable == null) {
      return null;
    }
    return invokablesTable.values();
  }

  @TruffleBoundary
  public SInvokable lookupInvokable(final SSymbol selector) {
    SInvokable invokable;

    if (invokablesTable != null) {
      // Lookup invokable and return if found
      invokable = invokablesTable.get(selector);
      if (invokable != null) {
        return invokable;
      }
    }

    // Traverse the super class chain by calling lookup on the super class
    if (hasSuperClass()) {
      invokable = ((SClass) getSuperClass()).lookupInvokable(selector);
      if (invokable != null) {
        if (invokablesTable == null) {
          invokablesTable = new LinkedHashMap<>();
        }
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

  public void addPrimitive(final SInvokable value) {
    CompilerAsserts.neverPartOfCompilation("SClass.addInstanceInvokable(.)");

    value.setHolder(this);
    invokablesTable.put(value.getSignature(), value);
  }

  public SSymbol getInstanceFieldName(final int index) {
    return (SSymbol) instanceFields.getObjectStorage()[index];
  }

  public int getNumberOfInstanceFields() {
    return instanceFields.getObjectStorage().length;
  }

  public boolean hasPrimitives() {
    return this.hasPrimitives || clazz.hasPrimitives;
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
  @CompilationFinal private HashMap<SSymbol, SInvokable> invokablesTable;

  @CompilationFinal private SObject superclass;
  @CompilationFinal private SSymbol name;
  @CompilationFinal private SArray  instanceFields;

  @CompilationFinal(dimensions = 1) private Field[] instanceFieldDefinitions;

  @CompilationFinal private ObjectLayout layoutForInstances;
}
