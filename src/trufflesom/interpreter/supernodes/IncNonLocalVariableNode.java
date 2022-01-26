package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import bd.inlining.ScopeAdaptationVisitor;
import bd.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.NonLocalVariableNode;
import trufflesom.primitives.arithmetic.AdditionPrim;
import trufflesom.primitives.arithmetic.AdditionPrimFactory;


@NodeChild(value = "value", type = ExpressionNode.class)
public abstract class IncNonLocalVariableNode extends NonLocalVariableNode {

  protected IncNonLocalVariableNode(final int contextLevel, final Local local) {
    super(contextLevel, local);
  }

  public abstract ExpressionNode getValue();

  @Specialization(guards = "ctx.isLong(slot)", rewriteOn = {FrameSlotTypeException.class})
  public final long doLong(final VirtualFrame frame, final long value,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    long current = ctx.getLong(slot);
    long result = Math.addExact(current, value);
    ctx.setLong(slot, result);
    return result;
  }

  @Specialization(guards = "ctx.isDouble(slot)", rewriteOn = {FrameSlotTypeException.class})
  public final double doDouble(final VirtualFrame frame, final double value,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    double current = ctx.getDouble(slot);
    double result = current + value;
    ctx.setDouble(slot, result);
    return result;
  }

  @Specialization(guards = "ctx.isDouble(slot)", rewriteOn = {FrameSlotTypeException.class})
  public final Object doString(final VirtualFrame frame, final String value,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    String current = (String) ctx.getObject(slot);
    String result = current + value;
    ctx.setObject(slot, result);
    return result;
  }

  @Fallback
  public final Object fallback(final VirtualFrame frame, final Object value) {
    MaterializedFrame ctx = determineContext(frame);
    CompilerDirectives.transferToInterpreterAndInvalidate();

    AdditionPrim add =
        AdditionPrimFactory.create(local.getReadNode(contextLevel, sourceCoord), getValue());
    add.initialize(sourceCoord);

    replace(local.getWriteNode(contextLevel, add, sourceCoord)).adoptChildren();

    Object preIncValue = ctx.getValue(slot);
    Object result = add.executeEvaluated(null, preIncValue, value);
    ctx.setObject(slot, result);
    return result;
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    ScopeElement<? extends Node> se = inliner.getAdaptedVar(local);
    if (se.var != local || se.contextLevel < contextLevel) {
      ExpressionNode node;
      if (se.contextLevel == 0) {
        node = IncLocalVariableNodeGen.create((Local) se.var, getValue());
      } else {
        node = IncNonLocalVariableNodeGen.create(se.contextLevel, (Local) se.var, getValue());
      }
      node.initialize(sourceCoord);

      replace(node);
    } else {
      assert contextLevel == se.contextLevel;
    }
  }
}
