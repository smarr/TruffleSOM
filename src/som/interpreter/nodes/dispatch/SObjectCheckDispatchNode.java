package som.interpreter.nodes.dispatch;

import som.vm.constants.ExecutionLevel;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SObject;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.utilities.BranchProfile;


public final class SObjectCheckDispatchNode extends AbstractDispatchNode {

  @Child private AbstractDispatchNode nextInCache;
  @Child private UninitializedDispatchNode uninitializedDispatch;

  private final BranchProfile uninitialized;

  public SObjectCheckDispatchNode(final AbstractDispatchNode nextInCache,
      final UninitializedDispatchNode uninitializedDispatch) {
    this.nextInCache           = nextInCache;
    this.uninitializedDispatch = uninitializedDispatch;
    this.uninitialized         = BranchProfile.create();
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame, final SMateEnvironment environment, final ExecutionLevel exLevel,final Object[] arguments) {
    Object rcvr = arguments[0];
    if (rcvr instanceof SObject) {
      return nextInCache.executeDispatch(frame, environment, exLevel, arguments);
    } else {
      uninitialized.enter();
      return uninitializedDispatch.executeDispatch(frame, environment, exLevel, arguments);
    }
  }

  @Override
  public int lengthOfDispatchChain() {
    return nextInCache.lengthOfDispatchChain();
  }
}
