package trufflesom.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.vm.Universe;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SObject;


public abstract class WhileCache extends BinaryExpressionNode {

  public static final int INLINE_CACHE_SIZE = 6;

  protected final boolean predicateBool;

  public WhileCache(final boolean predicateBool, final Universe universe) {
    this.predicateBool = predicateBool;
  }

  @Specialization(limit = "INLINE_CACHE_SIZE",
      guards = {"loopCondition.getMethod() == cachedLoopCondition",
          "loopBody.getMethod() == cachedLoopBody"})
  public final SObject doCached(final SBlock loopCondition, final SBlock loopBody,
      @Cached("loopCondition.getMethod()") final SInvokable cachedLoopCondition,
      @Cached("loopBody.getMethod()") final SInvokable cachedLoopBody,
      @Cached("create(loopCondition, loopBody, predicateBool)") final WhileWithDynamicBlocksNode whileNode) {
    return whileNode.doWhileUnconditionally(loopCondition, loopBody);
  }

  private boolean obj2bool(final Object o) {
    if (o instanceof Boolean) {
      return (boolean) o;
    } else {
      throw new IllegalStateException(
          "This should not happen! There are no objects that are booleans, and we only support booleans.");
    }
  }

  @Specialization(replaces = "doCached")
  @TruffleBoundary
  public final SObject doUncached(final SBlock loopCondition, final SBlock loopBody) {
    // no caching, direct invokes, no loop count reporting...
    CompilerAsserts.neverPartOfCompilation("WhileCache.GenericDispatch");

    Object conditionResult = loopCondition.getMethod().invoke1(loopCondition);

    // TODO: this is a simplification, we don't cover the case receiver isn't a boolean
    boolean loopConditionResult = obj2bool(conditionResult);

    // TODO: this is a simplification, we don't cover the case receiver isn't a boolean
    while (loopConditionResult == predicateBool) {
      loopBody.getMethod().invoke1(loopBody);
      conditionResult = loopCondition.getMethod().invoke1(loopCondition);
      loopConditionResult = obj2bool(conditionResult);
    }
    return Nil.nilObject;
  }
}
