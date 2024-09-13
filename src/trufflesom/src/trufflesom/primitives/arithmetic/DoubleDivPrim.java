package trufflesom.primitives.arithmetic;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SSymbol;


@Proxyable
@GenerateNodeFactory
@Primitive(className = "Integer", primitive = "//")
@Primitive(className = "Double", primitive = "//")
@Primitive(selector = "//")
public abstract class DoubleDivPrim extends ArithmeticPrim {
  @Override
  public SSymbol getSelector() {
    return SymbolTable.symbolFor("//");
  }

  @Specialization
  public static final double doDouble(final double left, final double right) {
    return left / right;
  }

  @Specialization
  public static final double doDouble(final double left, final long right) {
    return doDouble(left, (double) right);
  }

  @Specialization
  public static final double doLong(final long left, final long right) {
    return ((double) left) / right;
  }

  @Specialization
  public static final double doLong(final long left, final double right) {
    return left / right;
  }

  @Specialization
  @TruffleBoundary
  @SuppressWarnings("unused")
  public static final SAbstractObject doLong(final long left, final BigInteger right) {
    CompilerAsserts.neverPartOfCompilation("DoubleDiv100");
    // TODO: need to implement the "/" case here directly... :
    // return resendAsBigInteger("/", left, (SBigInteger) rightObj, frame.pack());
    throw new NotYetImplementedException();
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder, boolean resultUsed) {
    opBuilder.dsl.beginDoubleDivPrim();
    getReceiver().accept(opBuilder);
    getArgument().accept(opBuilder);
    opBuilder.dsl.endDoubleDivPrim();
  }
}
