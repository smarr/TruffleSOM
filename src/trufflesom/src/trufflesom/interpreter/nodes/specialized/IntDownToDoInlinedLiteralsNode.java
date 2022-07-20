package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.bytecode.BytecodeLocal;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import trufflesom.bdt.inlining.Inline;
import trufflesom.bdt.inlining.ScopeAdaptationVisitor;
import trufflesom.bdt.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.NoPreEvalExprNode;


@NodeChild(value = "from", type = ExpressionNode.class)
@NodeChild(value = "to", type = ExpressionNode.class)
@Inline(selector = "downTo:do:", inlineableArgIdx = 2, introduceTemps = 2, disabled = true)
@GenerateNodeFactory
public abstract class IntDownToDoInlinedLiteralsNode extends NoPreEvalExprNode {

  @Child protected ExpressionNode body;

  // In case we need to revert from this optimistic optimization, keep the
  // original node around
  private final ExpressionNode bodyActualNode;

  private final int   loopIndex;
  private final Local loopIdxVar;

  public abstract ExpressionNode getFrom();

  public abstract ExpressionNode getTo();

  public String getIndexName() {
    return loopIdxVar.getName();
  }

  public IntDownToDoInlinedLiteralsNode(final ExpressionNode originalBody,
      final ExpressionNode body, final Local loopIdxVar) {
    this.body = body;
    this.loopIdxVar = loopIdxVar;
    this.loopIndex = loopIdxVar.getIndex();
    this.bodyActualNode = originalBody;

    // and, we can already tell the loop index that it is going to be long
    // loopIndex.setKind(FrameSlotKind.Long);
  }

  @Specialization
  public final long doInt(final VirtualFrame frame, final long from, final long to) {
    if (CompilerDirectives.inInterpreter()) {
      try {
        doLooping(frame, from, to);
      } finally {
        reportLoopCount((int) from - to);
      }
    } else {
      doLooping(frame, from, to);
    }
    return from;
  }

  @Specialization
  public final long doIntToDo(final VirtualFrame frame, final long from, final double to) {
    if (CompilerDirectives.inInterpreter()) {
      try {
        doLooping(frame, from, (long) to);
      } finally {
        reportLoopCount((int) from - (int) to);
      }
    } else {
      doLooping(frame, from, (long) to);
    }
    return from;
  }

  protected final void doLooping(final VirtualFrame frame, final long from, final long to) {
    loopIdxVar.getFrameDescriptor().setSlotKind(loopIndex, FrameSlotKind.Long);
    if (from >= to) {
      frame.setLong(loopIndex, from);
      body.executeGeneric(frame);
    }
    for (long i = from - 1; i >= to; i--) {
      frame.setLong(loopIndex, i);
      body.executeGeneric(frame);
    }
  }

  @Specialization
  public final double doDoubleToDo(final VirtualFrame frame, final double from,
      final double to) {
    loopIdxVar.getFrameDescriptor().setSlotKind(loopIndex, FrameSlotKind.Double);
    if (CompilerDirectives.inInterpreter()) {
      try {
        doLoopingDouble(frame, from, to);
      } finally {
        reportLoopCount((int) (to - from));
      }
    } else {
      doLoopingDouble(frame, from, to);
    }
    return from;
  }

  protected final void doLoopingDouble(final VirtualFrame frame, final double from,
      final double to) {
    if (from >= to) {
      frame.setDouble(loopIndex, from);
      body.executeGeneric(frame);
    }
    for (double i = from - 1.0d; i >= to; i -= 1.0d) {
      frame.setDouble(loopIndex, i);
      body.executeGeneric(frame);
    }
  }

  private void reportLoopCount(final long count) {
    if (count < 1) {
      return;
    }

    CompilerAsserts.neverPartOfCompilation("reportLoopCount");
    Node current = getParent();
    while (current != null && !(current instanceof RootNode)) {
      current = current.getParent();
    }
    if (current != null) {
      ((Invokable) current).propagateLoopCountThroughoutLexicalScope(count);
    }
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    ScopeElement se = inliner.getAdaptedVar(loopIdxVar);
    IntDownToDoInlinedLiteralsNode node =
        IntDownToDoInlinedLiteralsNodeFactory.create(bodyActualNode,
            body, (Local) se.var, getFrom(), getTo());
    node.initialize(sourceCoord);
    replace(node);
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder) {
    BytecodeLocal idx = opBuilder.getLocal(loopIdxVar);
    BytecodeLocal from = opBuilder.dsl.createLocal();
    BytecodeLocal to = opBuilder.dsl.createLocal();

    opBuilder.dsl.beginBlock(); // start the expression

    // eval rcvr
    opBuilder.dsl.beginStoreLocal(from);
    getFrom().accept(opBuilder);
    opBuilder.dsl.endStoreLocal();

    // eval argument
    opBuilder.dsl.beginStoreLocal(to);
    getTo().accept(opBuilder);
    opBuilder.dsl.endStoreLocal();

    // idx := from
    opBuilder.dsl.beginStoreLocal(idx);
    opBuilder.dsl.emitLoadLocal(from);
    opBuilder.dsl.endStoreLocal();

    opBuilder.dsl.beginWhile();

    // condition: to <= idx (note that to and idx are swapped compare to the to:do:)
    opBuilder.dsl.beginLoopBoundLessOrEqual();
    opBuilder.dsl.emitLoadLocal(to);
    opBuilder.dsl.emitLoadLocal(idx);
    opBuilder.dsl.endLoopBoundLessOrEqual();

    opBuilder.dsl.beginBlock(); // begin loop body

    // do body of loop
    body.accept(opBuilder);

    // idx := idx - 1
    opBuilder.dsl.beginUnsafeLoopDecrement(idx);
    opBuilder.dsl.emitLoadLocal(idx);
    opBuilder.dsl.endUnsafeLoopDecrement();

    opBuilder.dsl.endBlock();

    opBuilder.dsl.endWhile();

    // return the receiver at the end
    opBuilder.dsl.emitLoadLocal(from);

    opBuilder.dsl.endBlock(); // end the expression
  }
}
