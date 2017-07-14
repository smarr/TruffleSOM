package som.primitives.basics;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.primitives.Primitive;
import som.vm.Universe;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SSymbol;


public class StringPrims {

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "concatenate:")
  public abstract static class ConcatPrim extends BinaryExpressionNode {
    public ConcatPrim(final SourceSection source) {
      super(source);
    }

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
  @Primitive(className = "String", primitive = "asSymbol", requiresContext = true)
  public abstract static class AsSymbolPrim extends UnaryExpressionNode {
    private final Universe universe;

    public AsSymbolPrim(final SourceSection source, final Universe universe) {
      super(source);
      this.universe = universe;
    }

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
    public SubstringPrim(final SourceSection sourceSection) {
      super(sourceSection);
    }

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
