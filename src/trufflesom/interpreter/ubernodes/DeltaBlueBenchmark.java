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
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public abstract class DeltaBlueBenchmark {
  /**
   * <pre>
   * | strength |
   * | v1 v2 direction |
   * execute = (
       "Enforce this constraint. Assume that it is satisfied."
       direction = #forward
         ifTrue:  [ v2 value: v1 value ]
         ifFalse: [ v1 value: v2 value ].
     )
   * </pre>
   */
  public static final class DBECExecute extends AbstractInvokable {
    @Child private AbstractReadFieldNode readDirection;
    @Child private AbstractReadFieldNode readV1;
    @Child private AbstractReadFieldNode readV2;

    @Child private AbstractDispatchNode dispatchValue;
    @Child private AbstractDispatchNode dispatchValue_;

    private static final SSymbol symForward = SymbolTable.symbolFor("forward");

    public DBECExecute(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readDirection = FieldAccessorNode.createRead(3);
      readV1 = FieldAccessorNode.createRead(1);
      readV2 = FieldAccessorNode.createRead(2);

      dispatchValue = new UninitializedDispatchNode(SymbolTable.symbolFor("value"));
      dispatchValue_ = new UninitializedDispatchNode(SymbolTable.symbolFor("value:"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      SObject rcvr = (SObject) frame.getArguments()[0];
      if (readDirection.read(rcvr) == symForward) {
        dispatchValue_.executeDispatch(frame, new Object[] {
            readV2.read(rcvr),
            dispatchValue.executeDispatch(frame, new Object[] {readV1.read(rcvr)})
        });
      } else {
        dispatchValue_.executeDispatch(frame, new Object[] {
            readV1.read(rcvr),
            dispatchValue.executeDispatch(frame, new Object[] {readV2.read(rcvr)})
        });
      }

      return rcvr;
    }
  }

  /**
   * <pre>
   *  output = (
        (direction == #forward)
            ifTrue:  [ ^ v2 ]
            ifFalse: [ ^ v1 ].
      )
   * </pre>
   */
  public static final class DBBCOutput extends AbstractInvokable {
    @Child private AbstractReadFieldNode readDirection;
    @Child private AbstractReadFieldNode readV1;
    @Child private AbstractReadFieldNode readV2;

    @Child private AbstractDispatchNode dispatchValue;
    @Child private AbstractDispatchNode dispatchValue_;

    private static final SSymbol symForward = SymbolTable.symbolFor("forward");

    public DBBCOutput(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readDirection = FieldAccessorNode.createRead(3);
      readV1 = FieldAccessorNode.createRead(1);
      readV2 = FieldAccessorNode.createRead(2);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      SObject rcvr = (SObject) frame.getArguments()[0];
      if (readDirection.read(rcvr) == symForward) {
        return readV2.read(rcvr);
      } else {
        return readV1.read(rcvr);
      }
    }
  }

  /**
   * <pre>
   * isSatisfied = (
       ^ direction notNil.
     )
   * </pre>
   */
  public static final class DBBCIsSatisfied extends AbstractInvokable {
    @Child private AbstractReadFieldNode readDirection;

    public DBBCIsSatisfied(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readDirection = FieldAccessorNode.createRead(3);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      SObject rcvr = (SObject) frame.getArguments()[0];
      return readDirection.read(rcvr) != Nil.nilObject;
    }
  }

  /**
   * <pre>
   * | strength |
   * | v1 v2 direction |
   * | scale offset |
   * execute = (
       direction = #forward
         ifTrue:  [ v2 value: (v1 value * scale value) + offset value ]
         ifFalse: [ v1 value: (v2 value - offset value) / scale value ].
     )
   * </pre>
   */
  public static final class DBSCExecute extends AbstractInvokable {
    @Child private AbstractReadFieldNode readDirection;
    @Child private AbstractReadFieldNode readV1;
    @Child private AbstractReadFieldNode readV2;
    @Child private AbstractReadFieldNode readScale;
    @Child private AbstractReadFieldNode readOffset;

    @Child private AbstractDispatchNode dispatchValue;
    @Child private AbstractDispatchNode dispatchValue_;

    private static final SSymbol symForward = SymbolTable.symbolFor("forward");

    public DBSCExecute(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readDirection = FieldAccessorNode.createRead(3);
      readV1 = FieldAccessorNode.createRead(1);
      readV2 = FieldAccessorNode.createRead(2);
      readScale = FieldAccessorNode.createRead(4);
      readOffset = FieldAccessorNode.createRead(5);

      dispatchValue = new UninitializedDispatchNode(SymbolTable.symbolFor("value"));
      dispatchValue_ = new UninitializedDispatchNode(SymbolTable.symbolFor("value:"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      SObject rcvr = (SObject) frame.getArguments()[0];
      if (readDirection.read(rcvr) == symForward) {
        long v1 =
            (Long) dispatchValue.executeDispatch(frame, new Object[] {readV1.read(rcvr)});
        long scale =
            (Long) dispatchValue.executeDispatch(frame, new Object[] {readScale.read(rcvr)});
        long offset = (Long) dispatchValue.executeDispatch(frame,
            new Object[] {readOffset.read(rcvr)});

        try {
          long value = Math.addExact((v1 * scale), offset);
          dispatchValue_.executeDispatch(frame, new Object[] {readV2.read(rcvr), value});
        } catch (ArithmeticException e) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          throw new UnsupportedOperationException();
        }
      } else {
        long v2 =
            (Long) dispatchValue.executeDispatch(frame, new Object[] {readV2.read(rcvr)});
        long offset = (Long) dispatchValue.executeDispatch(frame,
            new Object[] {readOffset.read(rcvr)});
        long scale =
            (Long) dispatchValue.executeDispatch(frame, new Object[] {readScale.read(rcvr)});

        try {
          long value = Math.subtractExact(v2, offset) / scale;
          dispatchValue_.executeDispatch(frame, new Object[] {readV1.read(rcvr), value});
        } catch (ArithmeticException e) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          throw new UnsupportedOperationException();
        }
      }

      return rcvr;
    }
  }

  /**
   * <pre>
   * | symbolicValue arithmeticValue |.
   * weaker: aStrength = (
        ^ arithmeticValue > aStrength arithmeticValue
     )
   * </pre>
   */
  public static final class DBSWeaker extends AbstractInvokable {
    @Child private AbstractReadFieldNode readArithValue;
    @Child private AbstractDispatchNode  dispatchArithValue;

    public DBSWeaker(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readArithValue = FieldAccessorNode.createRead(1);
      dispatchArithValue =
          new UninitializedDispatchNode(SymbolTable.symbolFor("arithmeticValue"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object aStrength = args[1];
      long arithVal = readArithValue.readLongSafe(rcvr);

      return arithVal > (Long) dispatchArithValue.executeDispatch(
          frame, new Object[] {aStrength});
    }
  }

  /**
   * <pre>
   * stronger: aStrength = (
        ^ arithmeticValue < aStrength arithmeticValue.
     )
   * </pre>
   */
  public static final class DBSStronger extends AbstractInvokable {
    @Child private AbstractReadFieldNode readArithValue;
    @Child private AbstractDispatchNode  dispatchArithValue;

    public DBSStronger(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readArithValue = FieldAccessorNode.createRead(1);
      dispatchArithValue =
          new UninitializedDispatchNode(SymbolTable.symbolFor("arithmeticValue"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object aStrength = args[1];
      long arithVal = readArithValue.readLongSafe(rcvr);

      return arithVal < (Long) dispatchArithValue.executeDispatch(
          frame, new Object[] {aStrength});
    }
  }

  /**
   * <pre>
   *  weakest: aStrength = (
        (aStrength weaker: self)
          ifTrue:  [ ^ aStrength ]
          ifFalse: [ ^ self ].
     )
   * </pre>
   */
  public static final class DBSWeakest extends AbstractInvokable {
    @Child private AbstractDispatchNode dispatchWeaker;

    public DBSWeakest(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      dispatchWeaker =
          new UninitializedDispatchNode(SymbolTable.symbolFor("weaker:"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object aStrength = args[1];

      if ((Boolean) dispatchWeaker.executeDispatch(
          frame, new Object[] {aStrength, rcvr})) {
        return aStrength;
      }
      return rcvr;
    }
  }
}
