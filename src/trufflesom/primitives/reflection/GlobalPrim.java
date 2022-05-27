package trufflesom.primitives.reflection;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.GlobalNode;
import trufflesom.interpreter.nodes.GlobalNode.UninitializedGlobalReadWithoutErrorNode;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.vm.Globals;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


@Primitive(className = "System", primitive = "global:")
@GenerateNodeFactory
public abstract class GlobalPrim extends BinaryExpressionNode {
  @Child private GetGlobalNode getGlobal = new UninitializedGetGlobal(0);

  @Specialization
  public final Object doSObject(final VirtualFrame frame, final SObject receiver,
      final SSymbol argument) {
    return getGlobal.getGlobal(frame, argument);
  }

  private abstract static class GetGlobalNode extends Node {
    protected static final int INLINE_CACHE_SIZE = 6;

    public abstract Object getGlobal(VirtualFrame frame, SSymbol argument);
  }

  private static final class UninitializedGetGlobal extends GetGlobalNode {
    private final int depth;

    UninitializedGetGlobal(final int depth) {
      this.depth = depth;
    }

    @Override
    public Object getGlobal(final VirtualFrame frame, final SSymbol argument) {
      return specialize(argument).getGlobal(frame, argument);
    }

    @TruffleBoundary
    private GetGlobalNode specialize(final SSymbol argument) {
      if (depth < INLINE_CACHE_SIZE) {
        return replace(new CachedGetGlobal(argument, depth));
      } else {
        GetGlobalNode head = this;
        while (head.getParent() instanceof GetGlobalNode) {
          head = (GetGlobalNode) head.getParent();
        }
        return head.replace(new GetGlobalFallback());
      }
    }
  }

  private static final class CachedGetGlobal extends GetGlobalNode {
    private final int            depth;
    private final SSymbol        name;
    @Child private GlobalNode    getGlobal;
    @Child private GetGlobalNode next;

    CachedGetGlobal(final SSymbol name, final int depth) {
      this.depth = depth;
      this.name = name;
      getGlobal = new UninitializedGlobalReadWithoutErrorNode(name);
      next = new UninitializedGetGlobal(this.depth + 1);
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

    @Override
    public Object getGlobal(final VirtualFrame frame, final SSymbol argument) {
      Object result = Globals.getGlobal(argument);
      return result != null ? result : Nil.nilObject;
    }
  }
}
