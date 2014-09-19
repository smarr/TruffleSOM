package som.primitives;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.SOMNode;
import som.primitives.SystemPrims.BinarySystemNode;
import som.vm.NotYetImplementedException;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class GlobalPrim extends BinarySystemNode {
  @Child private GetGlobalFallback getGlobal = new GetGlobalFallback();

  @Specialization(guards = "receiverIsSystemObject")
  public final Object doSObject(final VirtualFrame frame, final SObject receiver, final SSymbol argument) {
    return getGlobal.getGlobal(frame, argument);
  }

  private static final class GetGlobalFallback extends SOMNode {

    private final Universe universe;

    public GetGlobalFallback() {
      super(null);
      this.universe = Universe.current();
    }

    public Object getGlobal(final VirtualFrame frame, final SSymbol argument) {
      Object result = universe.getGlobal(argument);
      return result != null ? result : Nil.nilObject;
    }

    @Override
    public ExpressionNode getFirstMethodBodyNode() {
      throw new NotYetImplementedException();
    }
  }
}
