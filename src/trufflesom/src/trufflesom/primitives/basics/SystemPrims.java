package trufflesom.primitives.basics;

import java.io.IOException;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.sun.management.ThreadMXBean;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.bdt.source.SourceCoordinate;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.interpreter.nodes.nary.TernaryExpressionNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.vm.Globals;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.Universe;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public final class SystemPrims {

  @Primitive(className = "System", primitive = "load:")
  @GenerateNodeFactory
  public abstract static class LoadPrim extends BinaryExpressionNode {
    @Specialization
    public static final Object doSObject(@SuppressWarnings("unused") final SObject receiver,
        final SSymbol argument) {
      SClass result = Universe.loadClass(argument);
      return result != null ? result : Nil.nilObject;
    }
  }

  @Primitive(className = "System", primitive = "exit:")
  @GenerateNodeFactory
  public abstract static class ExitPrim extends BinaryExpressionNode {
    @Specialization
    public static final Object doSObject(final SObject receiver, final long error) {
      Universe.exit((int) error);
      return receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "global:put:")
  public abstract static class GlobalPutPrim extends TernaryExpressionNode {
    @Specialization
    public static final Object doSObject(@SuppressWarnings("unused") final SObject receiver,
        final SSymbol global,
        final Object value) {
      Globals.setGlobal(global, value);
      return receiver;
    }
  }

  @Primitive(className = "System", primitive = "printString:")
  @GenerateNodeFactory
  public abstract static class PrintStringPrim extends BinaryExpressionNode {
    @Specialization
    public static final Object doSObject(final SObject receiver, final String argument) {
      Universe.print(argument);
      return receiver;
    }

    @Specialization
    public static final Object doSObject(final SObject receiver, final SSymbol argument) {
      return doSObject(receiver, argument.getString());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "errorPrint:")
  public abstract static class ErrorPrintPrim extends BinaryExpressionNode {
    @Specialization
    public static final Object doSObject(final SObject receiver, final String argument) {
      Universe.errorPrint(argument);
      return receiver;
    }

    @Specialization
    public static final Object doSObject(final SObject receiver, final SSymbol argument) {
      return doSObject(receiver, argument.getString());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "errorPrintln:")
  public abstract static class ErrorPrintlnPrim extends BinaryExpressionNode {
    @Specialization
    public static final Object doSObject(final SObject receiver, final String argument) {
      Universe.errorPrintln(argument);
      return receiver;
    }

    @Specialization
    public static final Object doSObject(final SObject receiver, final SSymbol argument) {
      return doSObject(receiver, argument.getString());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "printNewline")
  public abstract static class PrintNewlinePrim extends UnaryExpressionNode {
    @Specialization
    public static final Object doSObject(final SObject receiver) {
      Universe.println();
      return receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "fullGC")
  public abstract static class FullGCPrim extends UnaryExpressionNode {

    @TruffleBoundary
    @Specialization
    public static final Object doSObject(@SuppressWarnings("unused") final SObject receiver) {
      System.gc();
      return true;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "loadFile:")
  public abstract static class LoadFilePrim extends BinaryExpressionNode {
    @TruffleBoundary
    @Specialization
    public static final Object doSObject(@SuppressWarnings("unused") final SObject receiver,
        final String fileName) {
      Path p = Paths.get(fileName);
      try {
        return new String(Files.readAllBytes(p));
      } catch (IOException e) {
        return Nil.nilObject;
      }
    }

    @Specialization
    public static final Object doSObject(final SObject receiver, final SSymbol argument) {
      return doSObject(receiver, argument.getString());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "printStackTrace")
  public abstract static class PrintStackTracePrim extends UnaryExpressionNode {
    @Specialization
    public static final boolean doSObject(@SuppressWarnings("unused") final SObject receiver) {
      printStackTrace(2, null);
      return true;
    }

    @TruffleBoundary
    public static void printStackTrace(final int skipDnuFrames, final SourceSection topNode) {
      List<String> method = new ArrayList<>();
      List<String> location = new ArrayList<>();
      int[] maxLengthMethod = {0};
      boolean[] first = {true};
      Universe.println("Stack Trace");

      Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Object>() {
        @Override
        public Object visitFrame(final FrameInstance frameInstance) {
          RootCallTarget ct = (RootCallTarget) frameInstance.getCallTarget();

          if (!(ct.getRootNode() instanceof Invokable)) {
            return new NotYetImplementedException(
                "do we need to handle other kinds of root nodes?");
          }

          Invokable m = (Invokable) ct.getRootNode();

          String id = m.getName();
          method.add(id);
          maxLengthMethod[0] = Math.max(maxLengthMethod[0], id.length());
          Node callNode = frameInstance.getCallNode();
          if (callNode != null || first[0]) {
            SourceSection nodeSS;
            if (first[0]) {
              first[0] = false;
              nodeSS = topNode;
            } else {
              nodeSS = callNode.getEncapsulatingSourceSection();
            }
            if (nodeSS != null) {
              location.add(nodeSS.getSource().getName()
                  + SourceCoordinate.getLocationQualifier(nodeSS));
            } else {
              location.add("");
            }
          } else {
            location.add("");
          }

          return null;
        }
      });

      StringBuilder sb = new StringBuilder();
      for (int i = method.size() - 1; i >= skipDnuFrames; i--) {
        sb.append(String.format("\t%1$-" + (maxLengthMethod[0] + 4) + "s",
            method.get(i)));
        sb.append(location.get(i));
        sb.append('\n');
      }

      Universe.print(sb.toString());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "time")
  public abstract static class TimePrim extends UnaryExpressionNode {
    @Specialization
    public static final long doSObject(@SuppressWarnings("unused") final SObject receiver) {
      return System.currentTimeMillis() - startTime;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "ticks")
  public abstract static class TicksPrim extends UnaryExpressionNode {
    @Specialization
    public static final long doSObject(@SuppressWarnings("unused") final SObject receiver) {
      return System.nanoTime() / 1000L - startMicroTime;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "gcStats")
  public abstract static class GcStatsPrim extends UnaryExpressionNode {
    @CompilationFinal private static List<GarbageCollectorMXBean> gcBeans;
    @CompilationFinal private static ThreadMXBean                 threadBean;

    @Specialization
    public static final SArray doSObject(@SuppressWarnings("unused") final Object receiver) {
      if (gcBeans == null) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        threadBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        threadBean.setThreadAllocatedMemoryEnabled(true);
      }

      SArray arr = createStatsArray();
      return arr;
    }

    @TruffleBoundary
    private static SArray createStatsArray() {
      long allocatedBytes = threadBean.getCurrentThreadAllocatedBytes();
      long counts = 0;
      long time = 0;

      for (GarbageCollectorMXBean b : gcBeans) {
        long c = b.getCollectionCount();
        if (c != -1) {
          counts += c;
        }

        long t = b.getCollectionTime();
        if (t != -1) {
          time += t;
        }
      }

      return new SArray(new long[] {counts, time, allocatedBytes});
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "totalCompilationTime")
  public abstract static class CompilerStatsPrim extends UnaryExpressionNode {
    @CompilationFinal private static CompilationMXBean bean;

    @Specialization
    public static final long doSObject(@SuppressWarnings("unused") final Object receiver) {
      if (bean == null) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        bean = ManagementFactory.getCompilationMXBean();
      }

      return bean.getTotalCompilationTime();
    }
  }

  {
    startMicroTime = System.nanoTime() / 1000L;
    startTime = startMicroTime / 1000L;
  }
  private static long startTime;
  private static long startMicroTime;
}
