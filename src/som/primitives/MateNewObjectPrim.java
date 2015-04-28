package som.primitives;

import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.MateUniverse;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SClass;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;


@GenerateNodeFactory
public abstract class MateNewObjectPrim extends UnaryExpressionNode {
  @Specialization
  public final SAbstractObject doSClass(final SClass receiver) {
    return MateUniverse.newInstance(receiver);
  }
}

