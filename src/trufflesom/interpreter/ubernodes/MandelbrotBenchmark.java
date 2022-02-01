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


public abstract class MandelbrotBenchmark {
  /**
   * <pre>
   * innerBenchmarkLoop: innerIterations = (
       ^ self verify: (self mandelbrot: innerIterations) inner: innerIterations.
     )
   * </pre>
   */
  public static final class MandelbrotInnerBenchmarkLoop extends AbstractInvokable {
    @Child private AbstractDispatchNode dispatchVerifyInner;
    @Child private AbstractDispatchNode dispatchMandelbrot;

    public MandelbrotInnerBenchmarkLoop(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      dispatchVerifyInner =
          new UninitializedDispatchNode(SymbolTable.symbolFor("verify:inner:"));
      dispatchMandelbrot = new UninitializedDispatchNode(SymbolTable.symbolFor("mandelbrot:"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      Object rcvr = args[0];
      Object innerIterations = args[1];

      return dispatchVerifyInner.executeDispatch(frame, new Object[] {
          rcvr,
          dispatchMandelbrot.executeDispatch(frame, new Object[] {rcvr, innerIterations}),
          innerIterations});
    }
  }

  /**
   * <pre>
   *  verify: result inner: innerIterations = (
        innerIterations = 500 ifTrue: [ ^ result = 191 ].
        innerIterations = 750 ifTrue: [ ^ result = 50  ].
        innerIterations = 1   ifTrue: [ ^ result = 128 ].
  
        ('No verification result for ' + innerIterations + ' found') println.
        ('Result is: ' + result asString) println.
        ^ false
      )
   * </pre>
   */
  public static final class MandelbrotVerifyInner extends AbstractInvokable {
    @Child private AbstractDispatchNode dispatchPrintln;
    @Child private AbstractDispatchNode dispatchAsString;

    public MandelbrotVerifyInner(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      dispatchPrintln = new UninitializedDispatchNode(SymbolTable.symbolFor("println"));
      dispatchAsString = new UninitializedDispatchNode(SymbolTable.symbolFor("asString"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      long result = (Long) args[1];
      long innerIterations = (Long) args[2];

      if (innerIterations == 500) {
        return result == 191;
      }

      if (innerIterations == 750) {
        return result == 50;
      }

      if (innerIterations == 1) {
        return result == 128;
      }

      dispatchPrintln.executeDispatch(frame, new Object[] {
          "No verification result for " + innerIterations + " found"});
      dispatchPrintln.executeDispatch(frame, new Object[] {
          "Result is: " + dispatchAsString.executeDispatch(frame, new Object[] {result})});

      return false;
    }
  }

  /**
   * <pre>
   mandelbrot: size = (
      | sum byteAcc bitNum y |
      sum     := 0.
      byteAcc := 0.
      bitNum  := 0.

      y := 0.
  
      [y < size] whileTrue: [
          | ci x |
          ci := (2.0 * y // size) - 1.0.
          x  := 0.
  
          [x < size] whileTrue: [
              | zr zrzr zi zizi cr escape z notDone |
              zrzr := zr := 0.0.
              zizi := zi := 0.0.
              cr   := (2.0 * x // size) - 1.5.
  
              z := 0.
              notDone := true.
              escape := 0.
              [notDone and: [z < 50]] whileTrue: [
                  zr := zrzr - zizi + cr.
                  zi := 2.0 * zr * zi + ci.
  
                  "preserve recalculation"
                  zrzr := zr * zr.
                  zizi := zi * zi.
  
                  (zrzr + zizi > 4.0) ifTrue: [
                      notDone := false.
                      escape  := 1.
                  ].
                  z := z + 1.
              ].

              byteAcc := (byteAcc << 1) + escape.
              bitNum  := bitNum + 1.

              " Code is very similar for these cases, but using separate blocks
                ensures we skip the shifting when it's unnecessary,
                which is most cases. "
              bitNum = 8
                  ifTrue: [
                    sum := sum bitXor: byteAcc.
                    byteAcc := 0.
                    bitNum  := 0. ]
                  ifFalse: [
                    (x = (size - 1)) ifTrue: [
                        byteAcc := byteAcc << (8 - bitNum).
                        sum := sum bitXor: byteAcc.
                        byteAcc := 0.
                        bitNum  := 0. ]].
              x := x + 1.
          ].
          y := y + 1.
      ].
  
      ^ sum
  )
   * </pre>
   */
  public static final class MandelbrotMandelbrot extends AbstractInvokable {
    public MandelbrotMandelbrot(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      long size = (Long) args[1];

      long sum = 0;
      long byteAcc = 0;
      long bitNum = 0;

      long y = 0;

      while (y < size) {
        double ci = (2.0 * y / size) - 1.0;
        long x = 0;

        while (x < size) {
          double zrzr = 0.0;
          double zr = 0.0;
          double zizi = 0.0;
          double zi = 0.0;

          double cr = (2.0 * x / size) - 1.5;

          long z = 0;
          boolean notDone = true;
          long escape = 0;

          while (notDone && z < 50) {
            zr = zrzr - zizi + cr;
            zi = 2.0 * zr * zi + ci;

            // preserve recalulation
            zrzr = zr * zr;
            zizi = zi * zi;

            if (zrzr + zizi > 4.0) {
              notDone = false;
              escape = 1;
            }

            try {
              z = Math.addExact(z, 1);
            } catch (ArithmeticException e) {
              CompilerDirectives.transferToInterpreterAndInvalidate();
              throw new NotYetImplementedException();
            }
          }

          if (Long.SIZE - Long.numberOfLeadingZeros(byteAcc) + 1 > Long.SIZE - 1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new ArithmeticException("shift overflows long");
          }

          try {
            byteAcc = Math.addExact(byteAcc << 1, escape);
          } catch (ArithmeticException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new NotYetImplementedException();
          }

          try {
            bitNum = Math.addExact(bitNum, 1);
          } catch (ArithmeticException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new NotYetImplementedException();
          }

          if (bitNum == 8) {
            sum = sum ^ byteAcc;
            byteAcc = 0;
            bitNum = 0;
          } else {
            long sizeM1;
            try {
              sizeM1 = Math.subtractExact(size, 1);
            } catch (ArithmeticException e) {
              CompilerDirectives.transferToInterpreterAndInvalidate();
              throw new NotYetImplementedException();
            }

            if (x == sizeM1) {
              long remainingBits;
              try {
                remainingBits = Math.subtractExact(8, bitNum);
              } catch (ArithmeticException e) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new NotYetImplementedException();
              }

              byteAcc = byteAcc << remainingBits;

              sum = sum ^ byteAcc;
              byteAcc = 0;
              bitNum = 0;
            }
          }

          try {
            x = Math.addExact(x, 1);
          } catch (ArithmeticException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new NotYetImplementedException();
          }
        }

        try {
          y = Math.addExact(y, 1);
        } catch (ArithmeticException e) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          throw new NotYetImplementedException();
        }
      }

      return sum;
    }
  }
}
