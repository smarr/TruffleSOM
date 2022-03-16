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
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;


public final class InitMethodBytecodes extends NoPreEvalExprNode {

  @Children private FieldWriteNode[] writeField;

  @CompilationFinal(dimensions = 1) private final byte[] bytecodes;

  private final Object constant;

  public InitMethodBytecodes(final byte[] bytecodes, final int[] fields,
      final Object constant) {
    assert bytecodes.length == fields.length;
    this.writeField = new FieldWriteNode[fields.length];

    for (int i = 0; i < fields.length; i += 1) {
      this.writeField[i] = FieldWriteNodeGen.create(fields[i], null, null);
    }

    this.bytecodes = bytecodes;
    this.constant = constant;
  }

  /**
   * <pre>
   * Vector2D >> #initX: anX y: aY = (
        x := anX.
        y := aY
      )

     Random >> #initialize = (
       seed := 74755
     )

  TODO: exclude from benchmark?
     Vector >> #initialize: size = (
        storage := Array new: size.
        first := 1.
        last  := 1.
     )

     BenchmarkHarness >> #initialize = (
        total         := 0.
        numIterations := 1.
        innerIterations := 1.
        printAll      := true.
        doGC          := false.
     )
  
     Planner >> #initialize = (
        "Planner initialize"

        currentMark := 1
     )
  
    TODO: exclude from benchmark? Replace by Vector2D?
     JsonLiteral >> #initializeWith: val = (
        value   := val.
        isNull  := 'null'  = val.
        isTrue  := 'true'  = val.
        isFalse := 'false' = val.
     )

     JsonParser >> #initializeWith: string = (
      input := string.
      index := 0.
      line  := 1.
      column := 0.
      current := nil.
      captureBuffer := ''.
    )

    ParseException >> #initializeWith: message at: anOffset line: aLine column: aColumn = (
      msg    := message.
      offset := anOffset.
      line   := aLine.
      column := aColumn.
    )

    Edge >> #initializeWith: destination and: w = (
      dest   := destination.
      weight := w.
    )
  
    Lexer >> #initialize: aString = (
      fileContent := aString.
      peekDone := false.
      index := 1.
    )

    SBlock >> #initialize: aSMethod in: aContext with: aBlockClass = (
      method := aSMethod.
      context := aContext.
      blockClass := aBlockClass.
    )

    SMethod >> #initializeWith: aSSymbol bc: bcArray literals: literalsArray numLocals: numLocals maxStack: maxStack = (
      signature := aSSymbol.
      bytecodes := bcArray.
      literals := literalsArray.
      numberOfLocals := numLocals.
      maximumNumberOfStackElements := maxStack.
    )
  
    SPrimitive >> #initialize: aSSymbol with: aBlock = (
      signature := aSSymbol.
      isEmpty := false.
      operation := aBlock.
    )
   *
   * This results in the following behavior:
   *  0: store-arg1 into field
      1: store-arg2 into field
      2: store-arg3 into field
      3: store-arg4 into field
      4: store-arg5 into field
      5: store-0 into field
      6: store-1 into field
  
      7: store-true into field
      8: store-false into field
      9: store-nil into field
      10: store-int into field
      11: store-object-const into field
  
      - there are at most 6 fields
      - all return self/rcvr
   * </pre>
   */
  @Override
  @ExplodeLoop(kind = LoopExplosionKind.MERGE_EXPLODE)
  @BytecodeInterpreterSwitch
  public Object executeGeneric(final VirtualFrame frame) {
    final Object[] args = frame.getArguments();
    final SObject rcvr = (SObject) args[0];

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
          writeField[i].executeEvaluated(frame, rcvr, args[1]);
          i += 1;
          break;
        case 1:
          writeField[i].executeEvaluated(frame, rcvr, args[2]);
          i += 1;
          break;
        case 2:
          writeField[i].executeEvaluated(frame, rcvr, args[3]);
          i += 1;
          break;
        case 3:
          writeField[i].executeEvaluated(frame, rcvr, args[4]);
          i += 1;
          break;
        case 4:
          writeField[i].executeEvaluated(frame, rcvr, args[5]);
          i += 1;
          break;
        case 5:
          writeField[i].doLong(frame, rcvr, 0);
          i += 1;
          break;
        case 6:
          writeField[i].doLong(frame, rcvr, 1);
          i += 1;
          break;
        case 7:
          writeField[i].doObject(frame, rcvr, true);
          i += 1;
          break;
        case 8:
          writeField[i].doObject(frame, rcvr, false);
          i += 1;
          break;
        case 9:
          writeField[i].doObject(frame, rcvr, Nil.nilObject);
          i += 1;
          break;
        case 10:
          writeField[i].doLong(frame, rcvr, (Long) constant);
          i += 1;
          break;
        case 11:
          writeField[i].doObject(frame, rcvr, constant);
          i += 1;
          break;
      }
    }

    return rcvr;
  }

}
