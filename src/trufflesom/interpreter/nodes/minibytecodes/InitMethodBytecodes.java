package trufflesom.interpreter.nodes.minibytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterSwitch;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind;

import trufflesom.interpreter.nodes.FieldNode.FieldWriteNode;
import trufflesom.interpreter.nodes.FieldNodeFactory.FieldWriteNodeGen;
import trufflesom.interpreter.nodes.NoPreEvalExprNode;
import trufflesom.vmobjects.SObject;


public final class InitMethodBytecodes extends NoPreEvalExprNode {

  @Child private FieldWriteNode writeField1;
  @Child private FieldWriteNode writeField2;

  @CompilationFinal(dimensions = 1) private final byte[] bytecodes;

  public InitMethodBytecodes(final byte[] bytecodes, final int fieldIdx1,
      final int fieldIdx2) {
    this.writeField1 = FieldWriteNodeGen.create(fieldIdx1, null, null);
    this.writeField2 = FieldWriteNodeGen.create(fieldIdx2, null, null);
    this.bytecodes = bytecodes;
  }

  /**
   * <pre>
   *  initX: anX y: aY = (
        x := anX.
        y := aY
      )
   * </pre>
   */
  @Override
  @ExplodeLoop(kind = LoopExplosionKind.MERGE_EXPLODE)
  @BytecodeInterpreterSwitch
  public Object executeGeneric(final VirtualFrame frame) {
    final Object[] args = frame.getArguments();
    final SObject rcvr = (SObject) args[0];

    // 0: read-arg-1, store-field-1
    // 1: read-arg-2, store-field-2

    final byte[] bytecodes = this.bytecodes;

    CompilerAsserts.partialEvaluationConstant(bytecodes);
    CompilerAsserts.compilationConstant(bytecodes);

    int i = 0;

    while (i < bytecodes.length) {
      byte b = bytecodes[i];

      CompilerAsserts.partialEvaluationConstant(b);
      CompilerAsserts.compilationConstant(b);
      CompilerAsserts.partialEvaluationConstant(i);

      switch (b) {
        case 0:
          writeField1.executeEvaluated(frame, rcvr, args[1]);
          i += 1;
          break;
        case 1:
          writeField2.executeEvaluated(frame, rcvr, args[2]);
          i += 1;
          break;
      }
    }

    return rcvr;
  }

}
