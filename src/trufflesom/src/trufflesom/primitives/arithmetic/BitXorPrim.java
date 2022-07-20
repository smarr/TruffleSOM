package trufflesom.primitives.arithmetic;

import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SSymbol;

import java.math.BigInteger;


@Proxyable
@GenerateNodeFactory
@Primitive(className = "Integer", primitive = "bitXor:", selector = "bitXor:")
public abstract class BitXorPrim extends ArithmeticPrim {
  @Specialization
  public static final long doLong(final long receiver, final long right) {
    return receiver ^ right;
  }

  @Specialization
  @TruffleBoundary
  public static final BigInteger doBigInt(final BigInteger receiver, final long right) {
    return receiver.xor(BigInteger.valueOf(right));
  }

  @Specialization
  @TruffleBoundary
  public static final BigInteger doBigInt(final BigInteger receiver, final BigInteger right) {
    return receiver.xor(right);
  }

  @Specialization
  @TruffleBoundary
  public static final BigInteger doLong(final long receiver, final BigInteger right) {
    return BigInteger.valueOf(receiver).xor(right);
  }

  @Override
  public final SSymbol getSelector() {
    return SymbolTable.symbolFor("bitXor:");
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder) {
    opBuilder.dsl.beginBitXorPrim();
    getReceiver().accept(opBuilder);
    getArgument().accept(opBuilder);
    opBuilder.dsl.endBitXorPrim();
  }
}
