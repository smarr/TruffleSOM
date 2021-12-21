package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode.WrapperNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.UnexpectedResultException;


final class GenericMessageSendNodeWrapper extends GenericMessageSendNode
    implements WrapperNode {

  @Child private GenericMessageSendNode delegateNode;
  @Child private ProbeNode              probeNode;

  GenericMessageSendNodeWrapper(final GenericMessageSendNode delegateNode,
      final ProbeNode probeNode) {
    this.delegateNode = delegateNode;
    this.probeNode = probeNode;
  }

  @Override
  public GenericMessageSendNode getDelegateNode() {
    return delegateNode;
  }

  @Override
  public ProbeNode getProbeNode() {
    return probeNode;
  }

  @Override
  public NodeCost getCost() {
    return NodeCost.NONE;
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] arguments) {
    Object returnValue;
    for (;;) {
      boolean wasOnReturnExecuted = false;
      try {
        probeNode.onEnter(frame);
        returnValue = delegateNode.doPreEvaluated(frame, arguments);
        wasOnReturnExecuted = true;
        probeNode.onReturnValue(frame, returnValue);
        break;
      } catch (Throwable t) {
        Object result = probeNode.onReturnExceptionalOrUnwind(frame, t, wasOnReturnExecuted);
        if (result == ProbeNode.UNWIND_ACTION_REENTER) {
          continue;
        } else if (result != null) {
          returnValue = result;
          break;
        }
        throw t;
      }
    }
    return returnValue;
  }

  @Override
  public boolean executeBoolean(final VirtualFrame frame) throws UnexpectedResultException {
    boolean returnValue;
    for (;;) {
      boolean wasOnReturnExecuted = false;
      try {
        try {
          probeNode.onEnter(frame);
          returnValue = delegateNode.executeBoolean(frame);
          wasOnReturnExecuted = true;
          probeNode.onReturnValue(frame, returnValue);
          break;
        } catch (UnexpectedResultException e) {
          wasOnReturnExecuted = true;
          probeNode.onReturnValue(frame, e.getResult());
          throw e;
        }
      } catch (Throwable t) {
        Object result = probeNode.onReturnExceptionalOrUnwind(frame, t, wasOnReturnExecuted);
        if (result == ProbeNode.UNWIND_ACTION_REENTER) {
          continue;
        }
        if (result instanceof Boolean) {
          returnValue = (boolean) result;
          break;
        } else if (result != null) {
          throw new UnexpectedResultException(result);
        }
        throw t;
      }
    }
    return returnValue;
  }

  @Override
  public double executeDouble(final VirtualFrame frame) throws UnexpectedResultException {
    double returnValue;
    for (;;) {
      boolean wasOnReturnExecuted = false;
      try {
        try {
          probeNode.onEnter(frame);
          returnValue = delegateNode.executeDouble(frame);
          wasOnReturnExecuted = true;
          probeNode.onReturnValue(frame, returnValue);
          break;
        } catch (UnexpectedResultException e) {
          wasOnReturnExecuted = true;
          probeNode.onReturnValue(frame, e.getResult());
          throw e;
        }
      } catch (Throwable t) {
        Object result = probeNode.onReturnExceptionalOrUnwind(frame, t, wasOnReturnExecuted);
        if (result == ProbeNode.UNWIND_ACTION_REENTER) {
          continue;
        }
        if (result instanceof Double) {
          returnValue = (double) result;
          break;
        } else if (result != null) {
          throw new UnexpectedResultException(result);
        }
        throw t;
      }
    }
    return returnValue;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object returnValue;
    for (;;) {
      boolean wasOnReturnExecuted = false;
      try {
        probeNode.onEnter(frame);
        returnValue = delegateNode.executeGeneric(frame);
        wasOnReturnExecuted = true;
        probeNode.onReturnValue(frame, returnValue);
        break;
      } catch (Throwable t) {
        Object result = probeNode.onReturnExceptionalOrUnwind(frame, t, wasOnReturnExecuted);
        if (result == ProbeNode.UNWIND_ACTION_REENTER) {
          continue;
        } else if (result != null) {
          returnValue = result;
          break;
        }
        throw t;
      }
    }
    return returnValue;
  }

  @Override
  public long executeLong(final VirtualFrame frame) throws UnexpectedResultException {
    long returnValue;
    for (;;) {
      boolean wasOnReturnExecuted = false;
      try {
        try {
          probeNode.onEnter(frame);
          returnValue = delegateNode.executeLong(frame);
          wasOnReturnExecuted = true;
          probeNode.onReturnValue(frame, returnValue);
          break;
        } catch (UnexpectedResultException e) {
          wasOnReturnExecuted = true;
          probeNode.onReturnValue(frame, e.getResult());
          throw e;
        }
      } catch (Throwable t) {
        Object result = probeNode.onReturnExceptionalOrUnwind(frame, t, wasOnReturnExecuted);
        if (result == ProbeNode.UNWIND_ACTION_REENTER) {
          continue;
        }
        if (result instanceof Long) {
          returnValue = (long) result;
          break;
        } else if (result != null) {
          throw new UnexpectedResultException(result);
        }
        throw t;
      }
    }
    return returnValue;
  }

}
