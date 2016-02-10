package som.primitives.reflection;

import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.object.basic.DynamicObjectBasic;


@GenerateNodeFactory
public abstract class PerformInSuperclassPrim extends TernaryExpressionNode {
  @Child private IndirectCallNode call = Truffle.getRuntime().createIndirectCallNode();

  @Specialization
  public final Object doSAbstractObject(final VirtualFrame frame,
      final Object receiver, final SSymbol selector, final DynamicObjectBasic clazz) {
    CompilerAsserts.neverPartOfCompilation("PerformInSuperclassPrim");
    SInvokable invokable = SClass.lookupInvokable(clazz, selector);
    return call.call(frame, invokable.getCallTarget(), new Object[] {receiver});
  }
}
