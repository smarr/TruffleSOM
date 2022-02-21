package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.primitives.arrays.AtPrim;
import trufflesom.primitives.arrays.AtPrimFactory;
import trufflesom.primitives.arrays.AtPutPrim;
import trufflesom.primitives.arrays.AtPutPrimFactory;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SObject;


public abstract class PermuteBenchmark {
  /**
   * <pre>
   * permute: n = (
       count := count + 1.
       n <> 0 ifTrue: [
         self permute: n - 1.
         n downTo: 1 do: [ :i |
           self swap: n with: i.
           self permute: n - 1.
           self swap: n with: i ] ]
      )
   * </pre>
   */
  public static final class PermutePermute extends AbstractInvokable {
    @Child private AbstractReadFieldNode  readCount;
    @Child private AbstractWriteFieldNode writeCount;

    @Child private AbstractDispatchNode dispatchPermute;
    @Child private AbstractDispatchNode dispatchSwap;

    public PermutePermute(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      readCount = FieldAccessorNode.createRead(0);
      writeCount = FieldAccessorNode.createWrite(0);
      dispatchPermute = new UninitializedDispatchNode(SymbolTable.symbolFor("permute:"));
      dispatchSwap = new UninitializedDispatchNode(SymbolTable.symbolFor("swap:with:"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];

      final long n = (Long) args[1];

      long cnt1;
      try {
        // count + 1.
        cnt1 = Math.addExact(readCount.readLongSafe(rcvr), 1L);
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException();
      }
      // count := count + 1.
      writeCount.write(rcvr, cnt1);

      // n <> 0 ifTrue: [
      if (n != 0) {
        long n1;
        try {
          // n - 1
          n1 = Math.subtractExact(n, 1L);
        } catch (ArithmeticException e) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          throw new UnsupportedOperationException();
        }
        // self permute: n - 1.
        dispatchPermute.executeDispatch(frame, new Object[] {rcvr, n1});

        // n downTo: 1 do: [ :i |
        for (long i = n; i >= 1L; i -= 1L) {
          // self swap: n with: i.
          dispatchSwap.executeDispatch(frame, new Object[] {rcvr, n, i});

          long n2;
          try {
            // n - 1.
            n2 = Math.subtractExact(n, 1L);
          } catch (ArithmeticException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnsupportedOperationException();
          }
          // self permute: n - 1.
          dispatchPermute.executeDispatch(frame, new Object[] {rcvr, n2});

          // self swap: n with: i
          dispatchSwap.executeDispatch(frame, new Object[] {rcvr, n, i});
        }
      }
      return rcvr;
    }
  }

  /**
   * <pre>
   *
     | count v |
     swap: i with: j = (
         | tmp |
         tmp := v at: i.
         v at: i put: (v at: j).
         v at: j put: tmp
     )
   * </pre>
   */
  public static final class PermuteSwap extends AbstractInvokable {
    @Child private AbstractReadFieldNode readV;

    @Child private AtPrim    atPrim;
    @Child private AtPutPrim atPutPrim;

    public PermuteSwap(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readV = FieldAccessorNode.createRead(1);

      atPrim = AtPrimFactory.create(null, null);
      atPutPrim = AtPutPrimFactory.create(null, null, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];

      Object i = args[1];
      Object j = args[2];

      Object v = readV.read(rcvr);

      Object tmp = atPrim.executeEvaluated(frame, v, i);
      atPutPrim.executeEvaluated(frame, v, i, atPrim.executeEvaluated(frame, v, j));
      atPutPrim.executeEvaluated(frame, v, j, tmp);
      return rcvr;
    }
  }
}
