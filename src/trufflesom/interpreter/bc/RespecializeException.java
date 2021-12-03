package trufflesom.interpreter.bc;

import com.oracle.truffle.api.nodes.ControlFlowException;

import trufflesom.interpreter.nodes.MessageSendNode.GenericMessageSendNode;


public class RespecializeException extends ControlFlowException {
  private static final long serialVersionUID = 8098665542946983677L;

  public final GenericMessageSendNode send;

  public RespecializeException(final GenericMessageSendNode send) {
    this.send = send;
  }
}
