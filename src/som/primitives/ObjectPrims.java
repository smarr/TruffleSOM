package som.primitives;

import som.interpreter.Types;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.primitives.reflection.IndexDispatch;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SObject;
import som.vmobjects.SReflectiveObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.object.basic.DynamicObjectBasic;


public final class ObjectPrims {

  @GenerateNodeFactory
  public abstract static class InstVarAtPrim extends BinaryExpressionNode {

    @Child private IndexDispatch dispatch;

    public InstVarAtPrim() {
      super();
      dispatch = IndexDispatch.create();
    }
    public InstVarAtPrim(final InstVarAtPrim node) { this(); }

    @Specialization
    public final Object doSObject(final DynamicObjectBasic receiver, final long idx) {
      return dispatch.executeDispatch(receiver, (int) idx - 1);
    }

    @Override
    public final Object executeEvaluated(final VirtualFrame frame,
      final Object receiver, final Object firstArg) {
      assert receiver instanceof DynamicObjectBasic;
      assert firstArg instanceof Long;

      DynamicObjectBasic rcvr = (DynamicObjectBasic) receiver;
      long idx     = (long) firstArg;
      return doSObject(rcvr, idx);
    }
  }

  @GenerateNodeFactory
  public abstract static class InstVarAtPutPrim extends TernaryExpressionNode {
    @Child private IndexDispatch dispatch;

    public InstVarAtPutPrim() {
      super();
      dispatch = IndexDispatch.create();
    }
    public InstVarAtPutPrim(final InstVarAtPutPrim node) { this(); }

    @Specialization
    public final Object doSObject(final DynamicObjectBasic receiver, final long idx, final Object val) {
      dispatch.executeDispatch(receiver, (int) idx - 1, val);
      return val;
    }

    @Override
    public final Object executeEvaluated(final VirtualFrame frame,
      final Object receiver, final Object firstArg, final Object secondArg) {
      assert receiver instanceof DynamicObjectBasic;
      assert firstArg instanceof Long;
      assert secondArg != null;

      DynamicObjectBasic rcvr = (DynamicObjectBasic) receiver;
      long idx     = (long) firstArg;
      return doSObject(rcvr, idx, secondArg);
    }
  }

  @GenerateNodeFactory
  public abstract static class InstVarNamedPrim extends BinaryExpressionNode {
    @Specialization
    public final Object doSObject(final DynamicObjectBasic receiver, final SSymbol fieldName) {
      CompilerAsserts.neverPartOfCompilation("InstVarNamedPrim");
      return receiver.get(SObject.getFieldIndex(receiver, fieldName), Nil.nilObject);
    }
  }

  @GenerateNodeFactory
  public abstract static class HaltPrim extends UnaryExpressionNode {
    public HaltPrim() { super(null); }
    @Specialization
    public final Object doSAbstractObject(final Object receiver) {
      Universe.errorPrintln("BREAKPOINT");
      return receiver;
    }
  }

  @GenerateNodeFactory
  public abstract static class ClassPrim extends UnaryExpressionNode {
    @Specialization
    public final DynamicObjectBasic doSAbstractObject(final SAbstractObject receiver) {
      return receiver.getSOMClass();
    }

    @Specialization
    public final DynamicObjectBasic doDynamicObjectBasic(final DynamicObjectBasic receiver) {
      return SObject.getSOMClass(receiver);
    }

    @Specialization
    public final DynamicObjectBasic doObject(final Object receiver) {
      return Types.getClassOf(receiver);
    }
  }

  @GenerateNodeFactory
  public abstract static class installEnvironmentPrim extends BinaryExpressionNode {
    @Specialization
    public final Object doSObject(final DynamicObjectBasic receiver, final DynamicObjectBasic environment) {
      SReflectiveObject.setEnvironment(receiver, environment);
      return receiver;
    }
  }
}