package som.primitives;

import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.constants.Nil;
import som.vmobjects.SClass;
import som.vmobjects.SObject;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;


@GenerateNodeFactory
@ImportStatic(SClass.class)
public abstract class NewObjectPrim extends UnaryExpressionNode {
  @Specialization(guards = "receiver == cachedClass")
  public final DynamicObject cachedClass(final DynamicObject receiver,
      @Cached("receiver") final DynamicObject cachedClass,
      @Cached("getFactory(cachedClass)") final DynamicObjectFactory factory) {
    //The parameter is only valid for SReflectiveObjects
    return factory.newInstance(Nil.nilObject);
  }

  @Specialization(contains = "cachedClass")
  public DynamicObject uncached(final DynamicObject receiver) {
    return SObject.create(receiver);
  }
}

