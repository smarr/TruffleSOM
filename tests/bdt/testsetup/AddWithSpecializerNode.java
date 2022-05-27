package bdt.testsetup;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bdt.primitives.Primitive;
import bdt.primitives.Specializer;
import bdt.testsetup.AddWithSpecializerNode.AlwaysSpecialize;


@NodeChild(value = "left", type = ExprNode.class)
@NodeChild(value = "right", type = ExprNode.class)
@Primitive(className = "Int", primitive = "++", selector = "++",
    specializer = AlwaysSpecialize.class)
@GenerateNodeFactory
public abstract class AddWithSpecializerNode extends ExprNode {

  public static class AlwaysSpecialize extends Specializer<ExprNode, String> {
    public AlwaysSpecialize(final Primitive prim, final NodeFactory<ExprNode> fact) {
      super(prim, fact);
    }

    @Override
    public boolean matches(final Object[] args, final ExprNode[] argNodes) {
      return true;
    }
  }

  @Specialization
  public int add(final int left, final int right) {
    return left + right;
  }
}
