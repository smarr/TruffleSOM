package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.primitives.basics.SystemPrims.CompilerStatsPrim;
import trufflesom.primitives.basics.SystemPrims.GcStatsPrim;
import trufflesom.primitives.basics.SystemPrimsFactory.CompilerStatsPrimFactory;
import trufflesom.primitives.basics.SystemPrimsFactory.GcStatsPrimFactory;
import trufflesom.vm.Globals;
import trufflesom.vm.Globals.Association;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SObject;


/**
 * <pre>
 *   doRuns: bench = (
        | i total |
        i := 0.
        total := 0.

        [ i < numIterations ] whileTrue: [
            | startTime endTime runTime |
            startTime := system ticks.
            (bench innerBenchmarkLoop: innerIterations) ifFalse: [
              self error: 'Benchmark failed with incorrect result'. ].
            endTime   := system ticks.

            runTime := endTime - startTime.
            printAll ifTrue: [ self print: bench run: runTime ].

            total := total + runTime.
            i := i + 1.

            doGC ifTrue: [
              system fullGC ] ].

        ^ total
    )
 * </pre>
 */
public final class BenchmarkHarnessDoRuns extends AbstractInvokable {

  @Child private AbstractReadFieldNode readNumIterations;
  @Child private AbstractReadFieldNode readInnerIterations;
  @Child private AbstractReadFieldNode readPrintAll;
  @Child private AbstractReadFieldNode readDoGC;

  @Child private AbstractDispatchNode dispatchTicks;
  @Child private AbstractDispatchNode dispatchName;
  @Child private AbstractDispatchNode dispatchInnerBenchmarkLoop;
  @Child private AbstractDispatchNode dispatchError;
  @Child private AbstractDispatchNode dispatchPrintRun;
  @Child private AbstractDispatchNode dispatchFullGC;

  @Child private GcStatsPrim       gcStatsPrim = GcStatsPrimFactory.create(null);
  @Child private CompilerStatsPrim compStats   = CompilerStatsPrimFactory.create(null);

  public BenchmarkHarnessDoRuns(final Source source, final long sourceCoord) {
    super(new FrameDescriptor(), source, sourceCoord);

    readNumIterations = FieldAccessorNode.createRead(2);
    readInnerIterations = FieldAccessorNode.createRead(3);
    readPrintAll = FieldAccessorNode.createRead(4);
    readDoGC = FieldAccessorNode.createRead(5);

    dispatchTicks = new UninitializedDispatchNode(SymbolTable.symbolFor("ticks"));
    dispatchName = new UninitializedDispatchNode(SymbolTable.symbolFor("name"));
    dispatchInnerBenchmarkLoop =
        new UninitializedDispatchNode(SymbolTable.symbolFor("innerBenchmarkLoop:"));
    dispatchError = new UninitializedDispatchNode(SymbolTable.symbolFor("error:"));
    dispatchPrintRun = new UninitializedDispatchNode(SymbolTable.symbolFor("print:run:"));
    dispatchFullGC = new UninitializedDispatchNode(SymbolTable.symbolFor("fullGC"));
  }

  @Override
  public Object execute(final VirtualFrame frame) {
    Object[] args = frame.getArguments();
    SObject rcvr = (SObject) args[0];
    SObject bench = (SObject) args[1];
    Association system = Globals.getGlobalsAssociation(SymbolTable.symbolFor("system"));

    long i = 0;
    long total = 0;

    while (true) {
      long numIterations;

      try {
        numIterations = readNumIterations.readLong(rcvr);
      } catch (UnexpectedResultException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException();
      }

      if (!(i < numIterations)) {
        break;
      }

      long startTime;
      long endTime;
      long runTime;

      SArray startGC;
      SArray endGC;
      long startComp;
      long endComp;

      startGC = gcStatsPrim.doSObject(null);
      startComp = compStats.doSObject(null);

      startTime =
          (Long) dispatchTicks.executeDispatch(frame, new Object[] {system.getValue()});

      long innerIterations;

      try {
        innerIterations = readInnerIterations.readLong(rcvr);
      } catch (UnexpectedResultException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException();
      }

      if (!((Boolean) dispatchInnerBenchmarkLoop.executeDispatch(
          frame, new Object[] {bench, innerIterations}))) {
        dispatchError.executeDispatch(
            frame, new Object[] {rcvr, "Benchmark failed with incorrect result"});
      }

      endTime = (Long) dispatchTicks.executeDispatch(frame, new Object[] {system.getValue()});

      endGC = gcStatsPrim.doSObject(null);
      endComp = compStats.doSObject(null);

      try {
        runTime = Math.subtractExact(endTime, startTime);
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new NotYetImplementedException();
      }

      boolean printAll = (Boolean) readPrintAll.read(rcvr);
      if (printAll) {
        String name = (String) dispatchName.executeDispatch(frame, new Object[] {bench});
        printStats(startGC, endGC, startComp, endComp, name);
        dispatchPrintRun.executeDispatch(frame, new Object[] {rcvr, bench, runTime});
      }

      try {
        total = Math.addExact(total, runTime);
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new NotYetImplementedException();
      }

      try {
        i = Math.addExact(i, 1);
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new NotYetImplementedException();
      }

      boolean doGC = (Boolean) readDoGC.read(rcvr);
      if (doGC) {
        dispatchFullGC.executeDispatch(frame, new Object[] {system.getValue()});
      }
    }

    return total;
  }

  @TruffleBoundary
  private void printStats(final SArray startGC, final SArray endGC, final long startComp,
      final long endComp, final String name) {
    Universe.println(name + ": GC count:     "
        + (endGC.getLongStorage()[0] - startGC.getLongStorage()[0]) + "n");
    Universe.println(name + ": GC time:      "
        + (endGC.getLongStorage()[1] - startGC.getLongStorage()[1]) + "ms");
    Universe.println(name + ": Compile time: " + (endComp - startComp) + "ms");
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }
}
