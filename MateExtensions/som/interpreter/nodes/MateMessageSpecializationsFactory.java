package som.interpreter.nodes;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.EagerBinaryPrimitiveNode;
import som.interpreter.nodes.nary.EagerQuaternaryPrimitiveNode;
import som.interpreter.nodes.nary.EagerTernaryPrimitiveNode;
import som.interpreter.nodes.nary.EagerUnaryPrimitiveNode;
import som.interpreter.nodes.nary.MateEagerQuaternaryPrimitiveNode;
import som.interpreter.nodes.nary.MateEagerUnaryPrimitiveNode;
import som.interpreter.nodes.nary.MateEagerBinaryPrimitiveNode;
import som.interpreter.nodes.nary.MateEagerTernaryPrimitiveNode;
import som.interpreter.nodes.nary.QuaternaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vmobjects.SSymbol;


public class MateMessageSpecializationsFactory extends
    AbstractMessageSpecializationsFactory {
  @Override
  public EagerUnaryPrimitiveNode unaryPrimitiveFor(SSymbol selector,
      ExpressionNode receiver, UnaryExpressionNode primitive) {
    return new MateEagerUnaryPrimitiveNode(selector, receiver, primitive);
  }

  @Override
  public EagerBinaryPrimitiveNode binaryPrimitiveFor(SSymbol selector,
      ExpressionNode receiver, ExpressionNode argument,
      BinaryExpressionNode primitive) {
    return new MateEagerBinaryPrimitiveNode(selector, receiver, argument, primitive);
  }

  @Override
  public EagerTernaryPrimitiveNode ternaryPrimitiveFor(SSymbol selector,
      ExpressionNode receiver, ExpressionNode argument,
      ExpressionNode argument2, TernaryExpressionNode primitive) {
    return new MateEagerTernaryPrimitiveNode(selector, receiver, argument, argument2, primitive);    
  }
  
  @Override
  public EagerQuaternaryPrimitiveNode quaternaryPrimitiveFor(SSymbol selector,
      ExpressionNode receiver, ExpressionNode argument,
      ExpressionNode argument2, ExpressionNode argument3, QuaternaryExpressionNode primitive) {
    return new MateEagerQuaternaryPrimitiveNode(selector, receiver, argument, argument2, argument3, primitive);    
  }
}
