package som.primitives.basics;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.primitives.Primitive;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vmobjects.SClass;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;


public final class SystemPrims {

  @GenerateNodeFactory
  public abstract static class BinarySystemNode extends BinaryExpressionNode {
    protected final Universe universe;

    protected BinarySystemNode(final SourceSection source, final Universe universe) {
      super(source);
      this.universe = universe;
    }
  }

  public abstract static class UnarySystemNode extends UnaryExpressionNode {
    protected final Universe universe;

    protected UnarySystemNode(final SourceSection source, final Universe universe) {
      super(source);
      this.universe = universe;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "load:", requiresContext = true)
  public abstract static class LoadPrim extends BinarySystemNode {
    public LoadPrim(final SourceSection source, final Universe universe) {
      super(source, universe);
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final SSymbol argument) {
      SClass result = universe.loadClass(argument);
      return result != null ? result : Nil.nilObject;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "exit:", requiresContext = true)
  public abstract static class ExitPrim extends BinarySystemNode {
    public ExitPrim(final SourceSection source, final Universe universe) {
      super(source, universe);
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final long error) {
      universe.exit((int) error);
      return receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "global:put:", requiresContext = true)
  public abstract static class GlobalPutPrim extends TernaryExpressionNode {
    protected final Universe universe;

    public GlobalPutPrim(final SourceSection source, final Universe universe) {
      super(source);
      this.universe = universe;
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final SSymbol global,
        final Object value) {
      universe.setGlobal(global, value);
      return value;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "printString:", requiresContext = true)
  public abstract static class PrintStringPrim extends BinarySystemNode {
    public PrintStringPrim(final SourceSection source, final Universe universe) {
      super(source, universe);
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
  @Primitive(className = "System", primitive = "printNewline", requiresContext = true)
  public abstract static class PrintNewlinePrim extends UnarySystemNode {
    public PrintNewlinePrim(final SourceSection source, final Universe universe) {
      super(source, universe);
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver) {
      Universe.println();
      return receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "fullGC", requiresContext = true)
  public abstract static class FullGCPrim extends UnarySystemNode {
    public FullGCPrim(final SourceSection source, final Universe universe) {
      super(source, universe);
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver) {
      System.gc();
      return true;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "time", requiresContext = true)
  public abstract static class TimePrim extends UnarySystemNode {
    public TimePrim(final SourceSection source, final Universe universe) {
      super(source, universe);
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final long doSObject(final SObject receiver) {
      return System.currentTimeMillis() - startTime;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "ticks", requiresContext = true)
  public abstract static class TicksPrim extends UnarySystemNode {
    public TicksPrim(final SourceSection source, final Universe universe) {
      super(source, universe);
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
