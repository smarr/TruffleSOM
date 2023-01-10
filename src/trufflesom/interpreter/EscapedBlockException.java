package trufflesom.interpreter;

import com.oracle.truffle.api.nodes.ControlFlowException;

import trufflesom.vmobjects.SBlock;


public class EscapedBlockException extends ControlFlowException {
  private static final long serialVersionUID = 1124756129738412293L;

  private final transient SBlock block;

  public EscapedBlockException(final SBlock block) {
    this.block = block;
  }

  public SBlock getBlock() {
    return block;
  }
}
