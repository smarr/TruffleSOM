package trufflesom.primitives.reflection;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.primitives.Primitive;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode.BinarySystemOperation;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.interpreter.nodes.nary.TernaryExpressionNode.TernarySystemOperation;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode.UnarySystemOperation;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.Universe;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SInvokable.SPrimitive;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public final class ObjectPrims {

  @Primitive(className = "Object", primitive = "instVarAt:", selector = "instVarAt:",
      noWrapper = true)
  public abstract static class InstVarAtPrim extends BinarySystemOperation {
    @Child private IndexDispatch dispatch;

    @Override
    public BinarySystemOperation initialize(final Universe universe) {
      super.initialize(universe);
      dispatch = IndexDispatch.create(universe);
      return this;
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

    @Fallback
    public final Object makeGenericSend(final VirtualFrame frame,
        final Object receiver, final Object argument) {
      return makeGenericSend().doPreEvaluated(frame,
          new Object[] {receiver, argument});
    }

    private GenericMessageSendNode makeGenericSend() {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      GenericMessageSendNode node =
          MessageSendNode.createGeneric(SymbolTable.symbolFor("instVarAt:"),
              new ExpressionNode[] {getReceiver(), getArgument()}, sourceSection,
              SomLanguage.getCurrentContext());
      return replace(node);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "instVarAt:put:", selector = "instVarAt:put:")
  public abstract static class InstVarAtPutPrim extends TernarySystemOperation {
    @Child private IndexDispatch dispatch;

    @Override
    public TernarySystemOperation initialize(final Universe universe) {
      super.initialize(universe);
      dispatch = IndexDispatch.create(universe);
      return this;
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
  public abstract static class InstVarNamedPrim extends BinaryMsgExprNode {
    @Override
    public SSymbol getSelector() {
      return SymbolTable.symbolFor("instVarNamed:");
    }

    @Specialization
    @TruffleBoundary
    public final Object doSObject(final SObject receiver, final SSymbol fieldName) {
      CompilerAsserts.neverPartOfCompilation();
      return receiver.getField(receiver.getFieldIndex(fieldName));
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "halt")
  public abstract static class HaltPrim extends UnaryExpressionNode {
    @Specialization
    public final Object doSAbstractObject(final Object receiver) {
      Universe.errorPrintln("BREAKPOINT");
      return receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "class")
  public abstract static class ClassPrim extends UnarySystemOperation {

    public abstract SClass executeEvaluated(Object rcvr);

    @Specialization
    public final SClass getSomClass(final SArray receiver) {
      return universe.arrayClass;
    }

    @Specialization
    public final SClass getSomClass(final SBlock receiver) {
      return receiver.getSOMClass();
    }

    @Specialization
    public final SClass getSomClass(final SObject receiver) {
      return receiver.getSOMClass();
    }

    @Specialization
    public final SClass getSomClass(final SMethod receiver) {
      return universe.methodClass;
    }

    @Specialization
    public final SClass getSomClass(final SPrimitive receiver) {
      return universe.primitiveClass;
    }

    @Specialization
    public final SClass getSomClass(final SSymbol receiver) {
      return universe.symbolClass;
    }

    @Specialization(guards = "receiver")
    public final SClass getTrueClass(final boolean receiver) {
      return universe.getTrueClass();
    }

    @Specialization(guards = "!receiver")
    public final SClass getFalseClass(final boolean receiver) {
      return universe.getFalseClass();
    }

    @Specialization
    public final SClass getSomClass(final long receiver) {
      return universe.integerClass;
    }

    @Specialization
    public final SClass getSomClass(final BigInteger receiver) {
      return universe.integerClass;
    }

    @Specialization
    public final SClass getSomClass(final String receiver) {
      return universe.stringClass;
    }

    @TruffleBoundary
    @Specialization
    public final SClass getSomClass(final double receiver) {
      return universe.doubleClass;
    }
  }

  @GenerateNodeFactory
  @Primitive(selector = "isNil", noWrapper = true)
  public abstract static class IsNilNode extends UnaryExpressionNode {
    @Specialization
    public final boolean isNil(final Object receiver) {
      return receiver == Nil.nilObject;
    }
  }

  @GenerateNodeFactory
  @Primitive(selector = "notNil", noWrapper = true)
  public abstract static class NotNilNode extends UnaryExpressionNode {
    @Specialization
    public final boolean notNil(final Object receiver) {
      return receiver != Nil.nilObject;
    }
  }
}
