package som.interpreter;

import com.oracle.truffle.api.dsl.internal.SpecializationNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;

import som.interpreter.nodes.ContextualNode;
import som.interpreter.nodes.MateDispatch;
import som.interpreter.nodes.MateExpressionNode;
import som.interpreter.nodes.ReturnNonLocalNode.CatchNonLocalReturnNode;
import som.interpreter.nodes.SOMNode;
import som.interpreter.nodes.SequenceNode;
import som.interpreter.nodes.literals.LiteralNode;
import som.interpreter.nodes.specialized.whileloops.WhileCache;
import som.interpreter.nodes.specialized.whileloops.WhileCacheNodeGen;
import som.interpreter.objectstorage.FieldAccessorNode;
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
            (node instanceof SOMNode) &&
            !(node instanceof SOMNode) &&
            !(node instanceof MateDispatch) &&
            !(node instanceof SpecializationNode) &&
            !(node instanceof Invokable)
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
            !(node instanceof LiteralNode)
       )
       //|| 
       //(node instanceof FieldAccessorNode) 
       ){
      if (node instanceof SOMNode){
        matenode = ((SOMNode) node).wrapIntoMateNode();
      } else {
        if (node instanceof FieldAccessorNode){
          matenode = ((FieldAccessorNode) node).wrapIntoMateNode();
        } else {
          matenode = ((SOMNode) node).wrapIntoMateNode();
        }
      }
      node.replace(matenode);
    }
    return true;
  }

}