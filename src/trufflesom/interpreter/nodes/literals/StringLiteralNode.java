package trufflesom.interpreter.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;

import bd.inlining.ScopeAdaptationVisitor;
import trufflesom.compiler.Parser.ParseError;
import trufflesom.compiler.bc.BytecodeGenerator;
import trufflesom.compiler.bc.BytecodeMethodGenContext;


public final class StringLiteralNode extends LiteralNode {

  private final String value;

  public StringLiteralNode(final String value) {
    this.value = value;
  }

  @Override
  public String executeString(final VirtualFrame frame) {
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
}
