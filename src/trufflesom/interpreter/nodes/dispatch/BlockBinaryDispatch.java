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


public abstract class BlockBinaryDispatch extends Node {

  public abstract Object executeDispatch(Object rcvr, Object arg);

  protected static final SInvokable getMethod(final SBlock rcvr) {
    SInvokable method = rcvr.getMethod();
    assert method.getNumberOfArguments() == 2;
    return method;
  }

  protected static final CallTarget getCallTarget(final SBlock rcvr) {
    SInvokable blockMethod = getMethod(rcvr);
    return blockMethod.getCallTarget();
  }

  protected static final DirectCallNode createCallNode(final SBlock rcvr) {
    CallTarget ct = getCallTarget(rcvr);
    return Truffle.getRuntime().createDirectCallNode(ct);
  }

  protected static final IndirectCallNode createIndirectCall() {
    return Truffle.getRuntime().createIndirectCallNode();
  }

  @Specialization(guards = "rcvr.getMethod() == cached")
  public Object activateCachedBlock(final SBlock rcvr, final Object arg,
      @Cached("rcvr.getMethod()") final SInvokable cached,
      @Cached("createCallNode(rcvr)") final DirectCallNode call) {
    return call.call2(rcvr, arg);
  }

  @Specialization(replaces = "activateCachedBlock")
  public Object activateBlock(final SBlock rcvr, final Object arg,
      @Cached("createIndirectCall()") final IndirectCallNode indirect) {
    return indirect.call2(rcvr.getMethod().getCallTarget(), rcvr, arg);
  }
}
