package som.primitives;

import som.interpreter.Types;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SClass;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Specialization;


public final class ObjectPrims {
  public abstract static class InstVarAtPrim extends BinaryExpressionNode {

    public InstVarAtPrim() {
      super();
    }
    public InstVarAtPrim(final InstVarAtPrim node) { this(); }

    @Specialization
    public final Object doSObject(final SObject receiver, final long idx) {
      return receiver.getField((int) idx - 1);
    }
  }

  public abstract static class InstVarAtPutPrim extends TernaryExpressionNode {
    @Specialization
    public final Object doSObject(final SObject receiver, final long idx, final Object val) {
      receiver.setField((int) idx - 1, val);
      return val;
    }
  }

  public abstract static class InstVarNamedPrim extends BinaryExpressionNode {
    @Specialization
    public final Object doSObject(final SObject receiver, final SSymbol fieldName) {
      CompilerAsserts.neverPartOfCompilation();
      return receiver.getField(receiver.getFieldIndex(fieldName));
    }
  }

  public abstract static class HaltPrim extends UnaryExpressionNode {
    public HaltPrim() { super(null); }
    @Specialization
    public final Object doSAbstractObject(final Object receiver) {
      Universe.errorPrintln("BREAKPOINT");
      return receiver;
    }
  }

  public abstract static class ClassPrim extends UnaryExpressionNode {
    @Specialization
    public final SClass doSAbstractObject(final SAbstractObject receiver) {
      return receiver.getSOMClass();
    }

    @Specialization
    public final SClass doObject(final Object receiver) {
      return Types.getClassOf(receiver);
    }
  }
}
