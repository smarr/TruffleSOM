package trufflesom.interpreter.bc;

import com.oracle.truffle.api.nodes.ControlFlowException;

import trufflesom.interpreter.nodes.AbstractMessageSendNode;


public class RespecializeException extends ControlFlowException {
  private static final long serialVersionUID = 8098665542946983677L;

  public final transient AbstractMessageSendNode send;

  public RespecializeException(final AbstractMessageSendNode send) {
    this.send = send;
  }
}
