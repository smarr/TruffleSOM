package trufflesom.primitives.reflection;

import static trufflesom.vm.Classes.arrayClass;
import static trufflesom.vm.Classes.doubleClass;
import static trufflesom.vm.Classes.integerClass;
import static trufflesom.vm.Classes.methodClass;
import static trufflesom.vm.Classes.primitiveClass;
import static trufflesom.vm.Classes.stringClass;
import static trufflesom.vm.Classes.symbolClass;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.interpreter.nodes.nary.BinarySystemMsgOperation;
import trufflesom.interpreter.nodes.nary.BinarySystemOperation;
import trufflesom.interpreter.nodes.nary.TernarySystemOperation;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.interpreter.nodes.nary.UnarySystemOperation;
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

  @Primitive(className = "Object", primitive = "instVarAt:", selector = "instVarAt:")
  public abstract static class InstVarAtPrim extends BinarySystemMsgOperation {
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

    @Override
    public SSymbol getSelector() {
      return SymbolTable.symbolFor("instVarAt:");
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
      return arrayClass;
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
      return methodClass;
    }

    @Specialization
    public final SClass getSomClass(final SPrimitive receiver) {
      return primitiveClass;
    }

    @Specialization
    public final SClass getSomClass(final SSymbol receiver) {
      return symbolClass;
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
      return integerClass;
    }

    @Specialization
    public final SClass getSomClass(final BigInteger receiver) {
      return integerClass;
    }

    @Specialization
    public final SClass getSomClass(final String receiver) {
      return stringClass;
    }

    @TruffleBoundary
    @Specialization
    public final SClass getSomClass(final double receiver) {
      return doubleClass;
    }
  }

  @GenerateNodeFactory
  @Primitive(selector = "isNil")
  public abstract static class IsNilNode extends UnaryExpressionNode {
    @Specialization
    public final boolean isNil(final Object receiver) {
      return receiver == Nil.nilObject;
    }
  }

  @GenerateNodeFactory
  @Primitive(selector = "notNil")
  public abstract static class NotNilNode extends UnaryExpressionNode {
    @Specialization
    public final boolean notNil(final Object receiver) {
      return receiver != Nil.nilObject;
    }
  }
}
