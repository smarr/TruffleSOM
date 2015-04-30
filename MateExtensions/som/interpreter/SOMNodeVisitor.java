package som.interpreter;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.FieldNode;
import som.interpreter.nodes.MessageSendNode;
import som.interpreter.nodes.SequenceNode;


public interface SOMNodeVisitor {
  public void visit(FieldNode node);
  public void visit(MessageSendNode node);
  public void visit(SequenceNode node);
}
