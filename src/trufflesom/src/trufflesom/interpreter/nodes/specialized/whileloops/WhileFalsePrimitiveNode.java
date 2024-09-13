package trufflesom.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SObject;


@Proxyable
@GenerateNodeFactory
@Primitive(className = "Block", primitive = "whileFalse:", selector = "whileFalse:",
    receiverType = SBlock.class)
// TODO: need to check for the second argument, check WhileSplzr
public abstract class WhileFalsePrimitiveNode extends WhilePrimitiveNode {

  @Specialization(limit = "INLINE_CACHE_SIZE",
      guards = {"loopCondition.getMethod() == cachedLoopCondition",
          "loopBody.getMethod() == cachedLoopBody"})
  @SuppressWarnings("unused")
  public static final SObject doCached(final SBlock loopCondition, final SBlock loopBody,
      @Cached("loopCondition.getMethod()") final SInvokable cachedLoopCondition,
      @Cached("loopBody.getMethod()") final SInvokable cachedLoopBody,
      @Cached("create(cachedLoopCondition.getCallTarget())") final DirectCallNode conditionNode,
      @Cached("create(cachedLoopBody.getCallTarget())") final DirectCallNode bodyNode,
      @Bind final Node node) {
    return doWhileCached(loopCondition, loopBody, conditionNode, bodyNode, false, node);
  }

  @Specialization(replaces = "doCached")
  public static final SObject doUncached(final SBlock loopCondition, final SBlock loopBody) {
    return doWhileUncachedAndUncounted(loopCondition, loopBody, false);
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder, boolean resultUsed) {
    opBuilder.dsl.beginWhileFalsePrimitive();
    getReceiver().accept(opBuilder);
    getArgument().accept(opBuilder);
    opBuilder.dsl.endWhileFalsePrimitive();
  }
}
