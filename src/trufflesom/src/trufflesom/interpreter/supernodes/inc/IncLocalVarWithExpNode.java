package trufflesom.interpreter.supernodes.inc;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import trufflesom.bdt.inlining.ScopeAdaptationVisitor;
import trufflesom.bdt.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.LocalVariableNode;


@NodeChild(value = "value", type = ExpressionNode.class)
public abstract class IncLocalVarWithExpNode extends LocalVariableNode {
  public IncLocalVarWithExpNode(final Local variable) {
    super(variable);
  }

  public abstract ExpressionNode getValue();

  @Specialization(guards = "frame.isLong(slotIndex)",
      rewriteOn = {FrameSlotTypeException.class})
  public final long doLong(final VirtualFrame frame, final long value)
      throws FrameSlotTypeException {
    long current = frame.getLong(slotIndex);
    long result = Math.addExact(current, value);
    frame.setLong(slotIndex, result);
    return result;
  }

  @Specialization(guards = "frame.isDouble(slotIndex)",
      rewriteOn = {FrameSlotTypeException.class})
  public final double doDouble(final VirtualFrame frame, final double value)
      throws FrameSlotTypeException {
    double current = frame.getDouble(slotIndex);
    double result = current + value;
    frame.setDouble(slotIndex, result);
    return result;
  }

  @Specialization(guards = "frame.isObject(slotIndex)",
      rewriteOn = {FrameSlotTypeException.class})
  public final Object doString(final VirtualFrame frame, final String value)
      throws FrameSlotTypeException {
    String current = (String) frame.getObject(slotIndex);
    String result = concat(current, value);
    frame.setObject(slotIndex, result);
    return result;
  }

  @TruffleBoundary
  private static String concat(final String a, final String b) {
    return a.concat(b);
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    ScopeElement se = inliner.getAdaptedVar(local);
    if (se.var != local || se.contextLevel < 0) {
      IncLocalVarWithExpNode newNode =
          IncLocalVarWithExpNodeGen.create((Local) se.var, getValue());
      newNode.initialize(sourceCoord);
      replace(newNode);
    } else {
      assert 0 == se.contextLevel;
    }
  }

}
