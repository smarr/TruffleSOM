package som.interpreter;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;

import som.interpreter.nodes.FieldNode;
import som.interpreter.nodes.MessageSendNode;
import som.interpreter.nodes.SequenceNode;
import som.primitives.ObjectPrimsFactory.ClassPrimFactory.ClassPrimNodeGen;


public class MateifyVisitor implements NodeVisitor {

  public void visit(FieldNode node) {
    // TODO Auto-generated method stub

  }

  public void visit(MessageSendNode node) {
    // TODO Auto-generated method stub

  }

  public void visit(SequenceNode node) {
    // TODO Auto-generated method stub

  }
  
  public boolean visit(Primitive node) {
    // TODO Auto-generated method stub
    return true;
  }
  
  public boolean visit(ClassPrimNodeGen node) {
    // TODO Auto-generated method stub
    return true;
  }

  public boolean visit(Node node) {
    // TODO Auto-generated method stub
    return true;
  }

}