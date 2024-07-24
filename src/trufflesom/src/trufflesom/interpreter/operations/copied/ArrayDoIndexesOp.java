package trufflesom.interpreter.operations.copied;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.primitives.basics.BlockPrims.ValueOnePrim;
import trufflesom.primitives.basics.BlockPrimsFactory.ValueOnePrimFactory;
import trufflesom.primitives.basics.LengthPrim;
import trufflesom.primitives.basics.LengthPrimFactory;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SSymbol;


@Proxyable
public abstract class ArrayDoIndexesOp extends BinaryExpressionNode {

  public static final SSymbol symDoIndexes = SymbolTable.symbolFor("doIndexes:");

  @NeverDefault
  public static ValueOnePrim createValue() {
    return ValueOnePrimFactory.create(null, null);
  }

  @NeverDefault
  public static LengthPrim createLength() {
    return LengthPrimFactory.create(null);
  }

  @Specialization
  public static final SArray doArray(final VirtualFrame frame,
      final SArray receiver, final SBlock block,
      @Bind final Node node,
      @Cached("createValue()") final ValueOnePrim call,
      @Cached("createLength()") final LengthPrim length) {
    int l = (int) length.executeEvaluated(frame, receiver);
    loop(frame, block, l, call, node);
    return receiver;
  }

  private static void loop(final VirtualFrame frame, final SBlock block, final int length,
      final ValueOnePrim call, final Node node) {
    try {
      assert SArray.FIRST_IDX == 0;
      if (SArray.FIRST_IDX < length) {
        call.executeEvaluated(
            // +1 because it is going to the Smalltalk level
            frame, block, (long) SArray.FIRST_IDX + 1);
      }
      for (long i = 1; i < length; i++) {
        call.executeEvaluated(
            frame, block, i + 1); // +1 because it is going to the Smalltalk level
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        ArrayDoOp.reportLoopCount(node, length);
      }
    }
  }

  @Specialization
  public static Object fallback(final VirtualFrame frame, final Object rcvr,
      final Object arg,
      @Cached("create(symDoIndexes)") final AbstractDispatchNode dispatch) {
    return dispatch.executeDispatch(frame, new Object[] {rcvr, arg});
  }
}
