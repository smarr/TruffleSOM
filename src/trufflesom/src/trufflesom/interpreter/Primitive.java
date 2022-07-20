package trufflesom.interpreter;

import java.util.HashMap;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.bytecode.BytecodeLocal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.Source;

import trufflesom.bdt.primitives.nodes.PreevaluatedExpression;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.operations.SomOperations.LexicalScopeForOp;
import trufflesom.interpreter.operations.SomOperationsGen;
import trufflesom.vmobjects.SInvokable.SMethod;


public final class Primitive extends Invokable {

  @Child private PreevaluatedExpression primitive;

  private final PreevaluatedExpression uninitialized;

  public Primitive(final String name, final Source source, final long sourceCoord,
      final PreevaluatedExpression body, final PreevaluatedExpression uninitialized) {
    super(name, source, sourceCoord, new FrameDescriptor());
    this.primitive = body;
    this.uninitialized = uninitialized;
  }

  @Override
  public Object execute(final VirtualFrame frame) {
    return primitive.doPreEvaluated(frame, frame.getArguments());
  }

  @Override
  public Node deepCopy() {
    return new Primitive(name, source, sourceCoord, copyTrivialNode(), uninitialized);
  }

  @Override
  public ExpressionNode inline(final MethodGenerationContext mgenc, final SMethod outer) {
    // Note for completeness: for primitives, we use eager specialization,
    // which is essentially much simpler inlining
    throw new UnsupportedOperationException(
        "Primitives are currently not directly inlined. Only block methods are.");
  }

  @Override
  public String toString() {
    return primitive.getClass().getSimpleName();
  }

  @Override
  public void propagateLoopCountThroughoutLexicalScope(final long count) {
    propagateLoopCount(count);
  }

  private static Method getNextMethodOnStack() {
    return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Method>() {
      @Override
      public Method visitFrame(final FrameInstance frameInstance) {
        RootCallTarget ct = (RootCallTarget) frameInstance.getCallTarget();
        Invokable m = (Invokable) ct.getRootNode();
        if (m instanceof Primitive) {
          return null;
        } else {
          return (Method) m;
        }
      }
    });
  }

  private static CallTarget getCallerCallTarget() {
    FrameInstance caller = Truffle.getRuntime().iterateFrames((f) -> f, 1);
    return caller.getCallTarget(); // caller method
  }

  public static void propagateLoopCount(final long count) {
    CompilerAsserts.neverPartOfCompilation("Primitive.pLC(.)");

    // we need to skip the primitive and get to the method that called the primitive
    RootCallTarget ct = (RootCallTarget) getCallerCallTarget();
    Invokable m = (Invokable) ct.getRootNode();

    if (m instanceof Primitive) {
      // the caller is a primitive, that doesn't help, we need to skip it and
      // find a proper method
      m = getNextMethodOnStack();
    }

    if (m != null && !(m instanceof Primitive)) {
      m.propagateLoopCountThroughoutLexicalScope(count);
    }
  }

  @Override
  public boolean isTrivial() {
    return true;
  }

  @Override
  public PreevaluatedExpression copyTrivialNode() {
    return (PreevaluatedExpression) NodeUtil.cloneNode((Node) uninitialized);
  }

  @Override
  public AbstractDispatchNode asDispatchNode(final Object rcvr,
      final AbstractDispatchNode next) {
    return ((ExpressionNode) primitive).asDispatchNode(rcvr, source, next);
  }

  @Override
  public Invokable convertMethod(final SomOperationsGen.Builder opBuilder,
      final LexicalScopeForOp scope) {
    throw new IllegalStateException(
        "Should never be reached. SPrimitive already should prevent this,"
            + " and does not convert its invokables.");
  }

  @Override
  public HashMap<Local, BytecodeLocal> createLocals(
      final SomOperationsGen.Builder opBuilder) {
    throw new IllegalStateException(
        "Should never be reached. SPrimitive already should prevent this,"
            + " and does not convert its invokables.");
  }
}
