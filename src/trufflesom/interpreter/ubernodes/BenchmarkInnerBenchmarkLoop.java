package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SObject;


/**
 * <pre>
 *     innerBenchmarkLoop: innerIterations = (
        | i |
        i := 0.
        [ i < innerIterations ] whileTrue: [
            (self verifyResult: self benchmark) ifFalse: [ ^ false ].
            i := i + 1.
        ].
        ^ true
    )
 * </pre>
 */
public final class BenchmarkInnerBenchmarkLoop extends AbstractInvokable {

  @Child private AbstractDispatchNode dispatchBenchmark;
  @Child private AbstractDispatchNode dispatchVerifyResult;

  public BenchmarkInnerBenchmarkLoop(final Source source, final long sourceCoord) {
    super(new FrameDescriptor(), source, sourceCoord);

    dispatchBenchmark = new UninitializedDispatchNode(SymbolTable.symbolFor("benchmark"));
    dispatchVerifyResult =
        new UninitializedDispatchNode(SymbolTable.symbolFor("verifyResult:"));
  }

  @Override
  public Object execute(final VirtualFrame frame) {
    long i = 0;

    Object[] args = frame.getArguments();
    SObject rcvr = (SObject) args[0];
    long innerIterations = (Long) args[1];

    while (i < innerIterations) {
      Object benchmarkResult = dispatchBenchmark.executeDispatch(frame, new Object[] {rcvr});
      boolean verified = (Boolean) dispatchVerifyResult.executeDispatch(frame,
          new Object[] {rcvr, benchmarkResult});
      if (!verified) {
        return false;
      }

      try {
        i = Math.addExact(i, 1);
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new NotYetImplementedException();
      }
    }

    return true;
  }

}
