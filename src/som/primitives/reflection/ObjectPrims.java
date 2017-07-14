package som.primitives.reflection;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Primitive;
import som.interpreter.Types;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.primitives.basics.SystemPrims.BinarySystemNode;
import som.primitives.basics.SystemPrims.UnarySystemNode;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SClass;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;


public final class ObjectPrims {

  @Primitive(className = "Object", primitive = "instVarAt:", selector = "instVarAt:",
      requiresContext = true)
  public abstract static class InstVarAtPrim extends BinarySystemNode {

    @Child private IndexDispatch dispatch;

    public InstVarAtPrim(final SourceSection source, final Universe universe) {
      super(source, universe);
      dispatch = IndexDispatch.create(universe);
    }

    public InstVarAtPrim(final InstVarAtPrim node) {
      this(node.sourceSection, node.universe);
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
  @Primitive(className = "Object", primitive = "instVarAt:put:", selector = "instVarAt:put:",
      requiresContext = true)
  public abstract static class InstVarAtPutPrim extends TernaryExpressionNode {
    @Child private IndexDispatch dispatch;
    private final Universe       universe;

    public InstVarAtPutPrim(final SourceSection source, final Universe universe) {
      super(source);
      dispatch = IndexDispatch.create(universe);
      this.universe = universe;
    }

    public InstVarAtPutPrim(final InstVarAtPutPrim node) {
      this(node.sourceSection, node.universe);
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
  @Primitive(className = "Object", primitive = "instVarNamed:", selector = "instVarNamed:")
  public abstract static class InstVarNamedPrim extends BinaryExpressionNode {
    public InstVarNamedPrim(final SourceSection source) {
      super(source);
    }

    @Specialization
    public final Object doSObject(final SObject receiver, final SSymbol fieldName) {
      CompilerAsserts.neverPartOfCompilation();
      return receiver.getField(receiver.getFieldIndex(fieldName));
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "halt")
  public abstract static class HaltPrim extends UnaryExpressionNode {
    public HaltPrim(final SourceSection source) {
      super(source);
    }

    @Specialization
    public final Object doSAbstractObject(final Object receiver) {
      Universe.errorPrintln("BREAKPOINT");
      return receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "class", requiresContext = true)
  public abstract static class ClassPrim extends UnarySystemNode {
    public ClassPrim(final SourceSection source, final Universe universe) {
      super(source, universe);
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

  @GenerateNodeFactory
  @Primitive(selector = "isNil", noWrapper = true)
  public abstract static class IsNilNode extends UnaryExpressionNode {
    public IsNilNode(final SourceSection source) {
      super(source);
    }

    @Specialization
    public final boolean isNil(final Object receiver) {
      return receiver == Nil.nilObject;
    }
  }

  @GenerateNodeFactory
  @Primitive(selector = "notNil", noWrapper = true)
  public abstract static class NotNilNode extends UnaryExpressionNode {
    public NotNilNode(final SourceSection source) {
      super(source);
    }

    @Specialization
    public final boolean notNil(final Object receiver) {
      return receiver != Nil.nilObject;
    }
  }
}
