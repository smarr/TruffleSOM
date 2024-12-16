package trufflesom.primitives.arithmetic;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Integer", primitive = "bitXor:", selector = "bitXor:")
public abstract class BitXorPrim extends ArithmeticPrim {
  @Specialization
  public static final long doLong(final long receiver, final long right) {
    return receiver ^ right;
  }

  @Override
  public final SSymbol getSelector() {
    return SymbolTable.symbolFor("bitXor:");
  }
}
