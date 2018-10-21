package trufflesom.primitives;

import java.lang.reflect.InvocationTargetException;

import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.primitives.Primitive.NoChild;
import trufflesom.vm.Universe;
import trufflesom.vm.VmSettings;


/**
 * A Specializer defines when a node can be used as a eager primitive and how
 * it is to be instantiated.
 */
public class Specializer<T> {
  protected final Universe                            universe;
  protected final som.primitives.Primitive            prim;
  protected final NodeFactory<T>                      fact;
  private final NodeFactory<? extends ExpressionNode> extraChildFactory;

  @SuppressWarnings("unchecked")
  public Specializer(final som.primitives.Primitive prim, final NodeFactory<T> fact,
      final Universe universe) {
    this.prim = prim;
    this.fact = fact;
    this.universe = universe;

    if (prim.extraChild() == NoChild.class) {
      extraChildFactory = null;
    } else {
      try {
        extraChildFactory =
            (NodeFactory<? extends ExpressionNode>) prim.extraChild().getMethod("getInstance")
                                                        .invoke(null);
      } catch (IllegalAccessException | IllegalArgumentException
          | InvocationTargetException | NoSuchMethodException
          | SecurityException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public boolean inParser() {
    return prim.inParser() && !prim.requiresArguments();
  }

  public boolean noWrapper() {
    return prim.noWrapper();
  }

  public boolean classSide() {
    return prim.classSide();
  }

  public boolean matches(final Object[] args, final ExpressionNode[] argNodes) {
    if (prim.disabled() && VmSettings.DYNAMIC_METRICS) {
      return false;
    }

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

  private int numberOfNodeConstructorArguments(final ExpressionNode[] argNodes) {
    return argNodes.length + 1 +
        (extraChildFactory != null ? 1 : 0) +
        (prim.requiresArguments() ? 1 : 0) +
        (prim.requiresContext() ? 1 : 0);
  }

  public T create(final Object[] arguments,
      final ExpressionNode[] argNodes, final SourceSection section,
      final boolean eagerWrapper) {
    assert arguments == null || arguments.length == argNodes.length;
    int numArgs = numberOfNodeConstructorArguments(argNodes);

    Object[] ctorArgs = new Object[numArgs];
    ctorArgs[0] = section;
    int offset = 1;

    if (prim.requiresContext()) {
      ctorArgs[offset] = universe;
      offset += 1;
    }

    if (prim.requiresArguments()) {
      assert arguments != null;
      ctorArgs[offset] = arguments;
      offset += 1;
    }

    for (int i = 0; i < argNodes.length; i += 1) {
      ctorArgs[offset] = eagerWrapper ? null : argNodes[i];
      offset += 1;
    }

    if (extraChildFactory != null) {
      ctorArgs[offset] = extraChildFactory.createNode(null, null);
      offset += 1;
    }

    return fact.createNode(ctorArgs);
  }
}
