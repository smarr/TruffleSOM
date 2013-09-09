/**
 * Copyright (c) 2013 Stefan Marr, stefan.marr@vub.ac.be
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package som.interpreter.nodes;

import som.vm.Universe;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;


public class Method extends RootNode {

  @Child private final ExpressionNode expressionOrSequence;

  private final FrameSlot   selfSlot;
  @CompilationFinal private final FrameSlot[]  argumentSlots;
  @CompilationFinal private final FrameSlot[] temporarySlots;
  private final FrameSlot   nonLocalReturnMarker;
  private final Universe    universe;

  public Method(final ExpressionNode expressions,
                  final FrameSlot selfSlot,
                  final FrameSlot[] argumentSlots,
                  final FrameSlot[] temporarySlots,
                  final FrameSlot nonLocalReturnMarker,
                  final Universe  universe) {
    this.expressionOrSequence   = adoptChild(expressions);
    this.selfSlot      = selfSlot;
    this.argumentSlots = argumentSlots;
    this.temporarySlots = temporarySlots;
    this.nonLocalReturnMarker = nonLocalReturnMarker;
    this.universe      = universe;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    final FrameOnStackMarker marker = initializeFrame(frame.materialize());

    Object  result;
    boolean restart;

    do {
      restart = false;
      try {
        result = expressionOrSequence.executeGeneric(frame);
      } catch (ReturnException e) {
        if (!e.reachedTarget(marker)) {
          marker.frameNoLongerOnStack();
          throw e;
        } else {
          result = e.result();
        }
      } catch (RestartLoopException e) {
        restart = true;
        result  = null;
      }
    } while (restart);

    marker.frameNoLongerOnStack();
    return result;
  }

  @ExplodeLoop
  private FrameOnStackMarker initializeFrame(MaterializedFrame frame) {
    Object[] args = frame.getArguments(Arguments.class).arguments;
    try {
      for (int i = 0; i < argumentSlots.length; i++) {
        frame.setObject(argumentSlots[i], args[i]);
      }

      frame.setObject(selfSlot, frame.getArguments(Arguments.class).self);

      FrameOnStackMarker marker = new FrameOnStackMarker();
      frame.setObject(nonLocalReturnMarker, marker);

      for (int i = 0; i < temporarySlots.length; i++) {
        frame.setObject(temporarySlots[i], universe.nilObject);
      }

      return marker;
    } catch (FrameSlotTypeException e) {
     throw new RuntimeException("Should not happen, since we only have one type currently!");
    }
  }

  @Override
  public String toString() {
    final String name = "method-name-here";
    final String location = getSourceSection() == null ? "unknown" : getSourceSection().toString();
    return "Method " + name + ":" + location + "@" + Integer.toHexString(hashCode());
  }

}
