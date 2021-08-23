package trufflesom.interpreter.nodes.literals;

import java.math.BigInteger;

import com.oracle.truffle.api.frame.VirtualFrame;

import bd.inlining.ScopeAdaptationVisitor;
import trufflesom.compiler.Parser.ParseError;
import trufflesom.compiler.bc.BytecodeGenerator;
import trufflesom.compiler.bc.BytecodeMethodGenContext;


public final class BigIntegerLiteralNode extends LiteralNode {

  private final BigInteger value;

  public BigIntegerLiteralNode(final BigInteger value) {
    this.value = value;
  }

  @Override
  public BigInteger executeBigInteger(final VirtualFrame frame) {
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
