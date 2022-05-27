package trufflesom.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;

import bdt.primitives.Primitive;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SObject;


@GenerateNodeFactory
@Primitive(className = "Block", primitive = "whileFalse:", selector = "whileFalse:",
    receiverType = SBlock.class)
// TODO: need to check for the second argument, check WhileSplzr
public abstract class WhileFalsePrimitiveNode extends WhilePrimitiveNode {
  public WhileFalsePrimitiveNode() {
    super(false);
  }

  @Specialization(limit = "INLINE_CACHE_SIZE",
      guards = {"loopCondition.getMethod() == cachedLoopCondition",
          "loopBody.getMethod() == cachedLoopBody"})
  public final SObject doCached(final SBlock loopCondition, final SBlock loopBody,
      @Cached("loopCondition.getMethod()") final SInvokable cachedLoopCondition,
      @Cached("loopBody.getMethod()") final SInvokable cachedLoopBody,
      @Cached("createCallNode(cachedLoopCondition.getCallTarget())") final DirectCallNode conditionNode,
      @Cached("createCallNode(cachedLoopBody.getCallTarget())") final DirectCallNode bodyNode) {
    return doWhileCached(loopCondition, loopBody, conditionNode, bodyNode);
  }

  @Specialization(replaces = "doCached")
  public final SObject doUncached(final SBlock loopCondition, final SBlock loopBody) {
    return doWhileUncachedAndUncounted(loopCondition, loopBody);
  }
}
