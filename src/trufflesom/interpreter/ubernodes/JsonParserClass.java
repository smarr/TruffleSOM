package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;


public abstract class JsonParserClass {
  /**
   * <pre>
   * | input index line column current captureBuffer captureStart exceptionBlock |
   * read = (
        current = '\n' ifTrue: [
          line := line + 1.
          column := 0.
        ].

        index := index + 1.
        column := column + 1.

        input ifNil: [ self error:'input nil'].
        index <= input length
          ifTrue:  [ current := input charAt: index ]
          ifFalse: [ current := nil ]
     )
   * </pre>
   */
  public static final class JPRead extends AbstractInvokable {
    @CompilationFinal private boolean nilInput;

    @Child private AbstractReadFieldNode readInput;
    @Child private AbstractReadFieldNode readIndex;
    @Child private AbstractReadFieldNode readLine;
    @Child private AbstractReadFieldNode readColumn;
    @Child private AbstractReadFieldNode readCurrent;

    @Child private AbstractWriteFieldNode writeIndex;
    @Child private AbstractWriteFieldNode writeLine;
    @Child private AbstractWriteFieldNode writeColumn;
    @Child private AbstractWriteFieldNode writeCurrent;

    @Child private AbstractDispatchNode dispatchError;

    public JPRead(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      readInput = FieldAccessorNode.createRead(0);
      readIndex = FieldAccessorNode.createRead(1);
      readLine = FieldAccessorNode.createRead(2);
      readColumn = FieldAccessorNode.createRead(3);
      readCurrent = FieldAccessorNode.createRead(4);

      writeIndex = FieldAccessorNode.createWrite(1);
      writeLine = FieldAccessorNode.createWrite(2);
      writeColumn = FieldAccessorNode.createWrite(3);
      writeCurrent = FieldAccessorNode.createWrite(4);

      dispatchError = new UninitializedDispatchNode(SymbolTable.symbolFor("error:"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      SObject rcvr = (SObject) frame.getArguments()[0];

      Object current = readCurrent.read(rcvr);
      if ("\n".equals(current)) {
        try {
          long sum = Math.addExact(readLine.readLongSafe(rcvr), 1L);
          writeLine.write(rcvr, sum);
        } catch (ArithmeticException e) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          throw new UnsupportedOperationException();
        }

        writeColumn.write(rcvr, 0L);
      }

      // index := index + 1.
      long index;
      try {
        index = Math.addExact(readIndex.readLongSafe(rcvr), 1L);
        writeIndex.write(rcvr, index);
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException();
      }

      // column := column + 1.
      try {
        long sum = Math.addExact(readColumn.readLongSafe(rcvr), 1L);
        writeColumn.write(rcvr, sum);
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException();
      }

      // input ifNil: [ self error:'input nil'].
      Object input = readInput.read(rcvr);
      if (input == Nil.nilObject) {
        if (!nilInput) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          nilInput = true;
        }

        dispatchError.executeDispatch(frame, new Object[] {rcvr, "input nil"});
      }

      // index <= input length
      // ifTrue: [ current := input charAt: index ]
      if (index <= ((String) input).length()) {
        writeCurrent.write(rcvr, ((String) input).substring(((int) index - 1), (int) index));
      } else {
        // ifFalse: [ current := nil ]
        writeCurrent.write(rcvr, Nil.nilObject);
      }

      return rcvr;
    }
  }

  /**
   * <pre>
   * readChar: ch = (
      current = ch ifFalse: [ ^ false ].
      self read.
      ^ true
    )
   * </pre>
   */
  public static final class JPReadChar extends AbstractInvokable {
    @Child private AbstractReadFieldNode readCurrent;
    @Child private AbstractDispatchNode  dispatchRead;

    public JPReadChar(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      readCurrent = FieldAccessorNode.createRead(4);
      dispatchRead = new UninitializedDispatchNode(SymbolTable.symbolFor("read"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      String ch = (String) args[1];
      if (!ch.equals(readCurrent.read(rcvr))) {
        return false;
      }

      dispatchRead.executeDispatch(frame, new Object[] {rcvr});

      return true;
    }
  }

  /**
   * <pre>
   * isDigit = (
    current = '0' ifTrue: [^ true].
    current = '1' ifTrue: [^ true].
    current = '2' ifTrue: [^ true].
    current = '3' ifTrue: [^ true].
    current = '4' ifTrue: [^ true].
    current = '5' ifTrue: [^ true].
    current = '6' ifTrue: [^ true].
    current = '7' ifTrue: [^ true].
    current = '8' ifTrue: [^ true].
    current = '9' ifTrue: [^ true].
    ^ false
  )
   * </pre>
   */
  public static final class JPIsDigit extends AbstractInvokable {
    @Child private AbstractReadFieldNode readCurrent;

    public JPIsDigit(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      readCurrent = FieldAccessorNode.createRead(4);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];

      Object current = readCurrent.read(rcvr);
      if ("0".equals(current)) {
        return true;
      }
      if ("1".equals(current)) {
        return true;
      }
      if ("2".equals(current)) {
        return true;
      }
      if ("3".equals(current)) {
        return true;
      }
      if ("4".equals(current)) {
        return true;
      }
      if ("5".equals(current)) {
        return true;
      }
      if ("6".equals(current)) {
        return true;
      }
      if ("7".equals(current)) {
        return true;
      }
      if ("8".equals(current)) {
        return true;
      }
      if ("9".equals(current)) {
        return true;
      }

      return false;
    }
  }
}
