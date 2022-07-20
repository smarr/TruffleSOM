package trufflesom.interpreter.operations.copied;

import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.InlinedCountingConditionProfile;

import trufflesom.interpreter.nodes.nary.TernaryExpressionNode;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;


@Proxyable
@ImportStatic(ConditionProfile.class)
public abstract class IfMessageOp extends TernaryExpressionNode {
  public static int LIMIT = 3;

  public static final boolean notABlock(final Object arg) {
    return !(arg instanceof SBlock);
  }

  @Specialization(guards = {"arg.getMethod() == method"}, limit = "LIMIT")
  public static final Object cachedBlock(final boolean rcvr, final SBlock arg,
      final boolean expected,
      @SuppressWarnings("unused") @Cached("arg.getMethod()") final SInvokable method,
      @Cached("create(method.getCallTarget())") final DirectCallNode callTarget,
      @Shared("profile") @Cached final InlinedCountingConditionProfile condProf,
      @Bind("this") final Node self) {
    if (condProf.profile(self, rcvr == expected)) {
      return callTarget.call(new Object[] {arg});
    } else {
      return Nil.nilObject;
    }
  }

  @Specialization(replaces = "cachedBlock")
  public static final Object fallback(final boolean rcvr, final SBlock arg,
      final boolean expected,
      @Cached final IndirectCallNode callNode,
      @Shared("profile") @Cached final InlinedCountingConditionProfile condProf,
      @Bind("this") final Node self) {
    if (condProf.profile(self, rcvr == expected)) {
      return callNode.call(arg.getMethod().getCallTarget(), new Object[] {arg});
    } else {
      return Nil.nilObject;
    }
  }

  @Specialization(guards = {"notABlock(arg)"})
  public static final Object literal(final boolean rcvr, final Object arg,
      final boolean expected,
      @Shared("profile") @Cached final InlinedCountingConditionProfile condProf,
      @Bind("this") final Node self) {
    if (condProf.profile(self, rcvr == expected)) {
      return arg;
    } else {
      return Nil.nilObject;
    }
  }
}
