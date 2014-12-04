package som.interpreter;

import java.util.List;

import som.compiler.Variable.Argument;
import som.compiler.Variable.Local;
import som.interpreter.nodes.ArgumentReadNode.NonLocalArgumentReadNode;
import som.interpreter.nodes.ArgumentReadNode.NonLocalSuperReadNode;
import som.interpreter.nodes.ContextualNode;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.FieldNode.FieldReadNode;
import som.interpreter.nodes.FieldNode.FieldWriteNode;
import som.interpreter.nodes.FieldNodeFactory.FieldReadNodeFactory;
import som.interpreter.nodes.FieldNodeFactory.FieldWriteNodeFactory;
import som.interpreter.nodes.GlobalNode;
import som.interpreter.nodes.MessageSendNode;
import som.interpreter.nodes.ReturnNonLocalNode;
import som.interpreter.nodes.SequenceNode;
import som.interpreter.nodes.UninitializedVariableNode.UninitializedVariableReadNode;
import som.interpreter.nodes.UninitializedVariableNode.UninitializedVariableWriteNode;
import som.interpreter.nodes.literals.BlockNode.BlockNodeWithContext;
import som.vm.Universe;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.source.SourceSection;



public final class SNodeFactory {

  public static FieldReadNode createFieldRead(final ExpressionNode self,
      final int fieldIndex, final SourceSection source) {
    return FieldReadNodeFactory.create(fieldIndex, source, self);
  }

  public static GlobalNode createGlobalRead(final String name,
      final Universe universe, final SourceSection source) {
    return createGlobalRead(universe.symbolFor(name), universe, source);
  }
  public static GlobalNode createGlobalRead(final SSymbol name,
      final Universe universe, final SourceSection source) {
    return new GlobalNode(name, source);
  }

  public static FieldWriteNode createFieldWrite(final ExpressionNode self,
      final ExpressionNode exp, final int fieldIndex, final SourceSection source) {
    return FieldWriteNodeFactory.create(fieldIndex, source, self, exp);
  }

  public static ContextualNode createLocalVarRead(final Local variable,
      final int contextLevel, final SourceSection source) {
    return new UninitializedVariableReadNode(variable, contextLevel, source);
  }

  public static ExpressionNode createArgumentRead(final Argument variable,
      final int contextLevel, final SourceSection source) {
    return new NonLocalArgumentReadNode(variable.index, contextLevel, source);
  }

  public static ExpressionNode createSuperRead(final int contextLevel,
        final SSymbol holderClass, final boolean classSide, final SourceSection source) {
    return new NonLocalSuperReadNode(contextLevel, holderClass, classSide, source);
  }

  public static ContextualNode createVariableWrite(final Local variable,
        final int contextLevel,
        final ExpressionNode exp, final SourceSection source) {
    return new UninitializedVariableWriteNode(variable, contextLevel, exp, source);
  }

  public static SequenceNode createSequence(final List<ExpressionNode> exps,
      final SourceSection source) {
    return new SequenceNode(exps.toArray(new ExpressionNode[0]), source);
  }

  public static BlockNodeWithContext createBlockNode(final SMethod blockMethod,
      final SourceSection source) {
    return new BlockNodeWithContext(blockMethod, source);
  }

  public static MessageSendNode createMessageSend(final SSymbol msg,
      final ExpressionNode[] exprs, final SourceSection source) {
    if (exprs[0] instanceof NonLocalSuperReadNode) {
      return MessageSendNode.createSuper(msg, exprs, source);
    } else {
      return MessageSendNode.createGeneric(msg, exprs, source);
    }
  }

  public static ExpressionNode createMessageSend(final SSymbol msg,
      final List<ExpressionNode> exprs, final SourceSection source) {
    return createMessageSend(msg, exprs.toArray(new ExpressionNode[0]), source);
  }

  public static ReturnNonLocalNode createNonLocalReturn(final ExpressionNode exp,
      final FrameSlot markerSlot, final int contextLevel,
      final SourceSection source) {
    return new ReturnNonLocalNode(exp, markerSlot, contextLevel, source);
  }
}
