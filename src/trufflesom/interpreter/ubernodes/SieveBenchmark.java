package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.primitives.arrays.AtPrim;
import trufflesom.primitives.arrays.AtPrimFactory;
import trufflesom.primitives.arrays.AtPutPrim;
import trufflesom.primitives.arrays.AtPutPrimFactory;
import trufflesom.primitives.arrays.PutAllNode;
import trufflesom.primitives.arrays.PutAllNodeFactory;
import trufflesom.primitives.basics.LengthPrimFactory;


public abstract class SieveBenchmark {
  /**
   * <pre>
   * sieve: flags size: size = (
          | primeCount |
          primeCount := 0.
          flags putAll: true.
          2 to: size do: [ :i |
              (flags at: i - 1) ifTrue: [
                  | k |
                  primeCount := primeCount + 1.
                  k := i + i.
                  [ k <= size ] whileTrue: [
                      flags at: k - 1 put: false.
                      k := k + i
                  ] ] ].
          ^primeCount
      )
   * </pre>
   */
  public static final class SieveSieve extends AbstractInvokable {
    private final FrameSlot primeCount;
    private final FrameSlot k;

    @Child private PutAllNode putAll;
    @Child private AtPrim     atPrim;
    @Child private AtPutPrim  atPutPrim;

    public static SieveSieve create(final Source source, final long sourceCoord) {
      FrameDescriptor fd = new FrameDescriptor();
      FrameSlot primeCount = fd.addFrameSlot("primeCount");
      FrameSlot k = fd.addFrameSlot("k");
      return new SieveSieve(source, sourceCoord, fd, primeCount, k);
    }

    private SieveSieve(final Source source, final long sourceCoord, final FrameDescriptor fd,
        final FrameSlot primeCount, final FrameSlot k) {
      super(fd, source, sourceCoord);
      this.primeCount = primeCount;
      this.k = k;
      putAll = PutAllNodeFactory.create(null, null, LengthPrimFactory.create(null));
      atPrim = AtPrimFactory.create(null, null);
      atPutPrim = AtPutPrimFactory.create(null, null, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      Object flags = args[1];
      long size = (Long) args[2];

      frame.setLong(primeCount, 0);

      // flags putAll: true.
      putAll.executeEvaluated(frame, flags, true);

      // 2 to: size do: [ :i |
      for (long i = 2; i <= size; i += 1) {
        // (flags at: i - 1)
        boolean flag = (Boolean) atPrim.executeEvaluated(frame, flags, i - 1L);
        // ifTrue: [
        if (flag) {
          final long pc1;
          try {
            // count + 1.
            pc1 = Math.addExact(FrameUtil.getLongSafe(frame, primeCount), 1L);
          } catch (ArithmeticException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnsupportedOperationException();
          }

          // primeCount := primeCount + 1.
          frame.setLong(primeCount, pc1);

          // k := i + i.
          final long ii;
          try {
            // i + i.
            ii = Math.addExact(i, i);
          } catch (ArithmeticException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnsupportedOperationException();
          }
          frame.setLong(k, ii);

          // [ k <= size ] whileTrue: [
          while (FrameUtil.getLongSafe(frame, k) <= size) {
            // flags at: k - 1 put: false.
            long km1;
            try {
              // k - 1
              km1 = Math.subtractExact(FrameUtil.getLongSafe(frame, k), 1L);
            } catch (ArithmeticException e) {
              CompilerDirectives.transferToInterpreterAndInvalidate();
              throw new UnsupportedOperationException();
            }

            atPutPrim.executeEvaluated(frame, flags, km1, false);

            // k := k + i ]. ] ].
            final long ki;
            try {
              // k + i.
              ki = Math.addExact(FrameUtil.getLongSafe(frame, k), i);
            } catch (ArithmeticException e) {
              CompilerDirectives.transferToInterpreterAndInvalidate();
              throw new UnsupportedOperationException();
            }
            frame.setLong(k, ki);
          }
        }
      }

      // ^primeCount
      return FrameUtil.getLongSafe(frame, primeCount);
    }
  }
}
