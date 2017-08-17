package som.primitives.reflection;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Primitive;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.GlobalNode;
import som.interpreter.nodes.GlobalNode.UninitializedGlobalReadWithoutErrorNode;
import som.interpreter.nodes.SOMNode;
import som.interpreter.nodes.nary.BinaryExpressionNode.BinarySystemOperation;
import som.vm.NotYetImplementedException;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;


@Primitive(className = "System", primitive = "global:")
public abstract class GlobalPrim extends BinarySystemOperation {
  @Child private GetGlobalNode getGlobal;

  @Override
  public BinarySystemOperation initialize(final Universe universe) {
    super.initialize(universe);
    getGlobal = new UninitializedGetGlobal(0, universe).initialize(sourceSection);
    return this;
  }

  @Specialization(guards = "receiver == universe.getSystemObject()")
  public final Object doSObject(final VirtualFrame frame, final SObject receiver,
      final SSymbol argument) {
    return getGlobal.getGlobal(frame, argument);
  }

  private abstract static class GetGlobalNode extends SOMNode {
    protected static final int INLINE_CACHE_SIZE = 6;

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
        return replace(new CachedGetGlobal(argument, depth, sourceSection, universe));
      } else {
        GetGlobalNode head = this;
        while (head.getParent() instanceof GetGlobalNode) {
          head = (GetGlobalNode) head.getParent();
        }
        return head.replace(new GetGlobalFallback(universe).initialize(sourceSection));
      }
    }
  }

  private static final class CachedGetGlobal extends GetGlobalNode {
    private final int            depth;
    private final SSymbol        name;
    @Child private GlobalNode    getGlobal;
    @Child private GetGlobalNode next;

    CachedGetGlobal(final SSymbol name, final int depth, final SourceSection source,
        final Universe universe) {
      this.depth = depth;
      this.name = name;
      getGlobal = new UninitializedGlobalReadWithoutErrorNode(name, source, universe);
      next = new UninitializedGetGlobal(this.depth + 1, universe).initialize(source);
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
