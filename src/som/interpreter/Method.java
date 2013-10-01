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
package som.interpreter;

import som.interpreter.nodes.ExpressionNode;
import som.vm.Universe;
import som.vmobjects.SObject;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;


public class Method extends RootNode {

  @Child private ExpressionNode expressionOrSequence;

  private final FrameSlot   selfSlot;
//  @CompilationFinal private final FrameSlot[]  argumentSlots;
//  @CompilationFinal private final FrameSlot[] temporarySlots;

  @CompilationFinal private final FrameSlot argumentSlot1;
  @CompilationFinal private final FrameSlot argumentSlot2;
  @CompilationFinal private final FrameSlot argumentSlot3;
  @CompilationFinal private final FrameSlot argumentSlot4;

  @CompilationFinal private final FrameSlot temporarySlot1;
  @CompilationFinal private final FrameSlot temporarySlot2;
  @CompilationFinal private final FrameSlot temporarySlot3;
  @CompilationFinal private final FrameSlot temporarySlot4;

  private final FrameSlot   nonLocalReturnMarker;
  private final Universe    universe;

  private final FrameDescriptor frameDescriptor;

  public Method(final ExpressionNode expressions,
                final FrameSlot selfSlot,
                final FrameSlot[] argumentSlots,
                final FrameSlot[] temporarySlots,
                final FrameSlot nonLocalReturnMarker,
                final Universe  universe,
                final FrameDescriptor frameDescriptor) {
    this.expressionOrSequence = adoptChild(expressions);
    this.selfSlot        = selfSlot;

    if (argumentSlots.length > 4) {
      throw new RuntimeException("We did not expect that many arguments!");
    }

    if (temporarySlots.length > 4) {
      throw new RuntimeException("We did not expect that many temporaries!");
    }

    // --- init arguments ---
    if (argumentSlots.length == 4) { this.argumentSlot4 = argumentSlots[3]; } else { this.argumentSlot4 = null; }
    if (argumentSlots.length >= 3) { this.argumentSlot3 = argumentSlots[2]; } else { this.argumentSlot3 = null; }
    if (argumentSlots.length >= 2) { this.argumentSlot2 = argumentSlots[1]; } else { this.argumentSlot2 = null; }
    if (argumentSlots.length >= 1) { this.argumentSlot1 = argumentSlots[0]; } else { this.argumentSlot1 = null; }

    // --- init temps ---
    if (temporarySlots.length == 4) { this.temporarySlot4 = temporarySlots[3]; } else { this.temporarySlot4 = null; }
    if (temporarySlots.length >= 3) { this.temporarySlot3 = temporarySlots[2]; } else { this.temporarySlot3 = null; }
    if (temporarySlots.length >= 2) { this.temporarySlot2 = temporarySlots[1]; } else { this.temporarySlot2 = null; }
    if (temporarySlots.length >= 1) { this.temporarySlot1 = temporarySlots[0]; } else { this.temporarySlot1 = null; }

    this.nonLocalReturnMarker = nonLocalReturnMarker;
    this.universe        = universe;
    this.frameDescriptor = frameDescriptor;
  }

  @Override
  public Object execute(final VirtualFrame frame) {
    final FrameOnStackMarker marker = initializeFrame(this, frame);
    return messageSendExecution(marker, frame, expressionOrSequence);
  }

  public static SObject messageSendExecution(final FrameOnStackMarker marker,
      final VirtualFrame frame,
      final ExpressionNode expr) {
    SObject  result;
    boolean restart;

    do {
      restart = false;
      try {
        result = expr.executeGeneric(frame);
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

//  static int maxArgs = 0;
//  static int maxTemps = 0;
//
//  if (maxArgs  < method.argumentSlots.length)  { maxArgs  = method.argumentSlots.length;  System.out.println("MAX ARGS: " + maxArgs); }
//  if (maxTemps < method.temporarySlots.length) { maxTemps = method.temporarySlots.length; System.out.println("MAX TMPS: " + maxTemps);}


  // @ExplodeLoop
  public static FrameOnStackMarker initializeFrame(final Method method,
      final VirtualFrame frame) {
    frame.setObject(method.selfSlot, frame.getArguments(Arguments.class).self);

    final FrameOnStackMarker marker = new FrameOnStackMarker();
    frame.setObject(method.nonLocalReturnMarker, marker);

    Object[] args = frame.getArguments(Arguments.class).arguments;
//    for (int i = 0; i < method.argumentSlots.length; i++) {
//      frame.setObject(method.argumentSlots[i], args[i]);
//    }
    if (args.length > 0) {
      frame.setObject(method.argumentSlot1, args[0]);
      if (args.length > 1) {
        frame.setObject(method.argumentSlot2, args[1]);
        if (args.length > 2) {
          frame.setObject(method.argumentSlot3, args[2]);
          if (args.length > 3) {
            frame.setObject(method.argumentSlot4, args[3]);
          }
        }
      }
    }

//    for (int i = 0; i < method.temporarySlots.length; i++) {
//      frame.setObject(method.temporarySlots[i], method.universe.nilObject);
//    }

    if (method.temporarySlot1 != null) {
      frame.setObject(method.temporarySlot1, method.universe.nilObject);
      if (method.temporarySlot2 != null) {
        frame.setObject(method.temporarySlot2, method.universe.nilObject);
        if (method.temporarySlot3 != null) {
          frame.setObject(method.temporarySlot3, method.universe.nilObject);
          if (method.temporarySlot4 != null) {
            frame.setObject(method.temporarySlot4, method.universe.nilObject);
          }
        }
      }
    }

    return marker;
  }

  @Override
  public String toString() {
    SourceSection ss = getSourceSection();
    final String name = ss.getIdentifier();
    final String location = getSourceSection().toString();
    return "Method " + name + ":" + location + "@" + Integer.toHexString(hashCode());
  }

  public FrameDescriptor getFrameDescriptor() {
    return frameDescriptor;
  }

  public ExpressionNode methodCloneForInlining() {
    return expressionOrSequence.cloneForInlining();
  }
}
