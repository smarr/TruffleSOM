package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.primitives.arrays.AtPrim;
import trufflesom.primitives.arrays.AtPrimFactory;
import trufflesom.vmobjects.SObject;


public abstract class QueensBenchmark {
  /**
   * <pre>
   * | freeMaxs freeRows freeMins queenRows |
   * row: r column: c = (
        ^(freeRows at: r) && (freeMaxs at: c + r) && (freeMins at: c - r + 8).
     )
   * </pre>
   */
  public static final class QueensRowColumn extends AbstractInvokable {
    @Child private AbstractReadFieldNode readMax;
    @Child private AbstractReadFieldNode readRows;
    @Child private AbstractReadFieldNode readMins;
    @Child private AtPrim                atPrim;

    public QueensRowColumn(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      readMax = FieldAccessorNode.createRead(0);
      readRows = FieldAccessorNode.createRead(1);
      readMins = FieldAccessorNode.createRead(2);

      atPrim = AtPrimFactory.create(null, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      long r = (Long) args[1];
      long c = (Long) args[2];

      boolean freeRows = (Boolean) atPrim.executeEvaluated(frame, readRows.read(rcvr), r);
      if (!freeRows) {
        return false;
      }

      Object freeMaxs = readMax.read(rcvr);
      long cr;
      try {
        cr = Math.addExact(c, r);
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException();
      }

      boolean fm = (Boolean) atPrim.executeEvaluated(frame, freeMaxs, cr);
      if (!fm) {
        return false;
      }

      Object freeMins = readMins.read(rcvr);
      try {
        cr = Math.subtractExact(c, r);
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException();
      }

      long cr8;
      try {
        cr8 = Math.addExact(cr, 8);
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException();
      }

      return atPrim.executeEvaluated(frame, freeMins, cr8);
    }
  }
  /**
   * <pre>
   row: r column: c put: v = (
       freeRows at: r         put: v.
       freeMaxs at: c + r     put: v.
       freeMins at: c - r + 8 put: v.
   )
   * </pre>
   */
}
