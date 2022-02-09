package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.primitives.arrays.AtPrim;
import trufflesom.primitives.arrays.AtPrimFactory;
import trufflesom.primitives.arrays.AtPutPrim;
import trufflesom.primitives.arrays.AtPutPrimFactory;
import trufflesom.vm.SymbolTable;
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
}
