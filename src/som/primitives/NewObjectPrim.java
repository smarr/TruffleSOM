package som.primitives;

import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.constants.Nil;
import som.vmobjects.SClass;
import som.vmobjects.SObject;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.object.basic.DynamicObjectBasic;


@GenerateNodeFactory
@ImportStatic(SClass.class)
public abstract class NewObjectPrim extends UnaryExpressionNode {
  @Specialization(guards = "receiver == cachedClass")
  public final DynamicObjectBasic cachedClass(final DynamicObjectBasic receiver,
      @Cached("receiver") final DynamicObjectBasic cachedClass,
      @Cached("getFactory(cachedClass)") final DynamicObjectFactory factory) {
    //The parameter is only valid for SReflectiveObjects
    return (DynamicObjectBasic) factory.newInstance(Nil.nilObject);
  }

  @TruffleBoundary
  @Specialization(contains = "cachedClass")
  public DynamicObjectBasic uncached(final DynamicObjectBasic receiver) {
    return SObject.create(receiver);
  }
}

