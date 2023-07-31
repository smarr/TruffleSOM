package trufflesom.bdt.primitives;

import java.util.ArrayList;
import java.util.List;

import trufflesom.bdt.testsetup.AbsNodeFactory;
import trufflesom.bdt.testsetup.AddAbsNodeFactory;
import trufflesom.bdt.testsetup.AddNodeFactory;
import trufflesom.bdt.testsetup.AddWithSpecializerNodeFactory;
import trufflesom.bdt.testsetup.ExprNode;
import trufflesom.bdt.testsetup.StringId;


public class Primitives extends PrimitiveLoader<ExprNode, String> {
  protected Primitives() {
    super(new StringId());
    initialize();
  }

  @Override
  protected List<Specializer<ExprNode, String>> getSpecializers() {
    List<Specializer<ExprNode, String>> allSpecializers = new ArrayList<>();

    add(allSpecializers, AddNodeFactory.getInstance());
    add(allSpecializers, AddWithSpecializerNodeFactory.getInstance());
    add(allSpecializers, AbsNodeFactory.getInstance());
    add(allSpecializers, AddAbsNodeFactory.getInstance());

    return allSpecializers;
  }

  @Override
  protected void registerPrimitive(final Specializer<ExprNode, String> specializer) {
    /* not needed for testing */
  }
}
