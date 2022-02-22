package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameDescriptor.Builder;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
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

      if (innerIterations == 50) {
        return result == 15;
      }

      if (innerIterations == 1000) {
        return result == 101;
      }

      if (innerIterations == 750) {
        return result == 50;
      }

      if (innerIterations == 1) {
        return result == 128;
      }

      dispatchPrintln.executeDispatch(frame, new Object[] {
          stringAdd("No verification result for ", innerIterations, " found")});
      dispatchPrintln.executeDispatch(frame, new Object[] {
          stringAdd("Result is: ",
              (String) dispatchAsString.executeDispatch(frame, new Object[] {result}))});

      return false;
    }

    @TruffleBoundary
    private static String stringAdd(final String a, final String b) {
      return a + b;
    }

    @TruffleBoundary
    private static String stringAdd(final String a, final long b, final String c) {
      return a + b + c;
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
    private static final int notDoneSlot = 0;
    private static final int escapeSlot  = 1;
    private static final int crSlot      = 2;
    private static final int zSlot       = 3;

    private static final int ziSlot   = 4;
    private static final int ziziSlot = 5;
    private static final int zrzrSlot = 6;

    private static final int ciSlot = 7;

    private static final int xSlot       = 8;
    private static final int byteAccSlot = 9;
    private static final int bitNumSlot  = 10;
    private static final int sumSlot     = 11;

    private static final int ySlot = 12;

    private MandelbrotMandelbrot(final Source source, final long sourceCoord,
        final FrameDescriptor fd) {
      super(fd, source, sourceCoord);
    }

    public static MandelbrotMandelbrot create(final Source source, final long sourceCoord) {
      Builder b = FrameDescriptor.newBuilder(1);
      b.addSlot(FrameSlotKind.Boolean, "notDone", null);
      b.addSlot(FrameSlotKind.Long, "escape", null);
      b.addSlot(FrameSlotKind.Double, "cr", null);
      b.addSlot(FrameSlotKind.Double, "z", null);
      b.addSlot(FrameSlotKind.Double, "zi", null);
      b.addSlot(FrameSlotKind.Double, "zizi", null);
      b.addSlot(FrameSlotKind.Double, "zrzr", null);
      b.addSlot(FrameSlotKind.Double, "ci", null);
      b.addSlot(FrameSlotKind.Long, "x", null);
      b.addSlot(FrameSlotKind.Long, "byteAcc", null);
      b.addSlot(FrameSlotKind.Long, "bitNum", null);
      b.addSlot(FrameSlotKind.Long, "sum", null);
      b.addSlot(FrameSlotKind.Long, "y", null);

      FrameDescriptor fd = b.build();
      return new MandelbrotMandelbrot(source, sourceCoord, fd);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();

      frame.setLong(sumSlot, 0);
      frame.setLong(byteAccSlot, 0);
      frame.setLong(bitNumSlot, 0);

      frame.setLong(ySlot, 0);

      while (true) {
        final long size = (Long) args[1];
        final long y = frame.getLong(ySlot);
        if (!(y < size)) {
          break;
        }
        frame.setDouble(ciSlot, (2.0 * y / size) - 1.0);

        xLoop(frame);

        try {
          frame.setLong(ySlot, Math.addExact(y, 1));
        } catch (ArithmeticException e) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          throw new NotYetImplementedException();
        }
      }

      final long size = (Long) args[1];
      LoopNode.reportLoopCount(this, (int) size);

      return frame.getLong(sumSlot);
    }

    public void zLoop(final VirtualFrame frame) {
      while (true) {
        final boolean notDone = frame.getBoolean(notDoneSlot);
        final long z = frame.getLong(zSlot);
        if (!(notDone && z < 50)) {
          break;
        }

        double zr = frame.getDouble(zrzrSlot)
            - frame.getDouble(ziziSlot)
            + frame.getDouble(crSlot);
        final double zi = 2.0 * zr * frame.getDouble(ziSlot)
            + frame.getDouble(ciSlot);
        frame.setDouble(ziSlot, zi);

        // preserve recalulation
        final double zrzr = zr * zr;
        final double zizi = zi * zi;
        frame.setDouble(zrzrSlot, zrzr);
        frame.setDouble(ziziSlot, zizi);

        if (zrzr + zizi > 4.0) {
          frame.setBoolean(notDoneSlot, false);
          frame.setLong(escapeSlot, 1L);
        }

        try {
          frame.setLong(zSlot, Math.addExact(z, 1));
        } catch (ArithmeticException e) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          throw new NotYetImplementedException();
        }
      }

      LoopNode.reportLoopCount(this, 50);
    }

    private void xLoop(final VirtualFrame frame) {
      frame.setLong(xSlot, 0);

      while (true) {
        final long size = (Long) frame.getArguments()[1];
        final long x = frame.getLong(xSlot);

        if (!(x < size)) {
          break;
        }

        long byteAcc = frame.getLong(byteAccSlot);
        long bitNum = frame.getLong(bitNumSlot);
        long sum = frame.getLong(sumSlot);

        frame.setDouble(zrzrSlot, 0.0);
        frame.setDouble(ziziSlot, 0.0);
        frame.setDouble(ziSlot, 0.0);

        frame.setDouble(crSlot, (2.0 * x / size) - 1.5);
        frame.setLong(zSlot, 0);

        frame.setBoolean(notDoneSlot, true);
        frame.setLong(escapeSlot, 0);

        zLoop(frame);

        if (Long.SIZE - Long.numberOfLeadingZeros(byteAcc) + 1 > Long.SIZE - 1) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          throw new ArithmeticException("shift overflows long");
        }

        try {
          byteAcc = Math.addExact(byteAcc << 1, frame.getLong(escapeSlot));
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
          frame.setLong(xSlot, Math.addExact(x, 1));
        } catch (ArithmeticException e) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          throw new NotYetImplementedException();
        }

        frame.setLong(byteAccSlot, byteAcc);
        frame.setLong(bitNumSlot, bitNum);
        frame.setLong(sumSlot, sum);
      }

      final long size = (Long) frame.getArguments()[1];
      LoopNode.reportLoopCount(this, (int) size);
    }
  }
}
