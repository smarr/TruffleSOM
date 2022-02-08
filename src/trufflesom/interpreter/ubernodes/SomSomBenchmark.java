package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.primitives.arrays.AtPrim;
import trufflesom.primitives.arrays.AtPrimFactory;
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
}
