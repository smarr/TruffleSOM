package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.Node;

import bd.primitives.nodes.PreevaluatedExpression;


public abstract class AbstractDispatchNode extends Node implements PreevaluatedExpression {
  public static final int INLINE_CACHE_SIZE = 6;

  private final DispatchGuard guard;

  @Child public AbstractDispatchNode next;

  protected AbstractDispatchNode(final DispatchGuard guard) {
    this.guard = guard;
  }

  @Override
  public abstract Object doPreEvaluated(VirtualFrame frame, Object[] args);

  public boolean entryMatches(final Object rcvr) throws InvalidAssumptionException {
    return guard.entryMatches(rcvr);
  }

  public final <T extends Node> T insertHere(final T newChild) {
    return super.insert(newChild);
  }

  public static final class CachedDispatchNode extends AbstractDispatchNode {

    @Child protected DirectCallNode cachedMethod;

    public CachedDispatchNode(final DispatchGuard guard, final CallTarget callTarget) {
      super(guard);
      cachedMethod = insert(Truffle.getRuntime().createDirectCallNode(callTarget));
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame, final Object[] arguments) {
      return cachedMethod.call(arguments);
    }
  }

  public static final class CachedExprNode extends AbstractDispatchNode {

    @Child protected PreevaluatedExpression expr;

    public CachedExprNode(final DispatchGuard guard, final PreevaluatedExpression expr) {
      super(guard);
      this.expr = expr;
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame, final Object[] arguments) {
      return expr.doPreEvaluated(frame, arguments);
    }
  }
}
