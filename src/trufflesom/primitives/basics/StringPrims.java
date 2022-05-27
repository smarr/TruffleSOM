package trufflesom.primitives.basics;

import static trufflesom.vm.SymbolTable.symbolFor;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.interpreter.nodes.nary.TernaryExpressionNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SSymbol;


public class StringPrims {

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "concatenate:")
  public abstract static class ConcatPrim extends BinaryMsgExprNode {
    @Override
    public SSymbol getSelector() {
      return SymbolTable.symbolFor("concatenate:");
    }

    @Specialization
    @TruffleBoundary
    public final String doString(final String receiver, final String argument) {
      return receiver + argument;
    }

    @Specialization
    @TruffleBoundary
    public final String doString(final String receiver, final SSymbol argument) {
      return receiver + argument.getString();
    }

    @Specialization
    @TruffleBoundary
    public final String doSSymbol(final SSymbol receiver, final String argument) {
      return receiver.getString() + argument;
    }

    @Specialization
    @TruffleBoundary
    public final String doSSymbol(final SSymbol receiver, final SSymbol argument) {
      return receiver.getString() + argument.getString();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "charAt:", selector = "charAt:")
  public abstract static class CharAtPrim extends BinaryMsgExprNode {

    @CompilationFinal private boolean branchTaken;

    @Override
    public SSymbol getSelector() {
      return SymbolTable.symbolFor("charAt:");
    }

    @Specialization
    public final String doString(final String receiver, final long idx) {
      int index = (int) idx;
      if (0 < index && index <= receiver.length()) {
        return receiver.substring(index - 1, index);
      }

      if (!branchTaken) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        branchTaken = true;
      }
      return "Error - index out of bounds";
    }

    @Specialization
    public final String doSSymbol(final SSymbol receiver, final long idx) {
      int index = (int) idx;
      String s = receiver.getString();
      if (0 < index && index <= s.length()) {
        return s.substring(index - 1, index);
      }

      if (!branchTaken) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        branchTaken = true;
      }
      return "Error - index out of bounds";
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "asSymbol")
  public abstract static class AsSymbolPrim extends UnaryExpressionNode {
    @Specialization
    public final SAbstractObject doString(final String receiver) {
      return symbolFor(receiver);
    }

    @Specialization
    public final SAbstractObject doSSymbol(final SSymbol receiver) {
      return receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "primSubstringFrom:to:",
      selector = "primSubstringFrom:to:")
  public abstract static class SubstringPrim extends TernaryExpressionNode {
    @Specialization
    public final String doString(final String receiver, final long start,
        final long end) {
      try {
        return receiver.substring((int) start - 1, (int) end);
      } catch (StringIndexOutOfBoundsException e) {
        return "Error - index out of bounds";
      }
    }

    @Specialization
    public final String doSSymbol(final SSymbol receiver, final long start,
        final long end) {
      return doString(receiver.getString(), start, end);
    }
  }

  /**
   * Not normally a primitive, but seems the best place. It qualifies as an uber node, but is a
   * core class, soo...
   *
   * <pre>
   * substringFrom: start to: end = (
        ((end <= self length) && (start > 0) && (start <= end))
            ifTrue: [^self primSubstringFrom: start to: end]
            ifFalse: [
                self error: 'Attempting to index string out of its bounds (start: ' + start asString + ' end: ' + end asString + ' length: ' + self length asString + ')' ]
    )
   * </pre>
   */
  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "substringFrom:to:",
      selector = "substringFrom:to:")
  public abstract static class SubstringFrom extends TernaryExpressionNode {
    @CompilationFinal private boolean   indexOutOfBounds;
    @Child private AbstractDispatchNode dispatchError;

    @Specialization
    public final String doString(final VirtualFrame frame, final String receiver,
        final long start, final long end) {
      if (end <= receiver.length() && (start > 0) && (start <= end)) {
        return receiver.substring((int) start - 1, (int) end);
      } else {
        if (!indexOutOfBounds) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          dispatchError = new UninitializedDispatchNode(SymbolTable.symbolFor("error:"));
        }
        dispatchError.executeDispatch(frame,
            new Object[] {receiver, errorMessage(receiver, start, end)});
        return receiver;
      }
    }

    @TruffleBoundary
    private static String errorMessage(final String receiver, final long start,
        final long end) {
      return "Attempting to index string out of its bounds (start: " + start + " end: " + end
          + " length: " + receiver.length() + ")";
    }

    @Specialization
    public final String doSSymbol(final VirtualFrame frame, final SSymbol receiver,
        final long start, final long end) {
      return doString(frame, receiver.getString(), start, end);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "isWhiteSpace")
  public abstract static class IsWhiteSpacePrim extends UnaryExpressionNode {
    @TruffleBoundary
    @Specialization(guards = "receiver.length() == 1")
    public final boolean doChar(final String receiver) {
      return Character.isWhitespace(receiver.charAt(0));
    }

    @Specialization(guards = "receiver.getString().length() == 1")
    public final boolean doChar(final SSymbol receiver) {
      return doChar(receiver.getString());
    }

    @TruffleBoundary
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
  public abstract static class IsLettersPrim extends UnaryExpressionNode {
    @TruffleBoundary
    @Specialization(guards = "receiver.length() == 1")
    public final boolean doChar(final String receiver) {
      return Character.isLetter(receiver.charAt(0));
    }

    @Specialization(guards = "receiver.getString().length() == 1")
    public final boolean doChar(final SSymbol receiver) {
      return doChar(receiver.getString());
    }

    @TruffleBoundary
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
  public abstract static class IsDigitsPrim extends UnaryExpressionNode {
    @TruffleBoundary
    @Specialization(guards = "receiver.length() == 1")
    public final boolean doChar(final String receiver) {
      return Character.isDigit(receiver.charAt(0));
    }

    @Specialization(guards = "receiver.getString().length() == 1")
    public final boolean doChar(final SSymbol receiver) {
      return doChar(receiver.getString());
    }

    @TruffleBoundary
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
