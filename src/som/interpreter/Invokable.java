package som.interpreter;

import som.compiler.MethodGenerationContext;
import som.compiler.Variable.Local;
import som.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import som.interpreter.nodes.ArgumentReadNode.LocalSuperReadNode;
import som.interpreter.nodes.ArgumentReadNode.NonLocalArgumentReadNode;
import som.interpreter.nodes.ArgumentReadNode.NonLocalSuperReadNode;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.FieldNode.FieldWriteNode;
import som.interpreter.nodes.MateArgumentReadNode;
import som.interpreter.nodes.MateFieldNodes;
import som.interpreter.nodes.FieldNode.FieldReadNode;
import som.interpreter.nodes.MateUninitializedMessageSendNode;
import som.interpreter.nodes.MateUninitializedVariableNode;
import som.interpreter.nodes.MessageSendNode.UninitializedMessageSendNode;
import som.interpreter.nodes.UninitializedVariableNode.UninitializedVariableReadNode;
import som.interpreter.nodes.UninitializedVariableNode.UninitializedVariableWriteNode;
import som.vm.MateUniverse;
import som.vm.Universe;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.MateFieldNodesFactory.MateFieldWriteNodeGen;

public abstract class Invokable extends RootNode implements MateNode{

  @Child protected ExpressionNode  expressionOrSequence;

  protected final ExpressionNode uninitializedBody;

  public Invokable(final SourceSection sourceSection,
      final FrameDescriptor frameDescriptor,
      final ExpressionNode expressionOrSequence,
      final ExpressionNode uninitialized) {
    super(SomLanguage.class, sourceSection, frameDescriptor);
    this.uninitializedBody = this.mateifyUninitializedNode(uninitialized);
    this.expressionOrSequence = expressionOrSequence;
  }

  @Override
  public final Object execute(final VirtualFrame frame) {
    return expressionOrSequence.executeGeneric(frame);
  }

  public abstract Invokable cloneWithNewLexicalContext(final LexicalScope outerContext);

  public ExpressionNode inline(final MethodGenerationContext mgenc,
      final Local[] locals) {
    return InlinerForLexicallyEmbeddedMethods.doInline(uninitializedBody, mgenc,
        locals, getSourceSection().getCharIndex());
  }

  @Override
  public final boolean isCloningAllowed() {
    return true;
  }

  public final RootCallTarget createCallTarget() {
    return Truffle.getRuntime().createCallTarget(this);
  }

  public abstract void propagateLoopCountThroughoutLexicalScope(final long count);
  
  public void wrapIntoMateNode() {
    MateifyVisitor visitor = new MateifyVisitor();
    uninitializedBody.accept(visitor);
  }
  
  private ExpressionNode mateifyUninitializedNode(ExpressionNode uninitialized){
    if (Universe.current() instanceof MateUniverse){
      if (uninitialized instanceof UninitializedMessageSendNode){
        return new MateUninitializedMessageSendNode((UninitializedMessageSendNode)uninitialized);
      }
      if (uninitialized instanceof FieldReadNode){
        return new MateFieldNodes.MateFieldReadNode((FieldReadNode)uninitialized);
      }
      if (uninitialized instanceof FieldWriteNode){
        FieldWriteNode node = (FieldWriteNode)uninitialized;
        return MateFieldWriteNodeGen.create(node, node.getSelf(), node.getValue());
      }
      if (uninitialized instanceof LocalArgumentReadNode){
        return new MateArgumentReadNode.MateLocalArgumentReadNode((LocalArgumentReadNode)uninitialized);
      }
      if (uninitialized instanceof NonLocalArgumentReadNode){
        return new MateArgumentReadNode.MateNonLocalArgumentReadNode((NonLocalArgumentReadNode)uninitialized);
      }
      if (uninitialized instanceof LocalSuperReadNode){
        return new MateArgumentReadNode.MateLocalSuperReadNode((LocalSuperReadNode)uninitialized);
      }
      if (uninitialized instanceof NonLocalSuperReadNode){
        return new MateArgumentReadNode.MateNonLocalSuperReadNode((NonLocalSuperReadNode)uninitialized);
      }
      if (uninitialized instanceof UninitializedVariableReadNode){
        return new MateUninitializedVariableNode.MateUninitializedVariableReadNode((UninitializedVariableReadNode)uninitialized);
      }
      if (uninitialized instanceof UninitializedVariableWriteNode){
        return new MateUninitializedVariableNode.MateUninitializedVariableWriteNode((UninitializedVariableWriteNode)uninitialized);
      }
    }
    return uninitialized;
  }
}
