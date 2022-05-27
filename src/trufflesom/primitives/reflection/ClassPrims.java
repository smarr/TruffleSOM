package trufflesom.primitives.reflection;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SClass;


public class ClassPrims {

  @GenerateNodeFactory
  @Primitive(className = "Class", primitive = "name")
  public abstract static class NamePrim extends UnaryExpressionNode {
    @Specialization
    public final SAbstractObject doSClass(final SClass receiver) {
      return receiver.getName();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Class", primitive = "superclass")
  public abstract static class SuperClassPrim extends UnaryExpressionNode {
    @Specialization
    public final SAbstractObject doSClass(final SClass receiver) {
      return receiver.getSuperClass();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Class", primitive = "methods")
  public abstract static class InstanceInvokablesPrim extends UnaryExpressionNode {
    @Specialization
    public final SArray doSClass(final SClass receiver) {
      return receiver.getInstanceInvokables();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Class", primitive = "fields")
  public abstract static class InstanceFieldsPrim extends UnaryExpressionNode {
    @Specialization
    public final SArray doSClass(final SClass receiver) {
      return receiver.getInstanceFields();
    }
  }
}
