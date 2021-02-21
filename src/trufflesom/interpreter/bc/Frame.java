package trufflesom.interpreter.bc;

import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.vm.NotYetImplementedException;
import trufflesom.vmobjects.SObject;


public class Frame {

  public static Object getStackElement(final VirtualFrame frame, final int stackIndex) {
    return getStack(frame)[getStackPointer(frame) - stackIndex];
  }

  public static Object[] getStack(final VirtualFrame frame) {
    return null;
  }

  public static int getStackPointer(final VirtualFrame frame) {
    return -1;
  }

  public static void duplicateTopOfStack(final VirtualFrame frame) {
    Object[] stack = getStack(frame);
    int sp = getStackPointer(frame);
    Object top = stack[sp];
    doPush(frame, top, sp, stack);
  }

  private static void setStackPointer(final VirtualFrame frame, final int stackIndex) {
    throw new NotYetImplementedException();
  }

  public static Object[] getCallArguments(final VirtualFrame frame,
      final int numberOfArguments) {
    throw new NotYetImplementedException();
  }

  public static void push(final VirtualFrame frame, final Object value) {
    Object[] stack = getStack(frame);
    int sp = getStackPointer(frame);
    doPush(frame, value, sp, stack);
  }

  public static void pop(final VirtualFrame frame) {
    setStackPointer(frame, getStackPointer(frame) - 1);
  }

  public static Object popValue(final VirtualFrame frame) {
    Object[] stack = getStack(frame);
    int sp = getStackPointer(frame);
    Object value = stack[sp];
    setStackPointer(frame, sp - 1);
    return value;
  }

  private static void doPush(final VirtualFrame frame, final Object value,
      final int initialStackIndex, final Object[] stack) {
    int newStackIndex = initialStackIndex + 1;
    setStackPointer(frame, newStackIndex);
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
