package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.vmobjects.SSymbol;


public abstract class BinarySystemMsgOperation extends BinarySystemOperation {
  public abstract SSymbol getSelector();

  @Fallback
  public final Object makeGenericSend(final VirtualFrame frame,
      final Object receiver, final Object argument) {
    return makeGenericSend(getSelector()).doPreEvaluated(frame,
        new Object[] {receiver, argument});
  }
}
