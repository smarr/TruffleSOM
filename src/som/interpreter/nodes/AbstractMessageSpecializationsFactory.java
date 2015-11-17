package som.interpreter.nodes;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.EagerBinaryPrimitiveNode;
import som.interpreter.nodes.nary.EagerQuaternaryPrimitiveNode;
import som.interpreter.nodes.nary.EagerTernaryPrimitiveNode;
import som.interpreter.nodes.nary.EagerUnaryPrimitiveNode;
import som.interpreter.nodes.nary.QuaternaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vmobjects.SSymbol;


public abstract class AbstractMessageSpecializationsFactory {
  public abstract EagerUnaryPrimitiveNode unaryPrimitiveFor(SSymbol selector, ExpressionNode receiver, UnaryExpressionNode primitive);
  public abstract EagerBinaryPrimitiveNode binaryPrimitiveFor(SSymbol selector, ExpressionNode receiver, ExpressionNode argument, BinaryExpressionNode primitive);
  public abstract EagerTernaryPrimitiveNode ternaryPrimitiveFor(SSymbol selector, ExpressionNode receiver, ExpressionNode argument, ExpressionNode argument2, TernaryExpressionNode primitive);
  public abstract EagerQuaternaryPrimitiveNode quaternaryPrimitiveFor(SSymbol selector, ExpressionNode receiver, ExpressionNode argument, ExpressionNode argument2, ExpressionNode argument3, QuaternaryExpressionNode primitive);
  
  public static class SOMMessageSpecializationsFactory extends AbstractMessageSpecializationsFactory {

    @Override
    public EagerUnaryPrimitiveNode unaryPrimitiveFor(SSymbol selector,
        ExpressionNode receiver, UnaryExpressionNode primitive) {
      return new EagerUnaryPrimitiveNode(selector, receiver, primitive);
    }

    @Override
    public EagerBinaryPrimitiveNode binaryPrimitiveFor(SSymbol selector,
        ExpressionNode receiver, ExpressionNode argument,
        BinaryExpressionNode primitive) {
      return new EagerBinaryPrimitiveNode(selector, receiver, argument, primitive);
    }

    @Override
    public EagerTernaryPrimitiveNode ternaryPrimitiveFor(SSymbol selector,
        ExpressionNode receiver, ExpressionNode argument,
        ExpressionNode argument2, TernaryExpressionNode primitive) {
      return new EagerTernaryPrimitiveNode(selector, receiver, argument, argument2, primitive);    
    }
    
    @Override
    public EagerQuaternaryPrimitiveNode quaternaryPrimitiveFor(SSymbol selector,
        ExpressionNode receiver, ExpressionNode argument,
        ExpressionNode argument2, ExpressionNode argument3, QuaternaryExpressionNode primitive) {
      return new EagerQuaternaryPrimitiveNode(selector, receiver, argument, argument2, argument3, primitive);    
    }
  }
}
