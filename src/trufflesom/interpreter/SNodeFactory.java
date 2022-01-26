package trufflesom.interpreter;

import java.util.List;

import trufflesom.compiler.Variable.Argument;
import trufflesom.compiler.Variable.Internal;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentWriteNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgumentReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgumentWriteNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.FieldNodeFactory.FieldWriteNodeGen;
import trufflesom.interpreter.nodes.ReturnNonLocalNode;
import trufflesom.interpreter.nodes.ReturnNonLocalNode.CatchNonLocalReturnNode;
import trufflesom.interpreter.nodes.SequenceNode;
import trufflesom.interpreter.supernodes.IntIncrementNode;
import trufflesom.interpreter.supernodes.UninitIncFieldNode;
import trufflesom.primitives.arithmetic.AdditionPrim;


public final class SNodeFactory {

  public static CatchNonLocalReturnNode createCatchNonLocalReturn(
      final ExpressionNode methodBody, final Internal onStackMarker) {
    return new CatchNonLocalReturnNode(
        methodBody, onStackMarker).initialize(methodBody.getSourceCoordinate());
  }

  public static FieldReadNode createFieldRead(final ExpressionNode self,
      final int fieldIndex, final long coord) {
    return new FieldReadNode(self, fieldIndex).initialize(coord);
  }

  public static FieldNode createFieldWrite(final ExpressionNode self,
      final ExpressionNode exp, final int fieldIndex, final long coord) {
    assert coord != 0;
    if (exp instanceof IntIncrementNode
        && ((IntIncrementNode) exp).doesAccessField(fieldIndex)) {
      return ((IntIncrementNode) exp).createFieldIncNode(self, fieldIndex, coord);
    }

    if (exp instanceof AdditionPrim) {
      AdditionPrim add = (AdditionPrim) exp;
      ExpressionNode rcvr = add.getReceiver();
      ExpressionNode arg = add.getArgument();

      if (rcvr instanceof FieldReadNode
          && fieldIndex == ((FieldReadNode) rcvr).getFieldIndex()) {
        return new UninitIncFieldNode(self, arg, true, fieldIndex, coord);
      }
      if (arg instanceof FieldReadNode
          && fieldIndex == ((FieldReadNode) arg).getFieldIndex()) {
        return new UninitIncFieldNode(self, rcvr, false, fieldIndex, coord);
      }
    }

    return FieldWriteNodeGen.create(fieldIndex, self, exp).initialize(coord);
  }

  public static ExpressionNode createArgumentRead(final Argument variable,
      final int contextLevel, final long coord) {
    if (contextLevel == 0) {
      return new LocalArgumentReadNode(variable).initialize(coord);
    } else {
      return new NonLocalArgumentReadNode(variable, contextLevel).initialize(coord);
    }
  }

  public static ExpressionNode createArgumentWrite(final Argument variable,
      final int contextLevel, final ExpressionNode exp, final long coord) {
    if (contextLevel == 0) {
      return new LocalArgumentWriteNode(variable, exp).initialize(coord);
    } else {
      return new NonLocalArgumentWriteNode(variable, contextLevel, exp).initialize(coord);
    }
  }

  public static SequenceNode createSequence(final List<ExpressionNode> exps,
      final long coord) {
    return new SequenceNode(exps.toArray(new ExpressionNode[0])).initialize(coord);
  }

  public static ReturnNonLocalNode createNonLocalReturn(final ExpressionNode exp,
      final Internal markerSlot, final int contextLevel, final long coord) {
    return new ReturnNonLocalNode(exp, markerSlot, contextLevel).initialize(coord);
  }
}
