package trufflesom.primitives.basics;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.primitives.Primitive;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SClass;


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
