package trufflesom.primitives.reflection;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.vmobjects.SObject;


@GenerateNodeFactory
@Primitive(className = "Object", primitive = "objectSize")
public abstract class ObjectSizePrim extends UnaryExpressionNode {
  @Specialization
  public final long doArray(final Object[] receiver) {
    int size = 0;
    size += receiver.length;
    return size;
  }

  @Specialization
  public final long doSObject(final SObject receiver) {
    int size = 0;
    size += receiver.getNumberOfFields();
    return size;
  }

  @Specialization
  public final long doSAbstractObject(final Object receiver) {
    return 0; // TODO: allow polymorphism?
  }
}
