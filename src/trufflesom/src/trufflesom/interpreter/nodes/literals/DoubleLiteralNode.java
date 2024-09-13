package trufflesom.interpreter.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.bdt.inlining.ScopeAdaptationVisitor;
import trufflesom.compiler.Parser.ParseError;
import trufflesom.compiler.bc.BytecodeGenerator;
import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.Method.OpBuilder;


public final class DoubleLiteralNode extends LiteralNode {
  private final double value;

  public DoubleLiteralNode(final double value) {
    this.value = value;
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] arguments) {
    return value;
  }

  @Override
  public double executeDouble(final VirtualFrame frame) {
    return value;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    return value;
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    Object scope = inliner.getCurrentScope();

    if (scope instanceof BytecodeMethodGenContext) {
      BytecodeMethodGenContext mgenc = (BytecodeMethodGenContext) scope;
      try {
        BytecodeGenerator.emitPUSHCONSTANT(mgenc, value, null);
      } catch (ParseError e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder, boolean resultUsed) {
    if (resultUsed)
      opBuilder.dsl.emitLoadConstant(value);
  }
}
