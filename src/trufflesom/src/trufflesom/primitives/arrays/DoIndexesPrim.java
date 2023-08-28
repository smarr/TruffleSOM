package trufflesom.primitives.arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.primitives.basics.BlockPrims.ValueOnePrim;
import trufflesom.primitives.basics.BlockPrimsFactory.ValueOnePrimFactory;
import trufflesom.primitives.basics.LengthPrim;
import trufflesom.primitives.basics.LengthPrimFactory;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Array", primitive = "doIndexes:", selector = "doIndexes:",
    receiverType = SArray.class, disabled = true)
public abstract class DoIndexesPrim extends BinaryMsgExprNode {
  @Child private ValueOnePrim block;
  @Child private LengthPrim   length;

  @Override
  public SSymbol getSelector() {
    return SymbolTable.symbolFor("doIndexes:");
  }

  @Override
  @SuppressWarnings("unchecked")
  public DoIndexesPrim initialize(final long coord) {
    super.initialize(coord);
    block = ValueOnePrimFactory.create(null, null);
    length = LengthPrimFactory.create(null);
    return this;
  }

  @Specialization
  public final SArray doArray(final VirtualFrame frame,
      final SArray receiver, final SBlock block) {
    int l = (int) this.length.executeEvaluated(frame, receiver);
    loop(frame, block, l);
    return receiver;
  }

  private void loop(final VirtualFrame frame, final SBlock block, final int length) {
    try {
      assert SArray.FIRST_IDX == 0;
      if (SArray.FIRST_IDX < length) {
        this.block.executeEvaluated(
            // +1 because it is going to the Smalltalk level
            frame, block, (long) SArray.FIRST_IDX + 1);
      }
      for (long i = 1; i < length; i++) {
        this.block.executeEvaluated(
            frame, block, i + 1); // +1 because it is going to the Smalltalk level
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(length);
      }
    }
  }

  protected final void reportLoopCount(final long count) {
    assert count >= 0;
    CompilerAsserts.neverPartOfCompilation("reportLoopCount");
    Node current = getParent();
    while (current != null && !(current instanceof RootNode)) {
      current = current.getParent();
    }
    if (current != null) {
      ((Invokable) current).propagateLoopCountThroughoutLexicalScope(count);
    }
  }

}
