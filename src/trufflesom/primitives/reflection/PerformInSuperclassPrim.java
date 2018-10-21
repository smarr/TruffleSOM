package som.primitives.reflection;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.primitives.Primitive;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Object", primitive = "perform:inSuperclass:")
public abstract class PerformInSuperclassPrim extends TernaryExpressionNode {
  public PerformInSuperclassPrim(final SourceSection sourceSection) {
    super(sourceSection);
  }

  @Child private IndirectCallNode call = Truffle.getRuntime().createIndirectCallNode();

  @Specialization
  public final Object doSAbstractObject(final SAbstractObject receiver, final SSymbol selector,
      final SClass clazz) {
    CompilerAsserts.neverPartOfCompilation("PerformInSuperclassPrim");
    SInvokable invokable = clazz.lookupInvokable(selector);
    return call.call(invokable.getCallTarget(), new Object[] {receiver});
  }
}
