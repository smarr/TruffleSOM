package trufflesom.primitives.arithmetic;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vmobjects.SAbstractObject;


@GenerateNodeFactory
@Primitive(className = "Integer", primitive = "//")
@Primitive(className = "Double", primitive = "//")
@Primitive(selector = "//")
public abstract class DoubleDivPrim extends ArithmeticPrim {
  @Specialization
  public final double doDouble(final double left, final double right) {
    return left / right;
  }

  @Specialization
  public final double doLong(final long left, final long right) {
    return ((double) left) / right;
  }

  @Specialization
  public final double doDouble(final double left, final long right) {
    return doDouble(left, (double) right);
  }

  @Specialization
  public final SAbstractObject doLong(final long left, final BigInteger right) {
    CompilerAsserts.neverPartOfCompilation("DoubleDiv100");
    // TODO: need to implement the "/" case here directly... :
    // return resendAsBigInteger("/", left, (SBigInteger) rightObj, frame.pack());
    throw new NotYetImplementedException();
  }

  @Specialization
  public final double doLong(final long left, final double right) {
    return left / right;
  }
}
