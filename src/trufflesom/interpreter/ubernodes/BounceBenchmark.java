package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.GlobalNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.vm.Globals;
import trufflesom.vm.Globals.Association;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public abstract class BounceBenchmark {
  /**
   * <pre>
   * | x y xVel yVel |
    bounce = (
        | xLimit yLimit bounced |
        xLimit  := yLimit := 500.
        bounced := false.
  
        x := x + xVel.
        y := y + yVel.
        (x > xLimit)
            ifTrue: [ x := xLimit. xVel := 0 - xVel abs. bounced := true ].
        (x < 0)
            ifTrue: [ x := 0.      xVel := xVel abs.     bounced := true ].
        (y > yLimit)
            ifTrue: [ y := yLimit. yVel := 0 - yVel abs. bounced := true ].
        (y < 0)
            ifTrue: [ y := 0.      yVel := yVel abs.     bounced := true ].
        ^bounced
    )
   * </pre>
   */
  public static final class BallBounce extends AbstractInvokable {

    @Child private AbstractWriteFieldNode writeX;
    @Child private AbstractWriteFieldNode writeY;
    @Child private AbstractWriteFieldNode writeXVel;
    @Child private AbstractWriteFieldNode writeYVel;

    @Child private AbstractReadFieldNode readX;
    @Child private AbstractReadFieldNode readY;
    @Child private AbstractReadFieldNode readXVel;
    @Child private AbstractReadFieldNode readYVel;

    public BallBounce(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      writeX = FieldAccessorNode.createWrite(0);
      writeY = FieldAccessorNode.createWrite(1);
      writeXVel = FieldAccessorNode.createWrite(2);
      writeYVel = FieldAccessorNode.createWrite(3);

      readX = FieldAccessorNode.createRead(0);
      readY = FieldAccessorNode.createRead(1);
      readXVel = FieldAccessorNode.createRead(2);
      readYVel = FieldAccessorNode.createRead(3);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      SObject rcvr = (SObject) frame.getArguments()[0];
      long xLimit = 500;
      long yLimit = 500;

      boolean bounced = false;

      try {
        writeX.write(rcvr,
            Math.addExact(readX.readLongSafe(rcvr), readXVel.readLongSafe(rcvr)));
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new NotYetImplementedException();
      }

      try {
        writeY.write(rcvr,
            Math.addExact(readY.readLongSafe(rcvr), readYVel.readLongSafe(rcvr)));
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new NotYetImplementedException();
      }

      if (readX.readLongSafe(rcvr) > xLimit) {
        writeX.write(rcvr, xLimit);
        writeXVel.write(rcvr, -Math.abs(readXVel.readLongSafe(rcvr)));
        bounced = true;
      }

      if (readX.readLongSafe(rcvr) < 0) {
        writeX.write(rcvr, 0);
        writeXVel.write(rcvr, Math.abs(readXVel.readLongSafe(rcvr)));
        bounced = true;
      }

      if (readY.readLongSafe(rcvr) > yLimit) {
        writeY.write(rcvr, yLimit);
        writeYVel.write(rcvr, -Math.abs(readYVel.readLongSafe(rcvr)));
        bounced = true;
      }

      if (readY.readLongSafe(rcvr) < 0) {
        writeY.write(rcvr, 0);
        writeYVel.write(rcvr, Math.abs(readYVel.readLongSafe(rcvr)));
        bounced = true;
      }

      return bounced;
    }
  }

  /**
   * <pre>
   * initialize = (
        x := Random next % 500.
        y := Random next % 500.
        xVel := (Random next % 300) - 150.
        yVel := (Random next % 300) - 150.
     )
   * </pre>
   */
  public static final class BallInitialize extends AbstractInvokable {
    @CompilationFinal Association globalRandom;

    @Child private AbstractWriteFieldNode writeX;
    @Child private AbstractWriteFieldNode writeY;
    @Child private AbstractWriteFieldNode writeXVel;
    @Child private AbstractWriteFieldNode writeYVel;

    @Child private AbstractDispatchNode dispatchNext;

    public BallInitialize(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      writeX = FieldAccessorNode.createWrite(0);
      writeY = FieldAccessorNode.createWrite(1);
      writeXVel = FieldAccessorNode.createWrite(2);
      writeYVel = FieldAccessorNode.createWrite(3);

      dispatchNext = new UninitializedDispatchNode(SymbolTable.symbolFor("next"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];

      if (globalRandom == null) {
        lookupRandom(rcvr);
      }

      long rand1 =
          (Long) dispatchNext.executeDispatch(frame, new Object[] {globalRandom.getValue()});
      writeX.write(rcvr, Math.floorMod(rand1, 500L));

      long rand2 =
          (Long) dispatchNext.executeDispatch(frame, new Object[] {globalRandom.getValue()});
      writeY.write(rcvr, Math.floorMod(rand2, 500L));

      long rand3 =
          (Long) dispatchNext.executeDispatch(frame, new Object[] {globalRandom.getValue()});
      writeXVel.write(rcvr, Math.floorMod(rand3, 300L) - 150L);

      long rand4 =
          (Long) dispatchNext.executeDispatch(frame, new Object[] {globalRandom.getValue()});
      writeYVel.write(rcvr, Math.floorMod(rand4, 300L) - 150L);

      return rcvr;
    }

    private void lookupRandom(final Object rcvr) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      SSymbol sym = SymbolTable.symbolFor("Random");
      globalRandom = Globals.getGlobalsAssociation(sym);

      if (globalRandom == null) {
        GlobalNode.sendUnknownGlobalToMethodRcvr(rcvr, sym);
        globalRandom = Globals.getGlobalsAssociation(sym);
      }
    }
  }
}
