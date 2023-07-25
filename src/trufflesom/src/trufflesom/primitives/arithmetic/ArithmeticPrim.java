package trufflesom.primitives.arithmetic;

import java.math.BigInteger;

import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;


public abstract class ArithmeticPrim extends BinaryMsgExprNode {
  protected static final Number reduceToLongIfPossible(final BigInteger result) {
    if (result.bitLength() > Long.SIZE - 1) {
      return result;
    } else {
      return result.longValue();
    }
  }
}
