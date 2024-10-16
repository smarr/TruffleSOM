package trufflesom.interpreter.supernodes.inc;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import trufflesom.bdt.inlining.ScopeAdaptationVisitor;
import trufflesom.bdt.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.NonLocalVariableNode;
import trufflesom.primitives.arithmetic.AdditionPrim;
import trufflesom.primitives.arithmetic.AdditionPrimFactory;


@NodeChild(value = "value", type = ExpressionNode.class)
public abstract class IncNonLocalVarWithExpNode extends NonLocalVariableNode {

  protected IncNonLocalVarWithExpNode(final int contextLevel, final Local local) {
    super(contextLevel, local);
  }

  public abstract ExpressionNode getValue();

  @Specialization(guards = "ctx.isLong(slotIndex)", rewriteOn = {FrameSlotTypeException.class})
  public final long doLong(final VirtualFrame frame, final long value,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    long current = ctx.getLong(slotIndex);
    long result = Math.addExact(current, value);
    ctx.setLong(slotIndex, result);
    return result;
  }

  @Specialization(guards = "ctx.isDouble(slotIndex)",
      rewriteOn = {FrameSlotTypeException.class})
  public final double doDouble(final VirtualFrame frame, final double value,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    double current = ctx.getDouble(slotIndex);
    double result = current + value;
    ctx.setDouble(slotIndex, result);
    return result;
  }

  @Specialization(guards = "ctx.isObject(slotIndex)",
      rewriteOn = {FrameSlotTypeException.class})
  public final Object doString(final VirtualFrame frame, final String value,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    String current = (String) ctx.getObject(slotIndex);
    String result = concat(current, value);
    ctx.setObject(slotIndex, result);
    return result;
  }

  @TruffleBoundary
  private static String concat(final String a, final String b) {
    return a.concat(b);
  }

  @Fallback
  public final Object fallback(final VirtualFrame frame, final Object value) {
    MaterializedFrame ctx = determineContext(frame);
    CompilerDirectives.transferToInterpreterAndInvalidate();

    AdditionPrim add =
        AdditionPrimFactory.create(local.getReadNode(contextLevel, sourceCoord), getValue());
    add.initialize(sourceCoord);

    replace(local.getWriteNode(contextLevel, add, sourceCoord)).adoptChildren();

    Object preIncValue = ctx.getValue(slotIndex);
    Object result = add.executeEvaluated(null, preIncValue, value);
    ctx.setObject(slotIndex, result);
    return result;
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    ScopeElement se = inliner.getAdaptedVar(local);
    if (se.var != local || se.contextLevel < contextLevel) {
      ExpressionNode node;
      if (se.contextLevel == 0) {
        node = IncLocalVarWithExpNodeGen.create((Local) se.var, getValue());
      } else {
        node =
            IncNonLocalVarWithExpNodeGen.create(se.contextLevel, (Local) se.var, getValue());
      }
      node.initialize(sourceCoord);

      replace(node);
    } else {
      assert contextLevel == se.contextLevel;
    }
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder, boolean resultUsed) {
    opBuilder.dsl.beginIncNonLocalVarWithExp(opBuilder.getLocal(local), contextLevel);
    getValue().constructOperation(opBuilder, true);
    opBuilder.dsl.endIncNonLocalVarWithExp();
  }
}
