package som.primitives.basics;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.primitives.Primitive;
import som.vm.Universe;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SClass;


@GenerateNodeFactory
@Primitive(className = "Class", primitive = "new")
public abstract class NewObjectPrim extends UnaryExpressionNode {
  public NewObjectPrim(final SourceSection source) {
    super(source);
  }

  @Specialization
  public final SAbstractObject doSClass(final SClass receiver) {
    return Universe.newInstance(receiver);
  }
}
