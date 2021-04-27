package trufflesom.primitives.arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Primitive;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.primitives.basics.BlockPrims.ValueOnePrim;
import trufflesom.primitives.basics.BlockPrimsFactory.ValueOnePrimFactory;
import trufflesom.primitives.basics.LengthPrim;
import trufflesom.primitives.basics.LengthPrimFactory;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SBlock;


@GenerateNodeFactory
@Primitive(className = "Array", primitive = "doIndexes:", selector = "doIndexes:",
    receiverType = SArray.class, disabled = true)
public abstract class DoIndexesPrim extends BinaryExpressionNode {
  @Child private ValueOnePrim block;
  @Child private LengthPrim   length;

  @Override
  @SuppressWarnings("unchecked")
  public DoIndexesPrim initialize(final SourceSection source) {
    super.initialize(source);
    block = ValueOnePrimFactory.create(null, null);
    length = LengthPrimFactory.create(null);
    return this;
  }

  @Specialization
  public final SArray doArray(final VirtualFrame frame,
      final SArray receiver, final SBlock block) {
    int length = (int) this.length.executeEvaluated(receiver);
    loop(frame, block, length);
    return receiver;
  }

  private void loop(final VirtualFrame frame, final SBlock block, final int length) {
    try {
      assert SArray.FIRST_IDX == 0;
      if (SArray.FIRST_IDX < length) {
        this.block.executeEvaluated(
            // +1 because it is going to the Smalltalk level
            block, (long) SArray.FIRST_IDX + 1);
      }
      for (long i = 1; i < length; i++) {
        this.block.executeEvaluated(
            block, i + 1); // +1 because it is going to the Smalltalk level
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
