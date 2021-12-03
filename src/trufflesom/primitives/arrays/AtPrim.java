package trufflesom.primitives.arrays;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Array", primitive = "at:", selector = "at:",
    receiverType = SArray.class, inParser = false)
public abstract class AtPrim extends BinaryMsgExprNode {
  @Override
  public SSymbol getSelector() {
    return SymbolTable.symbolFor("at:");
  }

  @Specialization(guards = "receiver.isEmptyType()")
  public final Object doEmptySArray(final SArray receiver, final long idx) {
    assert idx > 0;
    assert idx <= receiver.getEmptyStorage();
    return Nil.nilObject;
  }

  @Specialization(guards = "receiver.isPartiallyEmptyType()")
  public final Object doPartiallyEmptySArray(final SArray receiver, final long idx) {
    return receiver.getPartiallyEmptyStorage().get(idx - 1);
  }

  @Specialization(guards = "receiver.isObjectType()")
  public final Object doObjectSArray(final SArray receiver, final long idx) {
    return receiver.getObjectStorage()[(int) idx - 1];
  }

  @Specialization(guards = "receiver.isLongType()")
  public final long doLongSArray(final SArray receiver, final long idx) {
    return receiver.getLongStorage()[(int) idx - 1];
  }

  @Specialization(guards = "receiver.isDoubleType()")
  public final double doDoubleSArray(final SArray receiver, final long idx) {
    return receiver.getDoubleStorage()[(int) idx - 1];
  }

  @Specialization(guards = "receiver.isBooleanType()")
  public final boolean doBooleanSArray(final SArray receiver, final long idx) {
    return receiver.getBooleanStorage()[(int) idx - 1];
  }
}
