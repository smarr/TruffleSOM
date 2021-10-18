package trufflesom.interpreter;

import java.util.List;

import com.oracle.truffle.api.source.SourceSection;

import trufflesom.compiler.Variable.Argument;
import trufflesom.compiler.Variable.Internal;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgument1ReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgument1WriteNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgument2ReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgument2WriteNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgument3ReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgument3WriteNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgument4ReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgument4WriteNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentWriteNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgument1ReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgument1WriteNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgument2ReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgument2WriteNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgument3ReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgument3WriteNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgument4ReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgument4WriteNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgumentReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgumentWriteNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.FieldNode.UninitFieldIncNode;
import trufflesom.interpreter.nodes.FieldNodeFactory.FieldWriteNodeGen;
import trufflesom.interpreter.nodes.LocalVariableNode.LocalVariableWriteNode;
import trufflesom.interpreter.nodes.LocalVariableNodeFactory.LocalVariableWriteNodeGen;
import trufflesom.interpreter.nodes.ReturnNonLocalNode;
import trufflesom.interpreter.nodes.ReturnNonLocalNode.CatchNonLocalReturnNode;
import trufflesom.interpreter.nodes.SequenceNode;
import trufflesom.interpreter.nodes.specialized.IntIncrementNode;
import trufflesom.vm.Universe;


public final class SNodeFactory {

  public static CatchNonLocalReturnNode createCatchNonLocalReturn(
      final ExpressionNode methodBody, final Internal onStackMarker) {
    return new CatchNonLocalReturnNode(
        methodBody, onStackMarker).initialize(methodBody.getSourceSection());
  }

  public static FieldReadNode createFieldRead(final ExpressionNode self,
      final int fieldIndex, final SourceSection source) {
    return new FieldReadNode(self, fieldIndex).initialize(source);
  }

  public static FieldNode createFieldWrite(final ExpressionNode self,
      final ExpressionNode exp, final int fieldIndex, final SourceSection source) {
    if (exp instanceof IntIncrementNode
        && ((IntIncrementNode) exp).doesAccessField(fieldIndex)) {
      return new UninitFieldIncNode(self, fieldIndex, source);
    }

    return FieldWriteNodeGen.create(fieldIndex, self, exp).initialize(source);
  }

  public static ExpressionNode createArgumentRead(final Argument variable,
      final int contextLevel, final SourceSection source) {
    if (contextLevel == 0) {
      if (variable.index == 0) {
        return new LocalArgument1ReadNode(variable).initialize(source);
      }
      if (variable.index == 1) {
        return new LocalArgument2ReadNode(variable).initialize(source);
      }
      if (variable.index == 2) {
        return new LocalArgument3ReadNode(variable).initialize(source);
      }
      if (variable.index == 3) {
        return new LocalArgument4ReadNode(variable).initialize(source);
      }

      return new LocalArgumentReadNode(variable).initialize(source);
    } else {
      if (variable.index == 0) {
        return new NonLocalArgument1ReadNode(variable, contextLevel).initialize(source);
      }
      if (variable.index == 1) {
        return new NonLocalArgument2ReadNode(variable, contextLevel).initialize(source);
      }
      if (variable.index == 2) {
        return new NonLocalArgument3ReadNode(variable, contextLevel).initialize(source);
      }
      if (variable.index == 3) {
        return new NonLocalArgument4ReadNode(variable, contextLevel).initialize(source);
      }
      return new NonLocalArgumentReadNode(variable, contextLevel).initialize(source);
    }
  }

  public static LocalVariableWriteNode createLocalVariableWrite(
      final Local var, final ExpressionNode exp, final SourceSection source) {
    return LocalVariableWriteNodeGen.create(var, exp).initialize(source);
  }

  public static ExpressionNode createArgumentWrite(final Argument variable,
      final int contextLevel, final ExpressionNode exp, final SourceSection source) {
    if (contextLevel == 0) {
      if (variable.index == 0) {
        return new LocalArgument1WriteNode(variable, exp).initialize(source);
      }
      if (variable.index == 1) {
        return new LocalArgument2WriteNode(variable, exp).initialize(source);
      }
      if (variable.index == 2) {
        return new LocalArgument3WriteNode(variable, exp).initialize(source);
      }
      if (variable.index == 3) {
        return new LocalArgument4WriteNode(variable, exp).initialize(source);
      }
      return new LocalArgumentWriteNode(variable, exp).initialize(source);
    } else {
      if (variable.index == 0) {
        return new NonLocalArgument1WriteNode(variable, contextLevel, exp).initialize(source);
      }
      if (variable.index == 1) {
        return new NonLocalArgument2WriteNode(variable, contextLevel, exp).initialize(source);
      }
      if (variable.index == 2) {
        return new NonLocalArgument3WriteNode(variable, contextLevel, exp).initialize(source);
      }
      if (variable.index == 3) {
        return new NonLocalArgument4WriteNode(variable, contextLevel, exp).initialize(source);
      }
      return new NonLocalArgumentWriteNode(variable, contextLevel, exp).initialize(source);
    }
  }

  public static SequenceNode createSequence(final List<ExpressionNode> exps,
      final SourceSection source) {
    return new SequenceNode(exps.toArray(new ExpressionNode[0])).initialize(source);
  }

  public static ReturnNonLocalNode createNonLocalReturn(final ExpressionNode exp,
      final Internal markerSlot, final int contextLevel,
      final SourceSection source, final Universe universe) {
    return new ReturnNonLocalNode(exp, markerSlot, contextLevel, universe).initialize(source);
  }
}
