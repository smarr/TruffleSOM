package trufflesom.primitives.arithmetic;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Integer", primitive = "bitXor:", selector = "bitXor:")
public abstract class BitXorPrim extends ArithmeticPrim {
  @Specialization
  public final long doLong(final long receiver, final long right) {
    return receiver ^ right;
  }

  @Override
  public SSymbol getSelector() {
    return SymbolTable.symbolFor("bitXor:");
  }
}
