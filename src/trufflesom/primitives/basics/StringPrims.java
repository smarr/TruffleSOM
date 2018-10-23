package trufflesom.primitives.basics;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.interpreter.nodes.nary.TernaryExpressionNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode.UnarySystemOperation;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SSymbol;


public class StringPrims {

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "concatenate:")
  public abstract static class ConcatPrim extends BinaryExpressionNode {
    @Specialization
    public final String doString(final String receiver, final String argument) {
      return receiver + argument;
    }

    @Specialization
    public final String doString(final String receiver, final SSymbol argument) {
      return receiver + argument.getString();
    }

    @Specialization
    public final String doSSymbol(final SSymbol receiver, final String argument) {
      return receiver.getString() + argument;
    }

    @Specialization
    public final String doSSymbol(final SSymbol receiver, final SSymbol argument) {
      return receiver.getString() + argument.getString();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "asSymbol")
  public abstract static class AsSymbolPrim extends UnarySystemOperation {
    @Specialization
    public final SAbstractObject doString(final String receiver) {
      return universe.symbolFor(receiver);
    }

    @Specialization
    public final SAbstractObject doSSymbol(final SSymbol receiver) {
      return receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "primSubstringFrom:to:")
  public abstract static class SubstringPrim extends TernaryExpressionNode {
    @Specialization
    public final String doString(final String receiver, final long start,
        final long end) {
      try {
        return receiver.substring((int) start - 1, (int) end);
      } catch (IndexOutOfBoundsException e) {
        return "Error - index out of bounds";
      }
    }

    @Specialization
    public final String doSSymbol(final SSymbol receiver, final long start,
        final long end) {
      return doString(receiver.getString(), start, end);
    }
  }
}
