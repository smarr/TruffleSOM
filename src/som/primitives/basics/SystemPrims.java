package som.primitives.basics;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vmobjects.SClass;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;


public final class SystemPrims {

  @GenerateNodeFactory
  public abstract static class BinarySystemNode extends BinaryExpressionNode {
    protected final Universe universe;

    protected BinarySystemNode(final Universe universe) {
      super(null);
      this.universe = universe;
    }
  }

  public abstract static class UnarySystemNode extends UnaryExpressionNode {
    protected final Universe universe;

    protected UnarySystemNode(final Universe universe) {
      super(null);
      this.universe = universe;
    }
  }

  public abstract static class LoadPrim extends BinarySystemNode {
    public LoadPrim(final Universe universe) {
      super(universe);
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final SSymbol argument) {
      SClass result = universe.loadClass(argument);
      return result != null ? result : Nil.nilObject;
    }
  }

  public abstract static class ExitPrim extends BinarySystemNode {
    public ExitPrim(final Universe universe) {
      super(universe);
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final long error) {
      universe.exit((int) error);
      return receiver;
    }
  }

  @GenerateNodeFactory
  public abstract static class GlobalPutPrim extends TernaryExpressionNode {
    protected final Universe universe;

    public GlobalPutPrim(final Universe universe) {
      this.universe = universe;
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final SSymbol global,
        final Object value) {
      universe.setGlobal(global, value);
      return value;
    }
  }

  public abstract static class PrintStringPrim extends BinarySystemNode {
    public PrintStringPrim(final Universe universe) {
      super(universe);
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final String argument) {
      Universe.print(argument);
      return receiver;
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final SSymbol argument) {
      return doSObject(receiver, argument.getString());
    }
  }

  @GenerateNodeFactory
  public abstract static class PrintNewlinePrim extends UnarySystemNode {
    public PrintNewlinePrim(final Universe universe) {
      super(universe);
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver) {
      Universe.println();
      return receiver;
    }
  }

  @GenerateNodeFactory
  public abstract static class FullGCPrim extends UnarySystemNode {
    public FullGCPrim(final Universe universe) {
      super(universe);
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver) {
      System.gc();
      return true;
    }
  }

  @GenerateNodeFactory
  public abstract static class TimePrim extends UnarySystemNode {
    public TimePrim(final Universe universe) {
      super(universe);
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final long doSObject(final SObject receiver) {
      return System.currentTimeMillis() - startTime;
    }
  }

  @GenerateNodeFactory
  public abstract static class TicksPrim extends UnarySystemNode {
    public TicksPrim(final Universe universe) {
      super(universe);
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final long doSObject(final SObject receiver) {
      return System.nanoTime() / 1000L - startMicroTime;
    }
  }

  {
    startMicroTime = System.nanoTime() / 1000L;
    startTime = startMicroTime / 1000L;
  }
  private static long startTime;
  private static long startMicroTime;
}
