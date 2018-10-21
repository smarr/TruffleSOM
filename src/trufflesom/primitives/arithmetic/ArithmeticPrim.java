package som.primitives.arithmetic;

import java.math.BigInteger;

import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.nary.BinaryExpressionNode;


public abstract class ArithmeticPrim extends BinaryExpressionNode {
  public ArithmeticPrim(final SourceSection source) {
    super(source);
  }

  protected final Number reduceToLongIfPossible(final BigInteger result) {
    if (result.bitLength() > Long.SIZE - 1) {
      return result;
    } else {
      return result.longValue();
    }
  }
}
