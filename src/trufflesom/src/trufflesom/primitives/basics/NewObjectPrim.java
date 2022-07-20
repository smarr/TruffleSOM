package trufflesom.primitives.basics;

import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.Source;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.bdt.primitives.nodes.PreevaluatedExpression;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.CachedNewObject;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.interpreter.objectstorage.ObjectLayout;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SObject;


@Proxyable
@GenerateNodeFactory
@Primitive(className = "Class", primitive = "new")
public abstract class NewObjectPrim extends UnaryExpressionNode {
  protected static final int LIMIT = 3;

  @Specialization(assumptions = "layout.getAssumption()",
      guards = "layout.layoutForSameClass(receiver)", limit = "LIMIT")
  public static final SAbstractObject doCached(final SClass receiver,
      @Cached("receiver.getLayoutForInstances()") final ObjectLayout layout) {
    return new SObject(receiver, layout);
  }

  @Specialization(replaces = "doCached")
  public static final SAbstractObject doUncached(final SClass receiver) {
    return new SObject(receiver);
  }

  @Override
  public boolean isTrivial() {
    return true;
  }

  @Override
  public PreevaluatedExpression copyTrivialNode() {
    return NewObjectPrimFactory.create(null);
  }

  @Override
  public AbstractDispatchNode asDispatchNode(final Object rcvr, final Source source,
      final AbstractDispatchNode next) {
    SClass clazz = (SClass) rcvr;
    ObjectLayout layout = clazz.getLayoutForInstances();
    return new CachedNewObject(clazz.getObjectLayout(), layout.getAssumption(), layout, source,
        next);
  }

  @Override
  public void beginConstructOperation(final OpBuilder opBuilder) {
    opBuilder.dsl.beginNewObjectPrim();
  }

  @Override
  public void endConstructOperation(final OpBuilder opBuilder) {
    opBuilder.dsl.endNewObjectPrim();
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder) {
    opBuilder.dsl.beginNewObjectPrim();
    getReceiver().accept(opBuilder);
    opBuilder.dsl.endNewObjectPrim();
  }
}
