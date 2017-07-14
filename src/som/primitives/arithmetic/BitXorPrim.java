package som.primitives.arithmetic;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import som.primitives.Primitive;


@GenerateNodeFactory
@Primitive(className = "Integer", primitive = "bitXor:", selector = "bitXor:")
public abstract class BitXorPrim extends ArithmeticPrim {
  public BitXorPrim(final SourceSection source) {
    super(source);
  }

  @Specialization
  public final long doLong(final long receiver, final long right) {
    return receiver ^ right;
  }
}
