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

  /**
   * <pre>
   * isWhiteSpace = (
        current = ' '  ifTrue: [^ true].
        current = '\t' ifTrue: [^ true].
        current = '\n' ifTrue: [^ true].
        current = '\r' ifTrue: [^ true].
        ^ false
      )
   * </pre>
   */
  public static final class JPIsWhiteSpace extends AbstractInvokable {
    @Child private AbstractReadFieldNode readCurrent;

    public JPIsWhiteSpace(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      readCurrent = FieldAccessorNode.createRead(4);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];

      Object current = readCurrent.read(rcvr);
      if (" ".equals(current)) {
        return true;
      }
      if ("\t".equals(current)) {
        return true;
      }
      if ("\n".equals(current)) {
        return true;
      }
      if ("\r".equals(current)) {
        return true;
      }

      return false;
    }
  }

  /**
   * <pre>
   * readValue = (
        current = 'n' ifTrue: [ ^ self readNull   ].
        current = 't' ifTrue: [ ^ self readTrue   ].
        current = 'f' ifTrue: [ ^ self readFalse  ].
        current = '"' ifTrue: [ ^ self readString ].
        current = '[' ifTrue: [ ^ self readArray  ].
        current = '{' ifTrue: [ ^ self readObject ].
  
        "Is this really the best way to write this?, or better #or:?,
         but with all the nesting, it's just ugly."
        current = '-' ifTrue: [ ^ self readNumber ].
        current = '0' ifTrue: [ ^ self readNumber ].
        current = '1' ifTrue: [ ^ self readNumber ].
        current = '2' ifTrue: [ ^ self readNumber ].
        current = '3' ifTrue: [ ^ self readNumber ].
        current = '4' ifTrue: [ ^ self readNumber ].
        current = '5' ifTrue: [ ^ self readNumber ].
        current = '6' ifTrue: [ ^ self readNumber ].
        current = '7' ifTrue: [ ^ self readNumber ].
        current = '8' ifTrue: [ ^ self readNumber ].
        current = '9' ifTrue: [ ^ self readNumber ].
  
        "else"
        self expected: 'value'
      )
   * </pre>
   */
  public static final class JPReadValue extends AbstractInvokable {
    @Child private AbstractReadFieldNode readCurrent;
    @Child private AbstractDispatchNode  dispatchReadNull;
    @Child private AbstractDispatchNode  dispatchReadTrue;
    @Child private AbstractDispatchNode  dispatchReadFalse;
    @Child private AbstractDispatchNode  dispatchReadString;
    @Child private AbstractDispatchNode  dispatchReadArray;
    @Child private AbstractDispatchNode  dispatchReadObject;
    @Child private AbstractDispatchNode  dispatchReadNumber;
    @Child private AbstractDispatchNode  dispatchExpected;

    public JPReadValue(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      readCurrent = FieldAccessorNode.createRead(4);
      dispatchReadNull = new UninitializedDispatchNode(SymbolTable.symbolFor("readNull"));
      dispatchReadTrue = new UninitializedDispatchNode(SymbolTable.symbolFor("readTrue"));
      dispatchReadFalse = new UninitializedDispatchNode(SymbolTable.symbolFor("readFalse"));
      dispatchReadString = new UninitializedDispatchNode(SymbolTable.symbolFor("readString"));
      dispatchReadArray = new UninitializedDispatchNode(SymbolTable.symbolFor("readArray"));
      dispatchReadObject = new UninitializedDispatchNode(SymbolTable.symbolFor("readObject"));
      dispatchReadNumber = new UninitializedDispatchNode(SymbolTable.symbolFor("readNumber"));
      dispatchExpected = new UninitializedDispatchNode(SymbolTable.symbolFor("expected:"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];

      Object current = readCurrent.read(rcvr);
      if ("n".equals(current)) {
        return dispatchReadNull.executeDispatch(frame, new Object[] {rcvr});
      }
      if ("t".equals(current)) {
        return dispatchReadTrue.executeDispatch(frame, new Object[] {rcvr});
      }
      if ("f".equals(current)) {
        return dispatchReadFalse.executeDispatch(frame, new Object[] {rcvr});
      }
      if ("\"".equals(current)) {
        return dispatchReadString.executeDispatch(frame, new Object[] {rcvr});
      }
      if ("[".equals(current)) {
        return dispatchReadArray.executeDispatch(frame, new Object[] {rcvr});
      }
      if ("{".equals(current)) {
        return dispatchReadObject.executeDispatch(frame, new Object[] {rcvr});
      }

      if ("-".equals(current) || "0".equals(current) || "1".equals(current)
          || "2".equals(current) || "3".equals(current) || "4".equals(current)
          || "5".equals(current) || "6".equals(current) || "7".equals(current)
          || "8".equals(current) || "9".equals(current)) {
        return dispatchReadNumber.executeDispatch(frame, new Object[] {rcvr});
      }

      dispatchExpected.executeDispatch(frame, new Object[] {rcvr, "value"});
      return rcvr;
    }
  }
}
