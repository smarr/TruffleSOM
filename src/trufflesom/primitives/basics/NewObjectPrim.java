package trufflesom.primitives.basics;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.interpreter.objectstorage.ObjectLayout;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SObject;


@GenerateNodeFactory
@Primitive(className = "Class", primitive = "new")
public abstract class NewObjectPrim extends UnaryExpressionNode {
  @Specialization(assumptions = "layout.getAssumption()",
      guards = "layout.layoutForSameClass(receiver)")
  public final SAbstractObject doCached(final SClass receiver,
      @Cached("receiver.getLayoutForInstances()") final ObjectLayout layout) {
    return new SObject(receiver, layout);
  }

  @Specialization(replaces = "doCached")
  public final SAbstractObject doUncached(final SClass receiver) {
    return new SObject(receiver);
  }
}
