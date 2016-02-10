package som.primitives;

import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vmobjects.SObject;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.object.basic.DynamicObjectBasic;


@GenerateNodeFactory
@ImportStatic(SObject.class)
public abstract class ObjectSizePrim extends UnaryExpressionNode {
  @Specialization
  public final long doArray(final Object[] receiver) {
    int size = 0;
    size += receiver.length;
    return size;
  }

  @Specialization // (guards = "isSObject(receiver)")
  public final long doSObject(final DynamicObjectBasic receiver) {
    int size = 0;
    size += SObject.getNumberOfFields(receiver);
    return size;
  }

  @Specialization
  public final long doSAbstractObject(final Object receiver) {
    return 0; // TODO: allow polymorphism?
  }
}
