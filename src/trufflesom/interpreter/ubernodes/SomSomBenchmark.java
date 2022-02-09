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
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public abstract class SomSomBenchmark {
  /**
   * <pre>
   * length: bytecode = (
      bytecode == #halt           ifTrue: [ ^ 1 ].
      bytecode == #dup            ifTrue: [ ^ 1 ].
      bytecode == #pushLocal      ifTrue: [ ^ 3 ].
      bytecode == #pushArgument   ifTrue: [ ^ 3 ].
      bytecode == #pushField      ifTrue: [ ^ 2 ].
      bytecode == #pushBlock      ifTrue: [ ^ 2 ].
      bytecode == #pushConstant   ifTrue: [ ^ 2 ].
      bytecode == #pushGlobal     ifTrue: [ ^ 2 ].
      bytecode == #pop            ifTrue: [ ^ 1 ].
      bytecode == #popLocal       ifTrue: [ ^ 3 ].
      bytecode == #popArgument    ifTrue: [ ^ 3 ].
      bytecode == #popField       ifTrue: [ ^ 2 ].
      bytecode == #send           ifTrue: [ ^ 2 ].
      bytecode == #superSend      ifTrue: [ ^ 2 ].
      bytecode == #returnLocal    ifTrue: [ ^ 1 ].
      bytecode == #returnNonLocal ifTrue: [ ^ 1 ].

      self error: 'Unknown bytecode' + bytecode asString
    )
   * </pre>
   */
  public static final class BytecodesLength extends AbstractInvokable {
    private static final SSymbol symHalt           = SymbolTable.symbolFor("halt");
    private static final SSymbol symDup            = SymbolTable.symbolFor("dup");
    private static final SSymbol symPushLocal      = SymbolTable.symbolFor("pushLocal");
    private static final SSymbol symPushArgument   = SymbolTable.symbolFor("pushArgument");
    private static final SSymbol symPushField      = SymbolTable.symbolFor("pushField");
    private static final SSymbol symPushBlock      = SymbolTable.symbolFor("pushBlock");
    private static final SSymbol symPushConstant   = SymbolTable.symbolFor("pushConstant");
    private static final SSymbol symPushGlobal     = SymbolTable.symbolFor("pushGlobal");
    private static final SSymbol symPop            = SymbolTable.symbolFor("pop");
    private static final SSymbol symPopLocal       = SymbolTable.symbolFor("popLocal");
    private static final SSymbol symPopArgument    = SymbolTable.symbolFor("popArgument");
    private static final SSymbol symPopField       = SymbolTable.symbolFor("popField");
    private static final SSymbol symSend           = SymbolTable.symbolFor("send");
    private static final SSymbol symSuperSend      = SymbolTable.symbolFor("superSend");
    private static final SSymbol symReturnLocal    = SymbolTable.symbolFor("returnLocal");
    private static final SSymbol symReturnNonLocal = SymbolTable.symbolFor("returnNonLocal");

    public BytecodesLength(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object bytecode = args[1];

      if (bytecode == symHalt) {
        return 1L;
      }
      if (bytecode == symDup) {
        return 1L;
      }
      if (bytecode == symPushLocal) {
        return 3L;
      }
      if (bytecode == symPushArgument) {
        return 3L;
      }
      if (bytecode == symPushField) {
        return 2L;
      }
      if (bytecode == symPushBlock) {
        return 2L;
      }
      if (bytecode == symPushConstant) {
        return 2L;
      }
      if (bytecode == symPushGlobal) {
        return 2L;
      }
      if (bytecode == symPop) {
        return 1L;
      }
      if (bytecode == symPopLocal) {
        return 3L;
      }
      if (bytecode == symPopArgument) {
        return 3L;
      }
      if (bytecode == symPopField) {
        return 2L;
      }
      if (bytecode == symSend) {
        return 2L;
      }
      if (bytecode == symSuperSend) {
        return 2L;
      }
      if (bytecode == symReturnLocal) {
        return 1L;
      }
      if (bytecode == symReturnNonLocal) {
        return 1L;
      }

      return rcvr;
    }
  }

  /**
   * <pre>
   * | signature
        holder
        bytecodes literals
        numberOfLocals maximumNumberOfStackElements |.
   * bytecode: index = (
       "Get the bytecode at the given index"
       ^ bytecodes at: index
     )
   * </pre>
   */
  public static final class SMethodBytecode extends AbstractInvokable {
    @Child private AbstractReadFieldNode readBytecodes;
    @Child private AtPrim                atPrim;

    public SMethodBytecode(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readBytecodes = FieldAccessorNode.createRead(2);
      atPrim = AtPrimFactory.create(null, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object index = args[1];
      return atPrim.executeEvaluated(frame, readBytecodes.read(rcvr), index);
    }
  }

  /**
   * <pre>
   *  constant: bytecodeIndex = (
        ^ literals at: (bytecodes at: bytecodeIndex + 1).
      )
   * </pre>
   */
  public static final class SMethodConstant extends AbstractInvokable {
    @Child private AbstractReadFieldNode readLiterals;
    @Child private AbstractReadFieldNode readBytecodes;
    @Child private AtPrim                at1Prim;
    @Child private AtPrim                at2Prim;

    public SMethodConstant(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readBytecodes = FieldAccessorNode.createRead(2);
      readLiterals = FieldAccessorNode.createRead(3);
      at1Prim = AtPrimFactory.create(null, null);
      at2Prim = AtPrimFactory.create(null, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      long bytecodeIndex = (Long) args[1];

      try {
        bytecodeIndex = Math.addExact(bytecodeIndex, 1L);
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException();
      }

      Object bytecode =
          at1Prim.executeEvaluated(frame, readBytecodes.read(rcvr), bytecodeIndex);
      return at2Prim.executeEvaluated(frame, readLiterals.read(rcvr), bytecode);
    }
  }

  /**
   * <pre>
      numberOfArguments = (
        "Get the number of arguments of this method"
        ^ signature numberOfSignatureArguments.
      )
   * </pre>
   */
  public static final class SMethodNumArgs extends AbstractInvokable {
    @Child private AbstractReadFieldNode readSignature;
    @Child private AbstractDispatchNode  dispatchNumSig;

    public SMethodNumArgs(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readSignature = FieldAccessorNode.createRead(0);
      dispatchNumSig =
          new UninitializedDispatchNode(SymbolTable.symbolFor("numberOfSignatureArguments"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      return dispatchNumSig.executeDispatch(frame, new Object[] {readSignature.read(rcvr)});
    }
  }

  /**
   * <pre>
   *  stackPointer (0)
      stack (6)
  
   * pop = (
        | sp |
        "Pop an object from the expression stack and return it"
        sp := stackPointer.
        stackPointer := stackPointer - 1.
        ^ stack at: sp.
     )
   * </pre>
   */
  public static final class FramePop extends AbstractInvokable {
    @Child private AbstractReadFieldNode  readStackPointer;
    @Child private AbstractWriteFieldNode writeStackPointer;
    @Child private AbstractReadFieldNode  readStack;
    @Child private AtPrim                 atPrim;

    public FramePop(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readStackPointer = FieldAccessorNode.createRead(0);
      writeStackPointer = FieldAccessorNode.createWrite(0);
      readStack = FieldAccessorNode.createRead(6);
      atPrim = AtPrimFactory.create(null, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];

      long sp = readStackPointer.readLongSafe(rcvr);
      long sp1;
      try {
        sp1 = Math.subtractExact(sp, 1);
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException();
      }

      writeStackPointer.write(rcvr, sp1);
      return atPrim.executeEvaluated(frame, readStack.read(rcvr), sp);
    }
  }

  /**
   * <pre>
    push: aSAbstractObject = (
      "Push an object onto the expression stack"
      | sp |
      sp := stackPointer + 1.
      stack at: sp put: aSAbstractObject.
      stackPointer := sp
    )
   * </pre>
   */
  public static final class FramePush extends AbstractInvokable {
    @Child private AbstractReadFieldNode  readStackPointer;
    @Child private AbstractWriteFieldNode writeStackPointer;
    @Child private AbstractReadFieldNode  readStack;
    @Child private AtPutPrim              atPutPrim;

    public FramePush(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readStackPointer = FieldAccessorNode.createRead(0);
      writeStackPointer = FieldAccessorNode.createWrite(0);
      readStack = FieldAccessorNode.createRead(6);
      atPutPrim = AtPutPrimFactory.create(null, null, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object aSAbstractObject = args[1];

      long sp = readStackPointer.readLongSafe(rcvr);
      try {
        sp = Math.addExact(sp, 1);
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException();
      }
      atPutPrim.executeEvaluated(frame, readStack.read(rcvr), sp, aSAbstractObject);
      writeStackPointer.write(rcvr, sp);
      return rcvr;
    }
  }

  /**
   * <pre>
   *  hasContext = (
        ^ context (4) ~= nil.
      )
   * </pre>
   */
  public static final class FrameHasContext extends AbstractInvokable {
    @Child private AbstractReadFieldNode readHasContext;

    public FrameHasContext(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readHasContext = FieldAccessorNode.createRead(4);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];

      return readHasContext.read(rcvr) != Nil.nilObject;
    }
  }

  /**
   * <pre>
    stackElement: index = (
      "Get the stack element with the given index
       (an index of zero yields the top element)"
      ^ stack at: stackPointer - index.
    )
   * </pre>
   */
  public static final class FrameStackElement extends AbstractInvokable {
    @Child private AbstractReadFieldNode readStackPointer;
    @Child private AbstractReadFieldNode readStack;
    @Child private AtPrim                atPrim;

    public FrameStackElement(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readStackPointer = FieldAccessorNode.createRead(0);
      readStack = FieldAccessorNode.createRead(6);
      atPrim = AtPrimFactory.create(null, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      long index = (Long) args[1];

      long sp = readStackPointer.readLongSafe(rcvr);
      try {
        sp = Math.subtractExact(sp, index);
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException();
      }
      return atPrim.executeEvaluated(frame, readStack.read(rcvr), sp);
    }
  }

  /**
   * <pre>
   * local: index = (
       ^ stack at: localOffset (2) + index - 1.
     )
   * </pre>
   */
  public static final class FrameLocal extends AbstractInvokable {
    @Child private AbstractReadFieldNode readLocalOffset;
    @Child private AbstractReadFieldNode readStack;
    @Child private AtPrim                atPrim;

    public FrameLocal(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readLocalOffset = FieldAccessorNode.createRead(2);
      readStack = FieldAccessorNode.createRead(6);
      atPrim = AtPrimFactory.create(null, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      long index = (Long) args[1];

      long offset = readLocalOffset.readLongSafe(rcvr);
      try {
        offset = Math.addExact(offset, index);
        offset = Math.subtractExact(offset, 1L);
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException();
      }
      return atPrim.executeEvaluated(frame, readStack.read(rcvr), offset);
    }
  }

  /**
   * <pre>
     local: index put: value = (
       stack at: localOffset + index - 1 put: value.
     )
   * </pre>
   */
  public static final class FrameLocalPut extends AbstractInvokable {
    @Child private AbstractReadFieldNode readLocalOffset;
    @Child private AbstractReadFieldNode readStack;
    @Child private AtPutPrim             atPutPrim;

    public FrameLocalPut(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readLocalOffset = FieldAccessorNode.createRead(2);
      readStack = FieldAccessorNode.createRead(6);
      atPutPrim = AtPutPrimFactory.create(null, null, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      long index = (Long) args[1];
      Object value = args[2];

      long offset = readLocalOffset.readLongSafe(rcvr);
      try {
        offset = Math.addExact(offset, index);
        offset = Math.subtractExact(offset, 1L);
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException();
      }
      atPutPrim.executeEvaluated(frame, readStack.read(rcvr), offset, value);
      return rcvr;
    }
  }

  /**
   * <pre>
    argument: index = (
      ^ stack at: index.
    )
   * </pre>
   */
  public static final class FrameArgument extends AbstractInvokable {
    @Child private AbstractReadFieldNode readStack;
    @Child private AtPrim                atPrim;

    public FrameArgument(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readStack = FieldAccessorNode.createRead(6);
      atPrim = AtPrimFactory.create(null, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      long index = (Long) args[1];

      return atPrim.executeEvaluated(frame, readStack.read(rcvr), index);
    }
  }

  /**
   * <pre>
   * clearPreviousFrame = (
       previousFrame (5) := nil.
     )
   * </pre>
   */
  public static final class FrameClearPrevious extends AbstractInvokable {
    @Child private AbstractWriteFieldNode writePreviousFrame;

    public FrameClearPrevious(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      writePreviousFrame = FieldAccessorNode.createWrite(5);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      writePreviousFrame.write(rcvr, Nil.nilObject);
      return rcvr;
    }
  }

  /**
   * <pre>
   * context: level = (
       | frame |
       "Get the context frame at the given level"
       frame := self.

       "Iterate through the context chain until the given level is reached"
       [level > 0] whileTrue: [
         "Get the context of the current frame"
         frame := frame context.

         "Go to the next level"
         level := level - 1 ].

       ^ frame
     )
   * </pre>
   */
  public static final class FrameContext extends AbstractInvokable {
    @Child private AbstractDispatchNode dispatchContext;

    public FrameContext(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      dispatchContext = new UninitializedDispatchNode(SymbolTable.symbolFor("context"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      long level = (Long) args[1];

      Object frameObj = rcvr;

      while (level > 0) {
        frameObj = dispatchContext.executeDispatch(frame, new Object[] {frameObj});

        try {
          level = Math.subtractExact(level, 1L);
        } catch (ArithmeticException e) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          throw new UnsupportedOperationException();
        }
      }

      return frameObj;
    }
  }

}
