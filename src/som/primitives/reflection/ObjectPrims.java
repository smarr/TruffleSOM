package som.primitives.reflection;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.Types;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.primitives.basics.SystemPrims;
import som.primitives.basics.SystemPrims.BinarySystemNode;
import som.primitives.basics.SystemPrims.UnarySystemNode;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SClass;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;


public final class ObjectPrims {

  @GenerateNodeFactory
  public abstract static class InstVarAtPrim extends BinarySystemNode {

    @Child private IndexDispatch dispatch;

    public InstVarAtPrim(final Universe universe) {
      super(universe);
      dispatch = IndexDispatch.create(universe);
    }

    public InstVarAtPrim(final InstVarAtPrim node) {
      this(node.universe);
    }

    @Specialization
    public final Object doSObject(final SObject receiver, final long idx) {
      return dispatch.executeDispatch(receiver, (int) idx - 1);
    }

    @Override
    public final Object executeEvaluated(final VirtualFrame frame,
        final Object receiver, final Object firstArg) {
      assert receiver instanceof SObject;
      assert firstArg instanceof Long;

      SObject rcvr = (SObject) receiver;
      long idx = (long) firstArg;
      return doSObject(rcvr, idx);
    }
  }

  @GenerateNodeFactory
  public abstract static class InstVarAtPutPrim extends TernaryExpressionNode {
    @Child private IndexDispatch dispatch;
    private final Universe       universe;

    public InstVarAtPutPrim(final Universe universe) {
      super();
      dispatch = IndexDispatch.create(universe);
      this.universe = universe;
    }

    public InstVarAtPutPrim(final InstVarAtPutPrim node) {
      this(node.universe);
    }

    @Specialization
    public final Object doSObject(final SObject receiver, final long idx, final Object val) {
      dispatch.executeDispatch(receiver, (int) idx - 1, val);
      return val;
    }

    @Override
    public final Object executeEvaluated(final VirtualFrame frame,
        final Object receiver, final Object firstArg, final Object secondArg) {
      assert receiver instanceof SObject;
      assert firstArg instanceof Long;
      assert secondArg != null;

      SObject rcvr = (SObject) receiver;
      long idx = (long) firstArg;
      return doSObject(rcvr, idx, secondArg);
    }
  }

  @GenerateNodeFactory
  public abstract static class InstVarNamedPrim extends BinaryExpressionNode {
    @Specialization
    public final Object doSObject(final SObject receiver, final SSymbol fieldName) {
      CompilerAsserts.neverPartOfCompilation();
      return receiver.getField(receiver.getFieldIndex(fieldName));
    }
  }

  @GenerateNodeFactory
  public abstract static class HaltPrim extends UnaryExpressionNode {
    public HaltPrim() {
      super(null);
    }

    @Specialization
    public final Object doSAbstractObject(final Object receiver) {
      Universe.errorPrintln("BREAKPOINT");
      return receiver;
    }
  }

  @GenerateNodeFactory
  public abstract static class ClassPrim extends UnarySystemNode {
    public ClassPrim(final Universe universe) {
      super(universe);
    }

    @Specialization
    public final SClass doSAbstractObject(final SAbstractObject receiver) {
      return receiver.getSOMClass(universe);
    }

    @Specialization
    public final SClass doObject(final Object receiver) {
      return Types.getClassOf(receiver, universe);
    }
  }

  public abstract static class IsNilNode extends UnaryExpressionNode {
    @Specialization
    public final boolean isNil(final Object receiver) {
      return receiver == Nil.nilObject;
    }
  }

  public abstract static class NotNilNode extends UnaryExpressionNode {
    @Specialization
    public final boolean isNil(final Object receiver) {
      return receiver != Nil.nilObject;
    }
  }
}
