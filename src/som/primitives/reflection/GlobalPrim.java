package som.primitives.reflection;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.GlobalNode;
import som.interpreter.nodes.GlobalNode.UninitializedGlobalReadWithoutErrorNode;
import som.primitives.basics.SystemPrims;
import som.primitives.basics.SystemPrims.BinarySystemNode;
import som.interpreter.nodes.SOMNode;
import som.vm.NotYetImplementedException;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;


public abstract class GlobalPrim extends BinarySystemNode {
  @Child private GetGlobalNode getGlobal;

  public GlobalPrim(final Universe universe) {
    super(universe);
    getGlobal = new UninitializedGetGlobal(0, universe);
  }

  @Specialization(guards = "receiver == universe.getSystemObject()")
  public final Object doSObject(final VirtualFrame frame, final SObject receiver,
      final SSymbol argument) {
    return getGlobal.getGlobal(frame, argument);
  }

  private abstract static class GetGlobalNode extends SOMNode {
    protected static final int INLINE_CACHE_SIZE = 6;

    private GetGlobalNode() {
      super(null);
    }

    public abstract Object getGlobal(VirtualFrame frame, SSymbol argument);

    @Override
    public ExpressionNode getFirstMethodBodyNode() {
      throw new NotYetImplementedException();
    }
  }

  private static final class UninitializedGetGlobal extends GetGlobalNode {
    private final int      depth;
    private final Universe universe;

    UninitializedGetGlobal(final int depth, final Universe universe) {
      this.depth = depth;
      this.universe = universe;
    }

    @Override
    public Object getGlobal(final VirtualFrame frame, final SSymbol argument) {
      return specialize(argument).getGlobal(frame, argument);
    }

    private GetGlobalNode specialize(final SSymbol argument) {
      if (depth < INLINE_CACHE_SIZE) {
        return replace(new CachedGetGlobal(argument, depth, universe));
      } else {
        GetGlobalNode head = this;
        while (head.getParent() instanceof GetGlobalNode) {
          head = (GetGlobalNode) head.getParent();
        }
        return head.replace(new GetGlobalFallback(universe));
      }
    }
  }

  private static final class CachedGetGlobal extends GetGlobalNode {
    private final int            depth;
    private final SSymbol        name;
    @Child private GlobalNode    getGlobal;
    @Child private GetGlobalNode next;

    CachedGetGlobal(final SSymbol name, final int depth, final Universe universe) {
      this.depth = depth;
      this.name = name;
      getGlobal = new UninitializedGlobalReadWithoutErrorNode(name, null, universe);
      next = new UninitializedGetGlobal(this.depth + 1, universe);
    }

    @Override
    public Object getGlobal(final VirtualFrame frame, final SSymbol argument) {
      if (name == argument) {
        return getGlobal.executeGeneric(frame);
      } else {
        return next.getGlobal(frame, argument);
      }
    }
  }

  private static final class GetGlobalFallback extends GetGlobalNode {

    private final Universe universe;

    GetGlobalFallback(final Universe universe) {
      this.universe = universe;
    }

    @Override
    public Object getGlobal(final VirtualFrame frame, final SSymbol argument) {
      Object result = universe.getGlobal(argument);
      return result != null ? result : Nil.nilObject;
    }
  }
}
