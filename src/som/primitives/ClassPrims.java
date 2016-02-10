package som.primitives;

import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SArray;
import som.vmobjects.SClass;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.object.basic.DynamicObjectBasic;


public class ClassPrims {

  @GenerateNodeFactory
  @ImportStatic(SClass.class)
  public abstract static class NamePrim extends UnaryExpressionNode {
    @Specialization(guards = "isSClass(receiver)")
    public final SAbstractObject doSClass(final DynamicObjectBasic receiver) {
      CompilerAsserts.neverPartOfCompilation("Class>>NamePrim");
      return SClass.getName(receiver);
    }
  }

  @GenerateNodeFactory
  @ImportStatic(SClass.class)
  public abstract static class SuperClassPrim extends UnaryExpressionNode {
    @Specialization(guards = "isSClass(receiver)")
    public final Object doSClass(final DynamicObjectBasic receiver) {
      CompilerAsserts.neverPartOfCompilation("Class>>SuperClassPrim");
      return SClass.getSuperClass(receiver);
    }
  }


  @GenerateNodeFactory
  @ImportStatic(SClass.class)
  public abstract static class InstanceInvokablesPrim extends UnaryExpressionNode {
    @TruffleBoundary
    @Specialization(guards = "isSClass(receiver)")
    public final SArray doSClass(final DynamicObjectBasic receiver) {
      CompilerAsserts.neverPartOfCompilation("Class>>InstanceInvokablesPrim");
      return SClass.getInstanceInvokables(receiver);
    }
  }

  @GenerateNodeFactory
  @ImportStatic(SClass.class)
  public abstract static class InstanceFieldsPrim extends UnaryExpressionNode {
    @Specialization(guards = "isSClass(receiver)")
    public final SArray doSClass(final DynamicObjectBasic receiver) {
      CompilerAsserts.neverPartOfCompilation("Class>>instanceFields");
      return SClass.getInstanceFields(receiver);
    }
  }
}
