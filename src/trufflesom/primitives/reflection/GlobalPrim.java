package trufflesom.primitives.reflection;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.GlobalNode;
import trufflesom.interpreter.nodes.GlobalNode.UninitializedGlobalReadWithoutErrorNode;
import trufflesom.interpreter.nodes.SOMNode;
import trufflesom.primitives.Primitive;
import trufflesom.primitives.basics.SystemPrims.BinarySystemNode;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.Universe;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


@Primitive(className = "System", primitive = "global:", requiresContext = true)
public abstract class GlobalPrim extends BinarySystemNode {
  @Child private GetGlobalNode getGlobal;

  public GlobalPrim(final SourceSection source, final Universe universe) {
    super(source, universe);
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
