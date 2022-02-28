package trufflesom.interpreter.nodes.minibytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterSwitch;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import trufflesom.interpreter.nodes.NoPreEvalExprNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.StorageLocation.LongStorageLocation;
import trufflesom.vmobjects.SObject;


public final class ComputeBytecode extends NoPreEvalExprNode {

  @Child AbstractReadFieldNode readField;

  @CompilationFinal(dimensions = 1) private final byte[] bytecodes;

  private final long const1;
  private final long const2;
  private final long const3;

  public ComputeBytecode(final long const1, final long const2, final long const3,
      final byte[] bytecodes, final int fieldIndex) {
    this.readField = FieldAccessorNode.createRead(fieldIndex);
    this.const1 = const1;
    this.const2 = const2;
    this.const3 = const3;
    this.bytecodes = bytecodes;
  }

  private static class State {
    long top;
  }

  /**
   * <pre>
   * next = (
        seed := ((seed * 1309) + 13849) & 65535.
        ^seed
     )
   * </pre>
   */
  @Override
  @ExplodeLoop(kind = LoopExplosionKind.MERGE_EXPLODE)
  @BytecodeInterpreterSwitch
  public Object executeGeneric(final VirtualFrame frame) {
    final Object[] args = frame.getArguments();
    final SObject rcvr = (SObject) args[0];
    final State state = new State();

    // bytecodes
    // 0: assert field is long, unsupported otherwise
    // 1: read field as long to top
    // 2: top * const1
    // 3: top + const2
    // 4: top & const3
    // 5: write field
    // 6: return top

    final byte[] bytecodes = this.bytecodes;

    CompilerAsserts.partialEvaluationConstant(bytecodes);
    CompilerAsserts.compilationConstant(bytecodes);
    CompilerDirectives.ensureVirtualized(state);

    LongStorageLocation storage = null;

    int i = 0;

    for (;;) {
      byte b = bytecodes[i];
      CompilerAsserts.partialEvaluationConstant(b);
      CompilerAsserts.compilationConstant(b);
      CompilerAsserts.partialEvaluationConstant(i);

      switch (b) {
        case 0:
          if (readField.isLong(rcvr)) {
            storage = readField.getLongStorage(rcvr);
          } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnsupportedOperationException();
          }
          i += 1;
          break;
        case 1:
          try {
            state.top = storage.readLong(rcvr);
          } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnsupportedOperationException();
          }
          i += 1;
          break;
        case 2:
          try {
            state.top = Math.multiplyExact(state.top, const1);
          } catch (ArithmeticException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnsupportedOperationException();
          }
          i += 1;
          break;
        case 3:
          try {
            state.top = Math.addExact(state.top, const2);
          } catch (ArithmeticException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnsupportedOperationException();
          }
          i += 1;
          break;
        case 4:
          state.top = state.top & const3;
          i += 1;
          break;
        case 5:
          storage.writeLong(rcvr, state.top);
          i += 1;
          break;
        case 6:
          return state.top;
      }
    }
  }
}
