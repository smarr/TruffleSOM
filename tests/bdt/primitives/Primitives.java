package bdt.primitives;

import java.util.ArrayList;
import java.util.List;

import bd.testsetup.AbsNodeFactory;
import bd.testsetup.AddAbsNodeFactory;
import bd.testsetup.AddNodeFactory;
import bd.testsetup.AddWithSpecializerNodeFactory;
import bdt.primitives.PrimitiveLoader;
import bdt.primitives.Specializer;
import bdt.testsetup.ExprNode;
import bdt.testsetup.StringId;


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
