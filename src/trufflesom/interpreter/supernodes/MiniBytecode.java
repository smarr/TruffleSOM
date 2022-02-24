package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterSwitch;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind;

import trufflesom.interpreter.nodes.NoPreEvalExprNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SSymbol;


public final class MiniBytecode extends NoPreEvalExprNode {

  @Child private AbstractDispatchNode dispatch;

  @CompilationFinal(dimensions = 1) private final byte[] bytecodes;

  public MiniBytecode(final byte[] bytecodes, final SSymbol selector) {
    dispatch = new UninitializedDispatchNode(selector);
    this.bytecodes = bytecodes;
  }

  private static class State {
    Object local1 = Nil.nilObject;
    Object local2 = Nil.nilObject;
    Object top    = null;
  }

  /**
   * <pre>
   * isShorter: x than: y = (
        | xTail yTail |
  
        xTail := x. yTail := y.
        [ yTail isNil ]
            whileFalse: [
                xTail isNil ifTrue: [ ^true ].
                xTail := xTail next.
                yTail := yTail next ].
  
        ^false
    )
   * </pre>
   */
  @Override
  @ExplodeLoop(kind = LoopExplosionKind.MERGE_EXPLODE)
  @BytecodeInterpreterSwitch
  public Object executeGeneric(final VirtualFrame frame) {
    final State state = new State();
    CompilerDirectives.ensureVirtualized(state);

    // bytecodes:
    // 0: read-arg-1, store-local-1
    // 1: read-arg-2, store-local-2
    // 2: jmp-if-local2-isNil (+ address)
    // 3: if-local1-isNil-return-true
    // 4: push-local-1
    // 5: push-local-2
    // 6: dispatch unary selector
    // 7: pop-local-1
    // 8: pop-local-2
    // 9: jump (+ address)
    // 10: return false

    final byte[] bytecodes = this.bytecodes;
    Object[] args = frame.getArguments();

    CompilerAsserts.partialEvaluationConstant(bytecodes);
    CompilerAsserts.compilationConstant(bytecodes);

    int i = 0;

    for (;;) {
      byte b = bytecodes[i];
      CompilerAsserts.partialEvaluationConstant(b);
      CompilerAsserts.compilationConstant(b);
      CompilerAsserts.partialEvaluationConstant(i);

      switch (b) {
        case 0:
          state.local1 = args[1];
          i += 1;
          break;
        case 1:
          state.local2 = args[2];
          i += 1;
          break;
        case 2:
          if (state.local2 == Nil.nilObject) {
            i = bytecodes[i + 1];
          } else {
            i += 2;
          }
          break;
        case 3:
          if (state.local1 == Nil.nilObject) {
            return true;
          } else {
            i += 1;
          }
          break;
        case 4:
          state.top = state.local1;
          i += 1;
          break;
        case 5:
          state.top = state.local2;
          i += 1;
          break;
        case 6:
          state.top = dispatch.executeDispatch(frame, new Object[] {state.top});
          i += 1;
          break;
        case 7:
          state.local1 = state.top;
          i += 1;
          break;
        case 8:
          state.local2 = state.top;
          i += 1;
          break;
        case 9:
          i = bytecodes[i + 1];
          CompilerAsserts.partialEvaluationConstant(i);
          break;
        case 10:
          return false;
      }
    }
  }
}
