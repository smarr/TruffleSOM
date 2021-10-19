package trufflesom.interpreter.nodes.bc;

import static trufflesom.interpreter.bc.Bytecodes.Q_SEND_2;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.TruffleCompiler;
import trufflesom.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode.BinaryMessageSendNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SSymbol;


public class BinaryPrimitiveWrapper extends BinaryExpressionNode {
  @Child private BinaryExpressionNode primitive;

  private final int bytecodeIndex;

  private final Universe universe;
  private final SSymbol  selector;

  public BinaryPrimitiveWrapper(final int bytecodeIndex, final SSymbol selector,
      final BinaryExpressionNode primitive, final Universe universe,
      final SourceSection sourceSection) {
    this.primitive = insert(primitive);
    this.selector = selector;
    this.universe = universe;
    this.bytecodeIndex = bytecodeIndex;
    this.sourceSection = sourceSection;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    CompilerDirectives.transferToInterpreter();
    throw new UnsupportedOperationException(
        "In the bytecode interpreter, `executeEvaluated` is expected to be used");
  }

  @Override
  public Object executeEvaluated(final VirtualFrame frame,
      final Object receiver, final Object argument) {
    try {
      return primitive.executeEvaluated(frame, receiver, argument);
    } catch (UnsupportedSpecializationException e) {
      TruffleCompiler.transferToInterpreterAndInvalidate(
          "Binary primitive with unsupported specialization.");
      return makeGenericSend().doPreEvaluated(frame,
          new Object[] {receiver, argument});
    }
  }

  private AbstractMessageSendNode makeGenericSend() {
    UninitializedDispatchNode uninit = new UninitializedDispatchNode(selector, universe);
    BinaryMessageSendNode node =
        new BinaryMessageSendNode(selector, null, null, uninit);
    node.initialize(sourceSection);

    assert getParent() instanceof BytecodeLoopNode : "BinaryPrimitiveWrapper are expected to be direct children of a `BytecodeLoopNode`.";
    ((BytecodeLoopNode) getParent()).requicken(bytecodeIndex, Q_SEND_2, node);

    return node;
  }

}
