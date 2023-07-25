package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import bdt.inlining.Inline;
import bdt.inlining.ScopeAdaptationVisitor;
import bdt.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.NoPreEvalExprNode;


@NodeChild(value = "from", type = ExpressionNode.class)
@NodeChild(value = "to", type = ExpressionNode.class)
@Inline(selector = "to:do:", inlineableArgIdx = 2, introduceTemps = 2, disabled = true)
@GenerateNodeFactory
public abstract class IntToDoInlinedLiteralsNode extends NoPreEvalExprNode {

  @Child protected ExpressionNode body;

  // In case we need to revert from this optimistic optimization, keep the
  // original node around
  private final ExpressionNode bodyActualNode;

  private final int   loopIdxVarIndex;
  private final Local loopIdxVar;

  public abstract ExpressionNode getFrom();

  public abstract ExpressionNode getTo();

  public String getIndexName() {
    return loopIdxVar.getName().getString();
  }

  public IntToDoInlinedLiteralsNode(final ExpressionNode originalBody,
      final ExpressionNode body, final Local loopIdxVar) {
    this.body = body;
    this.loopIdxVar = loopIdxVar;
    this.loopIdxVarIndex = loopIdxVar.getIndex();
    this.bodyActualNode = originalBody;

    // and, we can already tell the loop index that it is going to be long
    // loopIdxVar.getFrameDescriptor().setSlotKind(loopIdxVarIndex, FrameSlotKind.Long);
  }

  @Specialization
  public final long doIntToDo(final VirtualFrame frame, final long from, final long to) {
    if (CompilerDirectives.inInterpreter()) {
      try {
        doLooping(frame, from, to);
      } finally {
        reportLoopCount((int) to - from);
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
        reportLoopCount((int) to - from);
      }
    } else {
      doLooping(frame, from, (long) to);
    }
    return from;
  }

  protected final void doLooping(final VirtualFrame frame, final long from, final long to) {
    loopIdxVar.getFrameDescriptor().setSlotKind(loopIdxVarIndex, FrameSlotKind.Long);
    if (from <= to) {
      frame.setLong(loopIdxVarIndex, from);
      body.executeGeneric(frame);
    }
    for (long i = from + 1; i <= to; i++) {
      frame.setLong(loopIdxVarIndex, i);
      body.executeGeneric(frame);
    }
  }

  @Specialization
  public final double doDoubleToDo(final VirtualFrame frame, final double from,
      final double to) {
    this.loopIdxVar.getFrameDescriptor().setSlotKind(loopIdxVarIndex, FrameSlotKind.Double);
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
    if (from <= to) {
      frame.setDouble(loopIdxVarIndex, from);
      body.executeGeneric(frame);
    }
    for (double i = from + 1.0; i <= to; i += 1.0) {
      frame.setDouble(loopIdxVarIndex, i);
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
    ScopeElement<ExpressionNode> se = inliner.getAdaptedVar(loopIdxVar);
    IntToDoInlinedLiteralsNode node = IntToDoInlinedLiteralsNodeFactory.create(bodyActualNode,
        body, (Local) se.var, getFrom(), getTo());
    node.initialize(sourceCoord);
    replace(node);
  }
}
