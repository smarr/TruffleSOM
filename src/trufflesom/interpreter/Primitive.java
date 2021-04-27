package trufflesom.interpreter;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.compiler.MethodGenerationContext;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vmobjects.SInvokable.SMethod;


public final class Primitive extends Invokable {

  public Primitive(final String name, final SourceSection sourceSection,
      final ExpressionNode primitive, final FrameDescriptor frameDescriptor,
      final ExpressionNode uninitialized, final SomLanguage lang) {
    super(name, sourceSection, frameDescriptor, primitive, uninitialized, lang);
  }

  @Override
  public Node deepCopy() {
    assert getFrameDescriptor().getSize() == 0 : "Make sure there are no slots to be taken care off";
    return new Primitive(name, sourceSection, NodeUtil.cloneNode(uninitializedBody),
        getFrameDescriptor(), uninitializedBody, getLanguage(SomLanguage.class));
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
    return expressionOrSequence.getClass().getSimpleName() + "@"
        + Integer.toHexString(hashCode());
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

  public static void propagateLoopCount(final long count) {
    CompilerAsserts.neverPartOfCompilation("Primitive.pLC(.)");

    // we need to skip the primitive and get to the method that called the primitive
    FrameInstance caller = Truffle.getRuntime().getCallerFrame();

    RootCallTarget ct = (RootCallTarget) caller.getCallTarget(); // caller method
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
}
