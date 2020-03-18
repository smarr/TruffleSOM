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

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "isWhiteSpace")
  public abstract static class IsWhiteSpacePrim extends UnarySystemOperation {
    @Specialization(guards = "receiver.length() == 1")
    public final boolean doChar(final String receiver) {
      return Character.isWhitespace(receiver.charAt(0));
    }

    @Specialization(guards = "receiver.getString().length() == 1")
    public final boolean doChar(final SSymbol receiver) {
      return doChar(receiver.getString());
    }

    @Specialization(guards = "receiver.length() != 1")
    public final boolean doString(final String receiver) {
      for (int i = 0; i < receiver.length(); i++) {
        if (!Character.isWhitespace(receiver.charAt(i))) {
          return false;
        }
      }

      if (receiver.length() > 0) {
        return true;
      } else {
        return false;
      }
    }

    @Specialization(guards = "receiver.getString().length() != 1")
    public final boolean doSSymbol(final SSymbol receiver) {
      return doString(receiver.getString());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "isLetters")
  public abstract static class IsLettersPrim extends UnarySystemOperation {
    @Specialization(guards = "receiver.length() == 1")
    public final boolean doChar(final String receiver) {
      return Character.isLetter(receiver.charAt(0));
    }

    @Specialization(guards = "receiver.getString().length() == 1")
    public final boolean doChar(final SSymbol receiver) {
      return doChar(receiver.getString());
    }

    @Specialization(guards = "receiver.length() != 1")
    public final boolean doString(final String receiver) {
      for (int i = 0; i < receiver.length(); i++) {
        if (!Character.isLetter(receiver.charAt(i))) {
          return false;
        }
      }

      if (receiver.length() > 0) {
        return true;
      } else {
        return false;
      }
    }

    @Specialization(guards = "receiver.getString().length() != 1")
    public final boolean doSSymbol(final SSymbol receiver) {
      return doString(receiver.getString());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "isDigits")
  public abstract static class IsDigitsPrim extends UnarySystemOperation {
    @Specialization(guards = "receiver.length() == 1")
    public final boolean doChar(final String receiver) {
      return Character.isDigit(receiver.charAt(0));
    }

    @Specialization(guards = "receiver.getString().length() == 1")
    public final boolean doChar(final SSymbol receiver) {
      return doChar(receiver.getString());
    }

    @Specialization(guards = "receiver.length() != 1")
    public final boolean doString(final String receiver) {
      for (int i = 0; i < receiver.length(); i++) {
        if (!Character.isDigit(receiver.charAt(i))) {
          return false;
        }
      }

      if (receiver.length() > 0) {
        return true;
      } else {
        return false;
      }
    }

    @Specialization(guards = "receiver.getString().length() != 1")
    public final boolean doSSymbol(final SSymbol receiver) {
      return doString(receiver.getString());
    }
  }
}
