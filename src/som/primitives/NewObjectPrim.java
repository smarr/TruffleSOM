package som.primitives;

import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;


@GenerateNodeFactory
public abstract class NewObjectPrim extends UnaryExpressionNode {
  @Specialization
  public final DynamicObject doSClass(final DynamicObject receiver) {
    return Universe.newInstance(receiver);
  }
}

