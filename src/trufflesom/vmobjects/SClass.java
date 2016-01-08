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

import java.util.HashMap;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ValueProfile;

import trufflesom.compiler.Field;
import trufflesom.vm.Universe;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SInvokable.SPrimitive;


public final class SClass {

  private SClass() {
  } // just static helpers

  private static final SClassObjectType SCLASS_TYPE = new SClassObjectType();

  private static final Object INVOKABLES_TABLE = new Object();
  private static final Object OBJECT_FACTORY   = new Object();

  public static Shape createClassShape(final DynamicObject clazz, final Universe universe) {
    return SObject.LAYOUT.createShape(SCLASS_TYPE, clazz)
                         .defineProperty(universe.superclassSym, Nil.nilObject, 0)
                         .defineProperty(INVOKABLES_TABLE, new HashMap<SSymbol, SInvokable>(),
                             0)
                         .defineProperty(OBJECT_FACTORY, SObject.NIL_DUMMY_FACTORY, 0);
  }

  public static DynamicObject createWithoutClass(final Universe universe) {
    CompilerAsserts.neverPartOfCompilation("Class creation");
    DynamicObject clazz = universe.initClassFactory.newInstance(
        Nil.nilObject, // SUPERCLASS
        new HashMap<SSymbol, SInvokable>(), // INVOKABLES_TABLE
        SObject.NIL_DUMMY_FACTORY); // OBJECT_FACTORY, temporary value
    return clazz;
  }

  public static DynamicObject create(final DynamicObject clazzClazz, final Universe universe) {
    CompilerAsserts.neverPartOfCompilation("Class creation");
    Shape clazzShape = createClassShape(clazzClazz, universe);
    DynamicObjectFactory clazzFactory = clazzShape.createFactory();

    internalSetObjectFactory(clazzClazz, clazzFactory);

    DynamicObject clazz = clazzFactory.newInstance(
        Nil.nilObject, // SUPERCLASS
        new HashMap<SSymbol, SInvokable>(), // INVOKABLES_TABLE
        SObject.NIL_DUMMY_FACTORY); // OBJECT_FACTORY, temporary value

    Shape objectShape = SObject.createObjectShapeForClass(clazz);
    internalSetObjectFactory(clazz, objectShape.createFactory());

    return clazz;
  }

  private static void internalSetObjectFactory(final DynamicObject clazz,
      final DynamicObjectFactory factory) {
    Shape shape = clazz.getShape();
    Property property = shape.getProperty(OBJECT_FACTORY);
    property.setInternal(clazz, factory);
    assert shape == clazz.getShape();
  }

  public static void internalSetClass(final DynamicObject obj,
      final DynamicObject clazzClazz, final Universe universe) {
    assert obj.getShape().getObjectType() == SCLASS_TYPE;
    CompilerAsserts.neverPartOfCompilation("SObject.setClass");
    assert obj != null;
    assert clazzClazz != null;

    assert !universe.objectSystemInitialized : "This should really only be used during initialization of object system";

    Shape withoutClass = obj.getShape();
    Shape withClass = withoutClass.createSeparateShape(clazzClazz);

    obj.setShapeAndGrow(withoutClass, withClass);
    DynamicObjectFactory clazzFactory = withClass.createFactory();
    internalSetObjectFactory(clazzClazz, clazzFactory);
  }

  public static DynamicObjectFactory getFactory(final DynamicObject clazz) {
    assert clazz.getShape().getObjectType() == SCLASS_TYPE;
    return (DynamicObjectFactory) clazz.get(OBJECT_FACTORY);
  }

  public static boolean isSClass(final DynamicObject obj) {
    return obj.getShape().getObjectType() == SCLASS_TYPE;
  }

  // TODO: figure out whether this is really the best way for doing guards
  // there is the tradeoff with having two different hierarchies of shapes
  // for SClass and SObject. But, might not be performance critical in
  // either case
  private static final class SClassObjectType extends ObjectType {
    @Override
    public String toString() {
      return "SClass";
    }
  }

  // TODO: combine setters that are only used for initialization

  public static Object getSuperClass(final DynamicObject classObj, final Universe universe) {
    CompilerAsserts.neverPartOfCompilation("optimize caller");
    return classObj.get(universe.superclassSym);
  }

  public static void setSuperClass(final DynamicObject classObj, final DynamicObject value,
      final Universe universe) {
    CompilerAsserts.neverPartOfCompilation("should only be used during class initialization");
    classObj.define(universe.superclassSym, value);
  }

  public static boolean hasSuperClass(final DynamicObject classObj, final Universe universe) {
    return getSuperClass(classObj, universe) != Nil.nilObject;
  }

  public static SSymbol getName(final DynamicObject classObj, final Universe universe) {
    CompilerAsserts.neverPartOfCompilation("optimize caller");
    return (SSymbol) classObj.get(universe.nameSym);
  }

  public static void setName(final DynamicObject classObj, final SSymbol value,
      final Universe universe) {
    CompilerAsserts.neverPartOfCompilation("should only be used during class initialization");
    classObj.define(universe.nameSym, value);
  }

  public static SArray getInstanceFields(final DynamicObject classObj,
      final Universe universe) {
    return (SArray) classObj.get(universe.instanceFieldsSym);
  }

  public static Field[] getInstanceFieldDefinitions(final DynamicObject classObj,
      final Universe universe) {
    return (Field[]) classObj.get(universe.instanceFieldDefinitionsSym);
  }

  public static void setInstanceFields(final DynamicObject classObj, final List<Field> fields,
      final Universe universe) {
    CompilerAsserts.neverPartOfCompilation("should only be used during class initialization");
    Field[] fieldDef = fields.toArray(new Field[0]);
    classObj.define(universe.instanceFieldDefinitionsSym, fieldDef);

    // collect field names
    Object[] fieldNames = new Object[fields.size()];
    for (Field f : fields) {
      fieldNames[f.getIndex()] = f.getName();
    }

    classObj.define(universe.instanceFieldsSym, SArray.create(fieldNames));
  }

  public static SArray getInstanceInvokables(final DynamicObject classObj,
      final Universe universe) {
    return (SArray) classObj.get(universe.instanceInvokablesSym);
  }

  public static void setInstanceInvokables(final DynamicObject classObj, final SArray value,
      final Universe universe) {
    CompilerAsserts.neverPartOfCompilation("should only be used during class initialization");
    classObj.define(universe.instanceInvokablesSym, value);

    // Make sure this class is the holder of all invokables in the array
    for (int i = 0; i < getNumberOfInstanceInvokables(classObj, universe); i++) {
      getInstanceInvokable(classObj, i, universe).setHolder(classObj);
    }
  }

  private static final ValueProfile storageType = ValueProfile.createClassProfile();

  public static int getNumberOfInstanceInvokables(final DynamicObject classObj,
      final Universe universe) {
    // Return the number of instance invokables in this class
    return getInstanceInvokables(classObj, universe).getObjectStorage(storageType).length;
  }

  public static SInvokable getInstanceInvokable(final DynamicObject classObj,
      final int index, final Universe universe) {
    return (SInvokable) getInstanceInvokables(classObj, universe).getObjectStorage(
        storageType)[index];
  }

  public static void setInstanceInvokable(final DynamicObject classObj, final int index,
      final SInvokable value, final Universe universe) {
    CompilerAsserts.neverPartOfCompilation();
    // Set this class as the holder of the given invokable
    value.setHolder(classObj);

    getInstanceInvokables(classObj, universe).getObjectStorage(storageType)[index] = value;

    HashMap<SSymbol, SInvokable> invokablesTable = getInvokablesTable(classObj);

    if (invokablesTable.containsKey(value.getSignature())) {
      invokablesTable.put(value.getSignature(), value);
    }
  }

  @SuppressWarnings("unchecked")
  private static HashMap<SSymbol, SInvokable> getInvokablesTable(
      final DynamicObject classObj) {
    return (HashMap<SSymbol, SInvokable>) classObj.get(INVOKABLES_TABLE);
  }

  @TruffleBoundary
  public static SInvokable lookupInvokable(final DynamicObject classObj,
      final SSymbol selector, final Universe universe) {
    SInvokable invokable;
    HashMap<SSymbol, SInvokable> invokablesTable = getInvokablesTable(classObj);

    // Lookup invokable and return if found
    invokable = invokablesTable.get(selector);
    if (invokable != null) {
      return invokable;
    }

    // Lookup invokable with given signature in array of instance invokables
    for (int i = 0; i < getNumberOfInstanceInvokables(classObj, universe); i++) {
      // Get the next invokable in the instance invokable array
      invokable = getInstanceInvokable(classObj, i, universe);

      // Return the invokable if the signature matches
      if (invokable.getSignature() == selector) {
        invokablesTable.put(selector, invokable);
        return invokable;
      }
    }

    // Traverse the super class chain by calling lookup on the super class
    if (hasSuperClass(classObj, universe)) {
      invokable = lookupInvokable((DynamicObject) getSuperClass(classObj, universe), selector,
          universe);
      if (invokable != null) {
        invokablesTable.put(selector, invokable);
        return invokable;
      }
    }

    // Invokable not found
    return null;
  }

  public static int lookupFieldIndex(final DynamicObject classObj, final SSymbol fieldName,
      final Universe universe) {
    // Lookup field with given name in array of instance fields
    for (int i = getNumberOfInstanceFields(classObj, universe) - 1; i >= 0; i--) {
      // Return the current index if the name matches
      if (fieldName == getInstanceFieldName(classObj, i, universe)) {
        return i;
      }
    }

    // Field not found
    return -1;
  }

  private static boolean addInstanceInvokable(final DynamicObject classObj,
      final SInvokable value, final Universe universe) {
    CompilerAsserts.neverPartOfCompilation("SClass.addInstanceInvokable(.)");

    // Add the given invokable to the array of instance invokables
    for (int i = 0; i < getNumberOfInstanceInvokables(classObj, universe); i++) {
      // Get the next invokable in the instance invokable array
      SInvokable invokable = getInstanceInvokable(classObj, i, universe);

      // Replace the invokable with the given one if the signature matches
      if (invokable.getSignature() == value.getSignature()) {
        setInstanceInvokable(classObj, i, value, universe);
        return false;
      }
    }

    // Append the given method to the array of instance methods
    setInstanceInvokables(classObj,
        getInstanceInvokables(classObj, universe).copyAndExtendWith(value), universe);
    return true;
  }

  public static void addInstancePrimitive(final DynamicObject classObj,
      final SInvokable value, final boolean displayWarning, final Universe universe) {
    if (addInstanceInvokable(classObj, value, universe) && displayWarning) {
      Universe.print("Warning: Primitive " + value.getSignature().getString());
      Universe.println(" is not in class definition for class "
          + getName(classObj, universe).getString());
    }
  }

  public static SSymbol getInstanceFieldName(final DynamicObject classObj, final int index,
      final Universe universe) {
    return (SSymbol) getInstanceFields(classObj, universe).getObjectStorage(
        storageType)[index];
  }

  public static int getNumberOfInstanceFields(final DynamicObject classObj,
      final Universe universe) {
    return getInstanceFields(classObj, universe).getObjectStorage(storageType).length;
  }

  private static boolean includesPrimitives(final DynamicObject clazz,
      final Universe universe) {
    CompilerAsserts.neverPartOfCompilation("SClass.includesPrimitives(.)");
    // Lookup invokable with given signature in array of instance invokables
    for (int i = 0; i < getNumberOfInstanceInvokables(clazz, universe); i++) {
      // Get the next invokable in the instance invokable array
      if (getInstanceInvokable(clazz, i, universe) instanceof SPrimitive) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasPrimitives(final DynamicObject classObj, final Universe universe) {
    return includesPrimitives(classObj, universe)
        || includesPrimitives(SObject.getSOMClass(classObj), universe);
  }
}
