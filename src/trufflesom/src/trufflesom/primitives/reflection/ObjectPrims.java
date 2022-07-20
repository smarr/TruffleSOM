package trufflesom.primitives.reflection;

import static trufflesom.vm.Classes.arrayClass;
import static trufflesom.vm.Classes.doubleClass;
import static trufflesom.vm.Classes.falseClass;
import static trufflesom.vm.Classes.integerClass;
import static trufflesom.vm.Classes.methodClass;
import static trufflesom.vm.Classes.primitiveClass;
import static trufflesom.vm.Classes.stringClass;
import static trufflesom.vm.Classes.symbolClass;
import static trufflesom.vm.Classes.trueClass;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.interpreter.nodes.nary.TernaryExpressionNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
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
  @Proxyable
  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "instVarAt:", selector = "instVarAt:")
  public abstract static class InstVarAtPrim extends BinaryMsgExprNode {
    @Child private IndexDispatch dispatch = IndexDispatch.create();

    @Specialization
    public static final Object doSObject(final SObject receiver, final long idx,
        @Cached final IndexDispatch dispatch) {
      return dispatch.executeDispatch(receiver, (int) idx - 1);
    }

    @Override
    public final Object executeEvaluated(final VirtualFrame frame,
        final Object receiver, final Object firstArg) {
      assert receiver instanceof SObject;
      assert firstArg instanceof Long;

      SObject rcvr = (SObject) receiver;
      long idx = (long) firstArg;
      return doSObject(rcvr, idx, dispatch);
    }

    @Override
    public final SSymbol getSelector() {
      return SymbolTable.symbolFor("instVarAt:");
    }

    @Override
    public void constructOperation(final OpBuilder opBuilder) {
      opBuilder.dsl.beginInstVarAtPrim();
      getReceiver().accept(opBuilder);
      getArgument().accept(opBuilder);
      opBuilder.dsl.endInstVarAtPrim();
    }
  }

  @Proxyable
  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "instVarAt:put:", selector = "instVarAt:put:")
  public abstract static class InstVarAtPutPrim extends TernaryExpressionNode {
    @Child private IndexDispatch dispatch = IndexDispatch.create();

    @Specialization
    public static final Object doSObject(final SObject receiver, final long idx,
        final Object val,
        @Cached final IndexDispatch dispatch) {
      dispatch.executeDispatch(receiver, (int) idx - 1, val);
      return receiver;
    }

    @Override
    public final Object executeEvaluated(final VirtualFrame frame,
        final Object receiver, final Object firstArg, final Object secondArg) {
      assert receiver instanceof SObject;
      assert firstArg instanceof Long;
      assert secondArg != null;

      SObject rcvr = (SObject) receiver;
      long idx = (long) firstArg;
      return doSObject(rcvr, idx, secondArg, dispatch);
    }

    @Override
    public void constructOperation(final OpBuilder opBuilder) {
      opBuilder.dsl.beginInstVarAtPutPrim();
      getReceiver().accept(opBuilder);
      getArg1().accept(opBuilder);
      getArg2().accept(opBuilder);
      opBuilder.dsl.endInstVarAtPutPrim();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "instVarNamed:", selector = "instVarNamed:")
  public abstract static class InstVarNamedPrim extends BinaryMsgExprNode {
    @Override
    public final SSymbol getSelector() {
      return SymbolTable.symbolFor("instVarNamed:");
    }

    @Specialization
    @TruffleBoundary
    public static final Object doSObject(final SObject receiver, final SSymbol fieldName) {
      CompilerAsserts.neverPartOfCompilation();
      return receiver.getField(receiver.getFieldIndex(fieldName));
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "halt")
  public abstract static class HaltPrim extends UnaryExpressionNode {
    @Specialization
    public static final Object doSAbstractObject(final Object receiver) {
      Universe.errorPrintln("BREAKPOINT");
      return receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "class")
  @SuppressWarnings("unused")
  public abstract static class ClassPrim extends UnaryExpressionNode {

    public abstract SClass executeEvaluated(Object rcvr);

    @Specialization
    public static final SClass getSomClass(@SuppressWarnings("unused") final SArray receiver) {
      return arrayClass;
    }

    @Specialization
    public static final SClass getSomClass(final SBlock receiver) {
      return receiver.getSOMClass();
    }

    @Specialization
    public static final SClass getSomClass(final SObject receiver) {
      return receiver.getSOMClass();
    }

    @Specialization
    public static final SClass getSomClass(
        @SuppressWarnings("unused") final SMethod receiver) {
      return methodClass;
    }

    @Specialization
    public static final SClass getSomClass(
        @SuppressWarnings("unused") final SPrimitive receiver) {
      return primitiveClass;
    }

    @Specialization
    public static final SClass getSomClass(
        @SuppressWarnings("unused") final SSymbol receiver) {
      return symbolClass;
    }

    @Specialization(guards = "receiver")
    public static final SClass getTrueClass(
        @SuppressWarnings("unused") final boolean receiver) {
      return trueClass;
    }

    @Specialization(guards = "!receiver")
    public static final SClass getFalseClass(
        @SuppressWarnings("unused") final boolean receiver) {
      return falseClass;
    }

    @Specialization
    public static final SClass getSomClass(@SuppressWarnings("unused") final long receiver) {
      return integerClass;
    }

    @Specialization
    public static final SClass getSomClass(
        @SuppressWarnings("unused") final BigInteger receiver) {
      return integerClass;
    }

    @Specialization
    public static final SClass getSomClass(@SuppressWarnings("unused") final String receiver) {
      return stringClass;
    }

    @TruffleBoundary
    @Specialization
    public static final SClass getSomClass(@SuppressWarnings("unused") final double receiver) {
      return doubleClass;
    }
  }

  @Proxyable
  @GenerateNodeFactory
  @Primitive(selector = "isNil")
  public abstract static class IsNilNode extends UnaryExpressionNode {
    @Specialization
    public static final boolean isNil(final Object receiver) {
      return receiver == Nil.nilObject;
    }

    @Override
    public void constructOperation(final OpBuilder opBuilder) {
      opBuilder.dsl.beginIsNil();
      getReceiver().accept(opBuilder);
      opBuilder.dsl.endIsNil();
    }
  }

  @Proxyable
  @GenerateNodeFactory
  @Primitive(selector = "notNil")
  public abstract static class NotNilNode extends UnaryExpressionNode {
    @Specialization
    public static final boolean notNil(final Object receiver) {
      return receiver != Nil.nilObject;
    }

    @Override
    public void constructOperation(final OpBuilder opBuilder) {
      opBuilder.dsl.beginNotNil();
      getReceiver().accept(opBuilder);
      opBuilder.dsl.endNotNil();
    }
  }
}
