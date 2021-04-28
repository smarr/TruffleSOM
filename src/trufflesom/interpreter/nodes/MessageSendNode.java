package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Specializer;
import bd.primitives.nodes.PreevaluatedExpression;
import bd.tools.nodes.Invocation;
import trufflesom.interpreter.Types;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode.CachedDispatchNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode.CachedExprNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode.GuardedDispatchNode;
import trufflesom.interpreter.nodes.dispatch.CachedDnuNode;
import trufflesom.interpreter.nodes.dispatch.DispatchGuard;
import trufflesom.interpreter.nodes.dispatch.GenericDispatchNode;
import trufflesom.interpreter.nodes.literals.IntegerLiteralNode;
import trufflesom.interpreter.nodes.nary.EagerlySpecializableNode;
import trufflesom.interpreter.nodes.specialized.IntIncrementNodeGen;
import trufflesom.primitives.Primitives;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public final class MessageSendNode {

  public static ExpressionNode create(final SSymbol selector,
      final ExpressionNode[] arguments, final SourceSection source, final Universe universe) {
    if (selector.getString().charAt(0) == '+') {
      if (arguments[1] instanceof IntegerLiteralNode) {
        IntegerLiteralNode lit = (IntegerLiteralNode) arguments[1];
        if (lit.executeLong(null) == 1) {
          return IntIncrementNodeGen.create(arguments[0]);
        }
      }
    }

    Primitives prims = universe.getPrimitives();
    Specializer<Universe, ExpressionNode, SSymbol> specializer =
        prims.getParserSpecializer(selector, arguments);
    if (specializer == null) {
      return new GenericMessageSendNode(selector, arguments, universe).initialize(source);
    }

    EagerlySpecializableNode newNode = (EagerlySpecializableNode) specializer.create(null,
        arguments, source, !specializer.noWrapper(), universe);

    if (specializer.noWrapper()) {
      return newNode;
    } else {
      return newNode.wrapInEagerWrapper(selector, arguments, universe);
    }
  }

  private static final ExpressionNode[] NO_ARGS = new ExpressionNode[0];

  public static AbstractMessageSendNode createForPerformNodes(final SSymbol selector,
      final SourceSection source, final Universe universe) {
    return new GenericMessageSendNode(selector, NO_ARGS, universe).initialize(source);
  }

  public static GenericMessageSendNode createGeneric(final SSymbol selector,
      final ExpressionNode[] argumentNodes, final SourceSection source,
      final Universe universe) {
    return new GenericMessageSendNode(
        selector, argumentNodes, universe, true).initialize(source);
  }

  public static AbstractMessageSendNode createSuperSend(final SClass superClass,
      final SSymbol selector, final ExpressionNode[] arguments, final SourceSection source) {
    SInvokable method = superClass.lookupInvokable(selector);

    if (method == null) {
      throw new NotYetImplementedException(
          "Currently #dnu with super sent is not yet implemented. ");
    }

    if (method.isTrivial()) {
      PreevaluatedExpression node = method.copyTrivialNode();
      return new SuperExprNode(selector, arguments, node).initialize(source);
    }

    DirectCallNode superMethodNode = Truffle.getRuntime().createDirectCallNode(
        method.getCallTarget());

    return new SuperSendNode(selector, arguments, superMethodNode).initialize(source);
  }

  public abstract static class AbstractMessageSendNode extends ExpressionNode
      implements PreevaluatedExpression, Invocation<SSymbol> {

    @Children protected final ExpressionNode[] argumentNodes;

    protected AbstractMessageSendNode(final ExpressionNode[] arguments) {
      this.argumentNodes = arguments;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      Object[] arguments = evaluateArguments(frame);
      return doPreEvaluated(frame, arguments);
    }

    @ExplodeLoop
    private Object[] evaluateArguments(final VirtualFrame frame) {
      Object[] arguments = new Object[argumentNodes.length];
      for (int i = 0; i < argumentNodes.length; i++) {
        arguments[i] = argumentNodes[i].executeGeneric(frame);
        assert arguments[i] != null;
      }
      return arguments;
    }
  }

  public static final class GenericMessageSendNode
      extends AbstractMessageSendNode {

    private final SSymbol  selector;
    private final Universe universe;

    @CompilationFinal private boolean triedEager;

    @Child private GuardedDispatchNode dispatchCache;

    private GenericMessageSendNode(final SSymbol selector, final ExpressionNode[] arguments,
        final Universe universe, final boolean triedEager) {
      super(arguments);
      this.selector = selector;
      this.universe = universe;
      this.triedEager = triedEager;
    }

    private GenericMessageSendNode(final SSymbol selector, final ExpressionNode[] arguments,
        final Universe universe) {
      this(selector, arguments, universe, false);
    }

    @Override
    @ExplodeLoop
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {

      GuardedDispatchNode cache = dispatchCache;

      Object rcvr = arguments[0];

      while (cache != null) {
        try {
          if (cache.entryMatches(rcvr)) {
            return cache.doPreEvaluated(frame, arguments);
          }
        } catch (InvalidAssumptionException e) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          cache = removeInvalidEntryAndReturnNext(cache);
          continue;
        }
        cache = cache.next;
      }

      CompilerDirectives.transferToInterpreterAndInvalidate();

      return specialize(arguments).doPreEvaluated(frame, arguments);
    }

    private GuardedDispatchNode removeInvalidEntryAndReturnNext(
        final GuardedDispatchNode cache) {
      if (cache.getParent() == this) {
        if (cache.next == null) {
          dispatchCache = null;
          return null;
        } else {
          dispatchCache = insert(cache.next);
          return cache.next;
        }
      } else {
        GuardedDispatchNode parent = (GuardedDispatchNode) cache.getParent();
        if (cache.next == null) {
          parent.next = null;
          return null;
        } else {
          parent.next = parent.insertHere(cache.next);
          return cache.next;
        }
      }
    }

    @Override
    public String toString() {
      return "GMsgSend(" + selector.getString() + ")";
    }

    private int getCacheSize(GuardedDispatchNode cache) {
      int cacheSize = 0;
      while (cache != null) {
        cache = cache.next;
        cacheSize += 1;
      }

      return cacheSize;
    }

    @Override
    public NodeCost getCost() {
      if (!triedEager) {
        return NodeCost.UNINITIALIZED;
      }

      GuardedDispatchNode cache = dispatchCache;

      if (cache instanceof GenericDispatchNode) {
        return NodeCost.MEGAMORPHIC;
      }

      int cacheSize = getCacheSize(cache);

      if (cacheSize == 0) {
        return NodeCost.UNINITIALIZED;
      }

      if (cacheSize == 1) {
        return NodeCost.MONOMORPHIC;
      }

      return NodeCost.POLYMORPHIC;
    }

    private PreevaluatedExpression specialize(final Object[] arguments) {
      if (!triedEager) {
        triedEager = true;
        PreevaluatedExpression eager = attemptEagerSpecialization(arguments);
        if (eager != null) {
          return eager;
        }
      }

      final GuardedDispatchNode first = dispatchCache;

      int cacheSize = getCacheSize(first);

      Object rcvr = arguments[0];
      assert rcvr != null;

      if (rcvr instanceof SObject) {
        SObject r = (SObject) rcvr;
        if (r.updateLayoutToMatchClass() && first != null) {
          // if the dispatchCache is null, we end up here, so continue directly below instead
          // otherwise, let's retry the cache!
          return this;
        }
      }

      if (cacheSize < AbstractDispatchNode.INLINE_CACHE_SIZE) {
        SClass rcvrClass = Types.getClassOf(rcvr, universe);
        SInvokable method = rcvrClass.lookupInvokable(selector);
        CallTarget callTarget = null;
        PreevaluatedExpression expr = null;
        if (method != null) {
          if (method.isTrivial()) {
            expr = method.copyTrivialNode();
            assert expr != null;
          } else {
            callTarget = method.getCallTarget();
          }
        }

        DispatchGuard guard = DispatchGuard.create(rcvr);

        GuardedDispatchNode node;
        if (expr != null) {
          node = new CachedExprNode(guard, expr);
        } else if (method != null) {
          node = new CachedDispatchNode(guard, callTarget);
        } else {
          node = new CachedDnuNode(rcvrClass, guard, selector, universe);
        }

        if (first != null) {
          reportPolymorphicSpecialize();
          node.next = node.insertHere(first);
        }
        dispatchCache = insert(node);
        return node;
      }

      // the chain is longer than the maximum defined by INLINE_CACHE_SIZE and
      // thus, this callsite is considered to be megaprophic, and we generalize it.
      GenericDispatchNode generic = new GenericDispatchNode(selector, universe);
      dispatchCache = insert(generic);
      reportPolymorphicSpecialize();
      return generic;
    }

    private PreevaluatedExpression attemptEagerSpecialization(final Object[] arguments) {
      Primitives prims = universe.getPrimitives();

      Specializer<Universe, ExpressionNode, SSymbol> specializer =
          prims.getEagerSpecializer(selector, arguments, argumentNodes);

      if (specializer != null) {
        boolean noWrapper = specializer.noWrapper();
        EagerlySpecializableNode newNode =
            (EagerlySpecializableNode) specializer.create(arguments, argumentNodes,
                sourceSection, !noWrapper, universe);
        if (noWrapper) {
          return replace(newNode);
        } else {
          return makeEagerPrim(newNode);
        }
      }
      return null;
    }

    private PreevaluatedExpression makeEagerPrim(final EagerlySpecializableNode prim) {
      assert prim.getSourceSection() != null;

      PreevaluatedExpression result = (PreevaluatedExpression) replace(
          prim.wrapInEagerWrapper(selector, argumentNodes, universe));

      return result;
    }

    @Override
    public SSymbol getInvocationIdentifier() {
      return selector;
    }
  }

  public static final class SuperSendNode extends AbstractMessageSendNode {
    private final SSymbol selector;

    @Child private DirectCallNode cachedSuperMethod;

    private SuperSendNode(final SSymbol selector, final ExpressionNode[] arguments,
        final DirectCallNode superMethod) {
      super(arguments);
      this.selector = selector;
      this.cachedSuperMethod = superMethod;
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return cachedSuperMethod.call(arguments);
    }

    @Override
    public SSymbol getInvocationIdentifier() {
      return selector;
    }

    @Override
    public String toString() {
      return "SuperSend(" + selector.getString() + ")";
    }
  }

  private static final class SuperExprNode extends AbstractMessageSendNode {
    private final SSymbol                 selector;
    @Child private PreevaluatedExpression expr;

    private SuperExprNode(final SSymbol selector, final ExpressionNode[] arguments,
        final PreevaluatedExpression expr) {
      super(arguments);
      this.selector = selector;
      this.expr = expr;
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return expr.doPreEvaluated(frame, arguments);
    }

    @Override
    public SSymbol getInvocationIdentifier() {
      return selector;
    }

    @Override
    public String toString() {
      return "SendExpr(" + selector.getString() + ")";
    }
  }
}
