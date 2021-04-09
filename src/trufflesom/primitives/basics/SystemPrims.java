package trufflesom.primitives.basics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode.BinarySystemOperation;
import trufflesom.interpreter.nodes.nary.TernaryExpressionNode.TernarySystemOperation;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode.UnarySystemOperation;
import trufflesom.vm.Universe;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public final class SystemPrims {

  @Primitive(className = "System", primitive = "load:")
  public abstract static class LoadPrim extends BinarySystemOperation {
    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final SSymbol argument) {
      SClass result = universe.loadClass(argument);
      return result != null ? result : Nil.nilObject;
    }
  }

  @Primitive(className = "System", primitive = "exit:")
  public abstract static class ExitPrim extends BinarySystemOperation {
    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final long error) {
      universe.exit((int) error);
      return receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "global:put:")
  public abstract static class GlobalPutPrim extends TernarySystemOperation {
    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final SSymbol global,
        final Object value) {
      universe.setGlobal(global, value);
      return value;
    }
  }

  @Primitive(className = "System", primitive = "printString:")
  public abstract static class PrintStringPrim extends BinarySystemOperation {
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

  @Primitive(className = "System", primitive = "errorPrint:")
  public abstract static class ErrorPrintPrim extends BinarySystemOperation {
    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final String argument) {
      Universe.errorPrint(argument);
      return receiver;
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final SSymbol argument) {
      return doSObject(receiver, argument.getString());
    }
  }

  @Primitive(className = "System", primitive = "errorPrintln:")
  public abstract static class ErrorPrintlnPrim extends BinarySystemOperation {
    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final String argument) {
      Universe.errorPrintln(argument);
      return receiver;
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final SSymbol argument) {
      return doSObject(receiver, argument.getString());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "printNewline")
  public abstract static class PrintNewlinePrim extends UnarySystemOperation {
    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver) {
      Universe.println();
      return receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "fullGC")
  public abstract static class FullGCPrim extends UnarySystemOperation {

    @TruffleBoundary
    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver) {
      System.gc();
      return true;
    }
  }

  @Primitive(className = "System", primitive = "loadFile:")
  public abstract static class LoadFilePrim extends BinarySystemOperation {
    @TruffleBoundary
    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final String fileName) {
      Path p = Paths.get(fileName);
      try {
        return new String(Files.readAllBytes(p));
      } catch (IOException e) {
        return Nil.nilObject;
      }
    }

    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final Object doSObject(final SObject receiver, final SSymbol argument) {
      return doSObject(receiver, argument.getString());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "printStackTrace")
  public abstract static class PrintStackTracePrim extends UnarySystemOperation {
    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final boolean doSObject(final SObject receiver) {
      return false;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "time")
  public abstract static class TimePrim extends UnarySystemOperation {
    @Specialization(guards = "receiver == universe.getSystemObject()")
    public final long doSObject(final SObject receiver) {
      return System.currentTimeMillis() - startTime;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "ticks")
  public abstract static class TicksPrim extends UnarySystemOperation {
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
