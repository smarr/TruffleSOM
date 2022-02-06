package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SObject;


public abstract class Vector2DClass {
  /**
   * <pre>
   * compare: a and: b = (
        a = b ifTrue: [ ^  0 ].
        a < b ifTrue: [ ^ -1 ].
        a > b ifTrue: [ ^  1 ].

        "We say that NaN is smaller than non-NaN."
        a = a ifTrue: [ ^ 1 ].
        ^ -1
      )
   * </pre>
   */
  public static final class Vector2dCompareAnd extends AbstractInvokable {

    public Vector2dCompareAnd(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      double a = (Double) args[1];
      double b = (Double) args[2];

      if (a == b) {
        return 0L;
      }
      if (a < b) {
        return -1L;
      }
      if (a > b) {
        return 1L;
      }

      if (a == a) {
        return 1L;
      }
      return -1;
    }
  }

  /**
   * <pre>
   * initX: anX y: aY = (
        x := anX.
        y := aY
      )
   * </pre>
   */
  public static final class Vector2dInitXY extends AbstractInvokable {

    @Child private AbstractWriteFieldNode writeX;
    @Child private AbstractWriteFieldNode writeY;

    public Vector2dInitXY(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      writeX = FieldAccessorNode.createWrite(0);
      writeY = FieldAccessorNode.createWrite(1);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      double x = (Double) args[1];
      double y = (Double) args[2];

      writeX.write(rcvr, x);
      writeY.write(rcvr, y);

      return rcvr;
    }
  }

  /**
   * <pre>
   * compareTo: other = (
      | result |
      result := self compare: x and: other x.
      result <> 0 ifTrue: [ ^ result ].
      ^ self compare: y and: other y
    )
   * </pre>
   */
  public static final class Vector2dCompareTo extends AbstractInvokable {
    @Child private AbstractDispatchNode dispatchCompareAnd;
    @Child private AbstractDispatchNode dispatchX;
    @Child private AbstractDispatchNode dispatchY;

    @Child private AbstractReadFieldNode readX;
    @Child private AbstractReadFieldNode readY;

    public Vector2dCompareTo(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      dispatchCompareAnd =
          new UninitializedDispatchNode(SymbolTable.symbolFor("compare:and:"));
      dispatchX = new UninitializedDispatchNode(SymbolTable.symbolFor("x"));
      dispatchY = new UninitializedDispatchNode(SymbolTable.symbolFor("y"));

      readX = FieldAccessorNode.createRead(0);
      readY = FieldAccessorNode.createRead(1);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object other = args[1];

      long result = (Long) dispatchCompareAnd.executeDispatch(frame, new Object[] {
          rcvr,
          readX.read(rcvr),
          dispatchX.executeDispatch(frame, new Object[] {other})});

      if (result != 0L) {
        return result;
      }

      return dispatchCompareAnd.executeDispatch(frame, new Object[] {
          rcvr,
          readY.read(rcvr),
          dispatchY.executeDispatch(frame, new Object[] {other})});
    }
  }
}
