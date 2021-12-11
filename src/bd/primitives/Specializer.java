package bd.primitives;

import java.lang.reflect.InvocationTargetException;

import com.oracle.truffle.api.dsl.NodeFactory;

import bd.inlining.nodes.WithSource;
import bd.primitives.Primitive.NoChild;
import bd.settings.VmSettings;


/**
 * A Specializer defines when a node can be used as a eager primitive, how
 * it is to be instantiated, and acts as factory for them.
 *
 * @param <ExprT> the root type of expressions used by the language
 * @param <Id> the type of the identifiers used for mapping to primitives, typically some form
 *          of interned string construct
 */
public class Specializer<ExprT, Id> {
  protected final Primitive          prim;
  protected final NodeFactory<ExprT> fact;

  private final NodeFactory<? extends ExprT> extraChildFactory;

  private final int extraArity;

  @SuppressWarnings("unchecked")
  public Specializer(final Primitive prim, final NodeFactory<ExprT> fact) {
    this.prim = prim;
    this.fact = fact;

    if (prim.extraChild() == NoChild.class) {
      extraChildFactory = null;
      extraArity = 0;
    } else {
      try {
        extraChildFactory =
            (NodeFactory<? extends ExprT>) prim.extraChild().getMethod("getInstance")
                                               .invoke(null);
        extraArity = extraChildFactory.getExecutionSignature().size();
      } catch (IllegalAccessException | IllegalArgumentException
          | InvocationTargetException | NoSuchMethodException
          | SecurityException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public Primitive getPrimitive() {
    return prim;
  }

  public boolean inParser() {
    return prim.inParser() && !prim.requiresArguments();
  }

  public boolean classSide() {
    return prim.classSide();
  }

  public String getName() {
    return fact.getClass().getSimpleName();
  }

  public boolean matches(final Object[] args, final ExprT[] argNodes) {
    // TODO: figure out whether we really want it like this with a VmSetting, or whether
    // there should be something on the context
    assert !(prim.disabled() && VmSettings.DYNAMIC_METRICS);

    if (args == null || prim.receiverType().length == 0) {
      // no constraints, so, it matches
      return true;
    }

    for (Class<?> c : prim.receiverType()) {
      if (c.isInstance(args[0])) {
        return true;
      }
    }
    return false;
  }

  private int numberOfNodeConstructorArguments(final ExprT[] argNodes) {
    int args = argNodes.length;

    return args +
        (extraChildFactory != null ? 1 : 0) +
        (prim.requiresArguments() ? 1 : 0);
  }

  @SuppressWarnings("unchecked")
  public ExprT create(final Object[] arguments, final ExprT[] argNodes,
      final long coord) {
    assert arguments == null || arguments.length >= argNodes.length;
    int numArgs = numberOfNodeConstructorArguments(argNodes);

    Object[] ctorArgs = new Object[numArgs];
    int offset = 0;

    if (prim.requiresArguments()) {
      assert arguments != null;
      ctorArgs[offset] = arguments;
      offset += 1;
    }

    for (int i = 0; i < argNodes.length; i += 1) {
      ctorArgs[offset] = argNodes[i];
      offset += 1;
    }

    if (extraChildFactory != null) {
      Object extraNode = extraChildFactory.createNode(new Object[extraArity]);
      if (extraNode instanceof WithSource) {
        ((WithSource) extraNode).initialize(coord);
      }
      ctorArgs[offset] = extraNode;
      offset += 1;
    }

    ExprT node = fact.createNode(ctorArgs);
    ((WithSource) node).initialize(coord);
    return node;
  }
}
