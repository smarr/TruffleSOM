package som.primitives.arrays;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.constants.Classes;
import som.vmobjects.SArray;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;


@GenerateNodeFactory
public abstract class NewPrim extends BinaryExpressionNode {

  protected static final boolean receiverIsArrayClass(final DynamicObject receiver) {
    return receiver == Classes.arrayClass;
  }

  @Specialization(guards = "receiverIsArrayClass(receiver)")
  public final SArray doSClass(final DynamicObject receiver, final long length) {
    return new SArray(length);
  }
}
