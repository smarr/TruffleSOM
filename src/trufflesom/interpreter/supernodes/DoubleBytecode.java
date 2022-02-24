package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterSwitch;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind;

import trufflesom.interpreter.nodes.NoPreEvalExprNode;


public class DoubleBytecode extends NoPreEvalExprNode {

  @CompilationFinal(dimensions = 1) private final byte[] bytecodes;

  private final long const1;
  private final long const2;
  private final long const3;

  public DoubleBytecode(final byte[] bytecodes, final long const1, final long const2,
      final long const3) {
    this.bytecodes = bytecodes;
    this.const1 = const1;
    this.const2 = const2;
    this.const3 = const3;
  }

  private static class State {
    double  a;
    double  b;
    boolean top;
  }

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
  @Override
  @ExplodeLoop(kind = LoopExplosionKind.MERGE_EXPLODE)
  @BytecodeInterpreterSwitch
  public Object executeGeneric(final VirtualFrame frame) {
    final Object[] args = frame.getArguments();

    final State state = new State();

    final byte[] bytecodes = this.bytecodes;

    CompilerAsserts.partialEvaluationConstant(bytecodes);
    CompilerAsserts.compilationConstant(bytecodes);
    CompilerDirectives.ensureVirtualized(state);

    // 0: read-arg1, ensure double
    // 1: read-arg2, ensure double
    // 2: a = b to top
    // 3: a < b to top
    // 4: a > b to top
    // 5: a = a to top
    // 6: return const1
    // 7: return const2
    // 8: return const3
    // 9: jump-on-false (+address)

    int i = 0;

    for (;;) {
      byte b = bytecodes[i];

      CompilerAsserts.partialEvaluationConstant(b);
      CompilerAsserts.compilationConstant(b);
      CompilerAsserts.partialEvaluationConstant(i);

      switch (b) {
        case 0: {
          Object arg = args[1];
          if (arg instanceof Double) {
            state.a = (Double) arg;
          } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnsupportedOperationException();
          }
          i += 1;
          break;
        }
        case 1: {
          Object arg = args[2];
          if (arg instanceof Double) {
            state.b = (Double) arg;
          } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnsupportedOperationException();
          }
          i += 1;
          break;
        }
        case 2: {
          state.top = state.a == state.b;
          i += 1;
          break;
        }
        case 3: {
          state.top = state.a < state.b;
          i += 1;
          break;
        }
        case 4: {
          state.top = state.a > state.b;
          i += 1;
          break;
        }
        case 5: {
          state.top = state.a == state.a;
          i += 1;
          break;
        }
        case 6: {
          return const1;
        }
        case 7: {
          return const2;
        }
        case 8: {
          return const3;
        }
        case 9: {
          if (!state.top) {
            i = bytecodes[i + 1];
          } else {
            i += 2;
          }
          break;
        }
      }
    }
  }
}
