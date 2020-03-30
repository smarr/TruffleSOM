package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;


public abstract class BlockDispatchNode extends Node {

  public abstract Object executeDispatch(Object[] arguments);

  protected static final boolean isSameMethod(final Object[] arguments,
      final SInvokable cached) {
    if (!(arguments[0] instanceof SBlock)) {
      return false;
    }
    return getMethod(arguments) == cached;
  }

  protected static final SInvokable getMethod(final Object[] arguments) {
    SInvokable method = ((SBlock) arguments[0]).getMethod();
    assert method.getNumberOfArguments() == arguments.length;
    return method;
  }

  protected final CallTarget getCallTarget(final Object[] arguments) {
    SInvokable blockMethod = getMethod(arguments);
    return blockMethod.getCallTarget();
  }

  protected final DirectCallNode createCallNode(final Object[] arguments) {
    CallTarget ct = getCallTarget(arguments);
    return Truffle.getRuntime().createDirectCallNode(ct);
  }

  protected static final IndirectCallNode createIndirectCall() {
    return Truffle.getRuntime().createIndirectCallNode();
  }

  @Specialization(guards = "isSameMethod(arguments, cached)")
  public Object activateCachedBlock(final Object[] arguments,
      @Cached("getMethod(arguments)") final SInvokable cached,
      @Cached("createCallNode(arguments)") final DirectCallNode call) {
    return call.call(arguments);
  }

  @Specialization(replaces = "activateCachedBlock")
  public Object activateBlock(final Object[] arguments,
      @Cached("createIndirectCall()") final IndirectCallNode indirect) {
    return indirect.call(getCallTarget(arguments), arguments);
  }
}
