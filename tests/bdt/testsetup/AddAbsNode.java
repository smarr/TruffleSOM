package bdt.testsetup;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import bdt.primitives.Primitive;


@NodeChild(value = "val", type = ExprNode.class)
@NodeChild(value = "abs", type = AbsNode.class, executeWith = "val")
@Primitive(primitive = "addAbs", selector = "addAbs", extraChild = AbsNodeFactory.class)
@GenerateNodeFactory
public abstract class AddAbsNode extends ExprNode {
  @Specialization
  public int addWithAbs(final int val, final int abs) {
    return val + abs;
  }
}
