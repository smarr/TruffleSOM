package som.interpreter;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;

import som.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import som.interpreter.nodes.ArgumentReadNode.NonLocalArgumentReadNode;
import som.interpreter.nodes.ContextualNode;
import som.interpreter.nodes.GlobalNode;
import som.interpreter.nodes.MateExpressionNode;
import som.interpreter.nodes.ReturnNonLocalNode.CatchNonLocalReturnNode;
import som.interpreter.nodes.SOMNode;
import som.interpreter.nodes.SequenceNode;
import som.interpreter.nodes.literals.LiteralNode;
import som.interpreter.nodes.specialized.whileloops.WhileCache;
import som.interpreter.objectstorage.FieldAccessorNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import som.primitives.GlobalPrim;
import som.primitives.HasGlobalPrim;
import som.primitives.reflection.PerformInSuperclassPrim;
import som.primitives.reflection.PerformWithArgumentsInSuperclassPrim;
import som.primitives.reflection.PerformWithArgumentsPrim;


public class MateifyVisitor implements NodeVisitor {

  public boolean visit(Node node) {
    Node matenode = null;
    if (
        (
            (node instanceof FieldAccessorNode)
        )
   || (
            (node instanceof SOMNode) && 
            !(node instanceof ContextualNode) && 
            !(node instanceof MateExpressionNode) &&
            !(node instanceof CatchNonLocalReturnNode) &&
            !(node instanceof SequenceNode) &&
            !(node instanceof WhileCache) &&
            !(node instanceof Primitive) &&
            !(node instanceof PerformWithArgumentsPrim) &&
            !(node instanceof PerformWithArgumentsInSuperclassPrim) &&
            !(node instanceof PerformInSuperclassPrim) &&
            !(node instanceof HasGlobalPrim) &&
            !(node instanceof GlobalPrim) &&
            !(node instanceof LiteralNode) &&
            !(node instanceof LocalArgumentReadNode) &&
            !(node instanceof NonLocalArgumentReadNode) &&
            !(node instanceof GlobalNode)
       )
       ){
      if (node instanceof SOMNode){
        matenode = ((SOMNode) node).wrapIntoMateNode();
      } else {
        if (node instanceof AbstractWriteFieldNode){
          matenode = ((AbstractWriteFieldNode) node).wrapIntoMateNode();
        } else {
          if (node instanceof AbstractReadFieldNode){
            matenode = ((AbstractReadFieldNode) node).wrapIntoMateNode();
          } else {
            matenode = ((SOMNode) node).wrapIntoMateNode();
          }
        }
      }
      node.replace(matenode);
    }
    return true;
  }

}