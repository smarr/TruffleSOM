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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.Layout;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.object.ShapeImpl;
import com.oracle.truffle.object.Transition;
import com.oracle.truffle.object.basic.ShapeBasic;


public class SObject {

  private static final SObjectObjectType SOBJECT_TYPE = new SObjectObjectType();

  protected static final Layout LAYOUT = Layout.createLayout();

  // Object shape with property for a class
  protected static final Shape INIT_NIL_SHAPE = LAYOUT.createShape(SOBJECT_TYPE);
  public static final DynamicObjectFactory NIL_DUMMY_FACTORY = INIT_NIL_SHAPE.createFactory();

  protected SObject() { } // this class cannot be instantiated, it provides only static helpers

  public static Shape createObjectShapeForClass(final DynamicObject clazz) {
   //return LAYOUT.createShape(SOBJECT_TYPE, clazz);
   return new ShapeBasic(INIT_NIL_SHAPE.getLayout(), 
        clazz, 
        (ShapeImpl) INIT_NIL_SHAPE, 
        INIT_NIL_SHAPE.getObjectType(), 
        ((ShapeImpl) INIT_NIL_SHAPE).getPropertyMap(),
        new Transition.ObjectTypeTransition(INIT_NIL_SHAPE.getObjectType()), 
        LAYOUT.createAllocator(), 
        INIT_NIL_SHAPE.getId());
  }

  public static DynamicObject create(final DynamicObject instanceClass) {
    CompilerAsserts.neverPartOfCompilation("Basic create without factory caching");
    DynamicObjectFactory factory = SClass.getFactory(instanceClass);
    assert factory != NIL_DUMMY_FACTORY;
    //The parameter is only valid for SReflectiveObjects
    return factory.newInstance(Nil.nilObject);
  }
  
  public static DynamicObject createNil() {
    // TODO: this is work in progress, the class should go as shared data into the shape
    // TODO: ideally, nil is like in SOMns an SObjectWithoutFields
    return NIL_DUMMY_FACTORY.newInstance(new Object[] { null });
  }
  
  /**
   * For SObjects, we store the class in the shape's shared data.
   * This makes sure that each class has a separate shape tree and the shapes
   * can be used for field accesses as well as message sends as guards.
   * Without the separation, it could well be that objects from two different
   * classes end up with the same shape, which would mean shapes could not be
   * used as guards for message sends, because it would not be guaranteed that
   * the right message is send/method is activated.
   *
   * Note, the SClasses store their class as a property, to avoid having
   * multiple shapes for each basic classes.
   */
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
    return (DynamicObject) obj.getShape().getSharedData();
  }

  public static final void internalSetNilClass(final DynamicObject obj, final DynamicObject value) {
    assert obj.getShape().getObjectType() == SOBJECT_TYPE;
    CompilerAsserts.neverPartOfCompilation("SObject.setClass");
    assert obj != null;
    assert value != null;

    assert !Universe.current().objectSystemInitialized : "This should really only be used during initialization of object system";

    Shape withoutClass = obj.getShape();
    Shape withClass = withoutClass.createSeparateShape(value);

    obj.setShapeAndGrow(withoutClass, withClass);
  }

  public static final int getFieldIndex(final DynamicObject obj, final SSymbol fieldName) {
    return SClass.lookupFieldIndex(getSOMClass(obj), fieldName);
  }

  public static final int getNumberOfFields(final DynamicObject obj) {
    throw new NotYetImplementedException();
  }
}

