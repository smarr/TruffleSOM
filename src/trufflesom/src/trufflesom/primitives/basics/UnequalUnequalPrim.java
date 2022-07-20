package trufflesom.primitives.basics;

import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;


@Proxyable
@GenerateNodeFactory
@Primitive(className = "Integer", primitive = "~=")
@Primitive(className = "Double", primitive = "~=")
@Primitive(className = "Object", primitive = "~=")
@Primitive(selector = "~=")
public abstract class UnequalUnequalPrim extends BinaryExpressionNode {
  @Specialization
  public static final boolean doLong(final long left, final long right) {
    return left != right;
  }

  @Specialization
  public static final boolean doDouble(final double left, final double right) {
    return left != right;
  }

  @Fallback
  public static final boolean fallback(final Object receiver, final Object argument) {
    return receiver != argument;
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder) {
    opBuilder.dsl.beginUnequalUnequalPrim();
    getReceiver().accept(opBuilder);
    getArgument().accept(opBuilder);
    opBuilder.dsl.endUnequalUnequalPrim();
  }
}
