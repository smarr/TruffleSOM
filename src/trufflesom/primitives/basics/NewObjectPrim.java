package som.primitives.basics;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SClass;


@GenerateNodeFactory
@Primitive(className = "Class", primitive = "new")
public abstract class NewObjectPrim extends UnaryExpressionNode {
  @Specialization
  public final SAbstractObject doSClass(final SClass receiver) {
    return Universe.newInstance(receiver);
  }
}
