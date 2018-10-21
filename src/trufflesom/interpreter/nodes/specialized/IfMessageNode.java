package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.primitives.Primitive;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;


public abstract class IfMessageNode extends BinaryExpressionNode {

  @GenerateNodeFactory
  @Primitive(selector = "ifTrue:", noWrapper = true)
  public abstract static class IfTrueMessageNode extends IfMessageNode {
    public IfTrueMessageNode(final boolean eagWrap, final SourceSection source) {
      super(true, source);
      assert !eagWrap;
    }
  }

  @GenerateNodeFactory
  @Primitive(selector = "ifFalse:", noWrapper = true)
  public abstract static class IfFalseMessageNode extends IfMessageNode {
    public IfFalseMessageNode(final boolean eagWrap, final SourceSection source) {
      super(false, source);
      assert !eagWrap;
    }
  }

  protected final ConditionProfile condProf = ConditionProfile.createCountingProfile();
  private final boolean            expected;

  protected IfMessageNode(final boolean expected, final SourceSection source) {
    super(source);
    this.expected = expected;
  }

  protected static DirectCallNode createDirect(final SInvokable method) {
    return Truffle.getRuntime().createDirectCallNode(method.getCallTarget());
  }

  protected static IndirectCallNode createIndirect() {
    return Truffle.getRuntime().createIndirectCallNode();
  }

  @Specialization(guards = {"arg.getMethod() == method"})
  public final Object cachedBlock(final boolean rcvr, final SBlock arg,
      @Cached("arg.getMethod()") final SInvokable method,
      @Cached("createDirect(method)") final DirectCallNode callTarget) {
    if (condProf.profile(rcvr == expected)) {
      return callTarget.call(new Object[] {arg});
    } else {
      return Nil.nilObject;
    }
  }

  @Specialization(replaces = "cachedBlock")
  public final Object fallback(final boolean rcvr, final SBlock arg,
      @Cached("createIndirect()") final IndirectCallNode callNode) {
    if (condProf.profile(rcvr == expected)) {
      return callNode.call(arg.getMethod().getCallTarget(), new Object[] {arg});
    } else {
      return Nil.nilObject;
    }
  }

  protected final boolean notABlock(final Object arg) {
    return !(arg instanceof SBlock);
  }

  @Specialization(guards = {"notABlock(arg)"})
  public final Object literal(final boolean rcvr, final Object arg) {
    if (condProf.profile(rcvr == expected)) {
      return arg;
    } else {
      return Nil.nilObject;
    }
  }
}
