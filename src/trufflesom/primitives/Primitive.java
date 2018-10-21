package som.primitives;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.oracle.truffle.api.dsl.NodeFactory;


/**
 * Annotation to be applied on node classes that are meant to be used as primitive operations.
 * Thus, the node implements a built-in function of the interpreter, and might need to be
 * represented as a language-level method/function.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(Primitive.Container.class)
public @interface Primitive {

  /** Name of the class, on which the primitive is to be installed. */
  String className() default "";

  /** Name of the selector, for which the primitive is to be installed. */
  String primitive() default "";

  /** Selector for eager replacement. */
  String selector() default "";

  // TODO: allow primitive and selector to be equal for a VM? how do we distinguish then
  // whether they are meant for eager specialization?

  /** Specialize already during parsing. */
  boolean inParser() default true;

  /**
   * Expected type of receiver for eager replacement,
   * if given one of the types needs to match.
   */
  Class<?>[] receiverType() default {};

  /**
   * The specializer is used to check when eager specialization is to be
   * applied and to construct the node.
   */
  @SuppressWarnings("rawtypes")
  Class<? extends Specializer> specializer() default Specializer.class;

  /** A factory for an extra child node that is passed as last argument. */
  @SuppressWarnings("rawtypes")
  Class<? extends NodeFactory> extraChild() default NoChild.class;

  /** Pass array of evaluated arguments to node constructor. */
  boolean requiresArguments() default false;

  /** Pass VM object, i.e., execution context to node constructor. */
  boolean requiresContext() default false;

  /** Disabled for Dynamic Metrics. */
  boolean disabled() default false;

  /**
   * Disable the eager primitive wrapper.
   *
   * This should only be used for nodes that are specifically designed
   * to handle all possible cases themselves.
   */
  boolean noWrapper() default false;

  /**
   * Whether the primitive is to be registered on the class-side of a class, instead of the
   * instance side.
   */
  boolean classSide() default false;

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE})
  public @interface Container {
    Primitive[] value();
  }

  abstract class NoChild implements NodeFactory<Object> {}
}
