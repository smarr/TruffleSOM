package trufflesom.interpreter.bc;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.vm.NotYetImplementedException;
import trufflesom.vmobjects.SObject;


public class Frame {

  public static Object getStackElement(final VirtualFrame frame, final int stackIndex,
      final FrameSlot stackPointer, final FrameSlot stackVar) {
    Object[] stack = (Object[]) frame.getValue(stackVar);
    return stack[getStackPointer(frame, stackPointer) - stackIndex];
  }

  public static int getStackPointer(final VirtualFrame frame, final FrameSlot stackPointer) {
    try {
      return frame.getInt(stackPointer);
    } catch (FrameSlotTypeException e) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      throw new RuntimeException(
          "Stack Pointer slot did store an int. There seems to be an interpreter bug.");
    }
  }

  public static void duplicateTopOfStack(final VirtualFrame frame,
      final FrameSlot stackPointer, final FrameSlot stackVar) {
    Object[] stack = (Object[]) frame.getValue(stackVar);
    int sp = getStackPointer(frame, stackPointer);
    Object top = stack[sp];
    doPush(frame, top, sp, stack, stackPointer);
  }

  private static void setStackPointer(final VirtualFrame frame, final int stackIndex,
      final FrameSlot stackPointer) {
    frame.setInt(stackPointer, stackIndex);
  }

  public static Object[] getCallArguments(final VirtualFrame frame,
      final int numberOfArguments) {
    throw new NotYetImplementedException();
  }

  public static void push(final VirtualFrame frame, final Object value,
      final FrameSlot stackPointer, final FrameSlot stackVar) {
    Object[] stack = (Object[]) frame.getValue(stackVar);
    int sp = getStackPointer(frame, stackPointer);
    doPush(frame, value, sp, stack, stackPointer);
  }

  public static void pop(final VirtualFrame frame, final FrameSlot stackPointer) {
    setStackPointer(frame, getStackPointer(frame, stackPointer) - 1, stackPointer);
  }

  public static Object popValue(final VirtualFrame frame, final FrameSlot stackPointer,
      final FrameSlot stackVar) {
    Object[] stack = (Object[]) frame.getValue(stackVar);
    int sp = getStackPointer(frame, stackPointer);
    Object value = stack[sp];
    setStackPointer(frame, sp - 1, stackPointer);
    return value;
  }

  private static void doPush(final VirtualFrame frame, final Object value,
      final int initialStackIndex, final Object[] stack, final FrameSlot stackPointer) {
    int newStackIndex = initialStackIndex + 1;
    setStackPointer(frame, newStackIndex, stackPointer);
    stack[newStackIndex] = value;
  }

  public static Object getArgument(final VirtualFrame frame, final int argIdx) {
    return frame.getArguments()[argIdx];
  }

  public static void setArgument(final VirtualFrame frame, final int argIdx,
      final Object value) {
    frame.getArguments()[argIdx] = value;
  }

  public static SObject getSelf(final VirtualFrame frame) {
    return (SObject) frame.getArguments()[0];
  }
}
