package trufflesom.interpreter.supernodes;

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


public class MiniBytecode extends NoPreEvalExprNode {

  @Child private AbstractDispatchNode dispatchNext;

  @CompilationFinal(dimensions = 1) private final byte[] bytecodes;

  public MiniBytecode(final byte[] bytecodes, final SSymbol next) {
    dispatchNext = new UninitializedDispatchNode(next);
    this.bytecodes = bytecodes;
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

    Object local1 = Nil.nilObject;
    Object local2 = Nil.nilObject;
    Object top = null;

    // bytecodes:
    // 0: read-arg-1, store-local-1
    // 1: read-arg-2, store-local-2
    // 2: jmp-if-local2-isNil (+ address)
    // 3: if-local1-isNil-return-true
    // 4: push-local-1
    // 5: push-local-2
    // 6: dispatch next
    // 7: pop-local-1
    // 8: pop-local-2
    // 9: jump (+ address)
    // 10: return false

    byte[] bytecodes = this.bytecodes;
    Object[] args = frame.getArguments();

    int i = 0;

    while (true) {
      byte b = bytecodes[i];

      switch (b) {
        case 0:
          local1 = args[1];
          i += 1;
          break;
        case 1:
          local2 = args[2];
          i += 1;
          break;
        case 2:
          if (local2 == Nil.nilObject) {
            i = bytecodes[i + 1];
          } else {
            i += 2;
          }
          break;
        case 3:
          if (local1 == Nil.nilObject) {
            return true;
          } else {
            i += 1;
          }
          break;
        case 4:
          top = local1;
          i += 1;
          break;
        case 5:
          top = local2;
          i += 1;
          break;
        case 6:
          top = dispatchNext.executeDispatch(frame, new Object[] {top});
          i += 1;
          break;
        case 7:
          local1 = top;
          i += 1;
          break;
        case 8:
          local2 = top;
          i += 1;
          break;
        case 9:
          i = bytecodes[i + 1];
          break;
        case 10:
          return false;
      }
    }
  }

}
