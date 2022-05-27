/**
 * Copyright (c) 2013 Stefan Marr,   stefan.marr@vub.ac.be
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
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

package trufflesom.vmobjects;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import bdt.primitives.nodes.PreevaluatedExpression;
import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.vm.Classes;


public abstract class SInvokable extends SAbstractObject {

  public SInvokable(final SSymbol signature, final AbstractInvokable invokable) {
    this.signature = signature;
    this.invokable = invokable;
  }

  public static final class SMethod extends SInvokable {
    private final SMethod[] embeddedBlocks;

    public SMethod(final SSymbol signature, final AbstractInvokable invokable,
        final SMethod[] embeddedBlocks) {
      super(signature, invokable);
      this.embeddedBlocks = embeddedBlocks;
    }

    public SMethod[] getEmbeddedBlocks() {
      return embeddedBlocks;
    }

    @Override
    public void setHolder(final SClass value) {
      super.setHolder(value);
      for (SMethod m : embeddedBlocks) {
        m.setHolder(value);
      }
    }

    @Override
    public SClass getSOMClass() {
      return Classes.methodClass;
    }

    @Override
    public String getIdentifier() {
      if (holder != null) {
        return holder.getName().getString() + "." + signature.getString();
      } else if (invokable.getSourceSection() != null) {
        // TODO find a better solution than charIndex
        Path absolute = Paths.get(invokable.getSourceSection().getSource().getURI());
        return absolute.toString() + ":" + invokable.getSourceSection().getCharIndex() + ":"
            + signature.getString();
      } else {
        return signature.toString();
      }
    }
  }

  public static final class SPrimitive extends SInvokable {
    public SPrimitive(final SSymbol signature, final Invokable invokable) {
      super(signature, invokable);
    }

    @Override
    public SClass getSOMClass() {
      return Classes.primitiveClass;
    }

    @Override
    public String getIdentifier() {
      if (holder != null) {
        return holder.getName().getString() + "." + signature.getString();
      } else if (invokable.getSourceSection() != null) {
        // TODO find a better solution than charIndex
        Path absolute = Paths.get(invokable.getSourceSection().getSource().getURI());
        return absolute.toString() + ":" + invokable.getSourceSection().getCharIndex() + ":"
            + signature.getString();
      } else {
        return signature.toString();
      }
    }
  }

  public final Source getSource() {
    return invokable.getSource();
  }

  public final long getSourceCoordinate() {
    return invokable.getSourceCoordinate();
  }

  public final SourceSection getSourceSection() {
    return invokable.getSourceSection();
  }

  @TruffleBoundary
  public final RootCallTarget getCallTarget() {
    RootCallTarget ct = invokable.getCallTarget();
    assert ct != null;
    return ct;
  }

  public final AbstractInvokable getInvokable() {
    return invokable;
  }

  public final SSymbol getSignature() {
    return signature;
  }

  public final SClass getHolder() {
    return holder;
  }

  public void setHolder(final SClass value) {
    transferToInterpreterAndInvalidate();
    holder = value;
    invokable.setHolder(value);
  }

  public final int getNumberOfArguments() {
    return getSignature().getNumberOfSignatureArguments();
  }

  public final Object invoke(final Object[] arguments) {
    return getCallTarget().call(arguments);
  }

  public final Object invoke(final IndirectCallNode node, final Object[] arguments) {
    return node.call(getCallTarget(), arguments);
  }

  @Override
  public final String toString() {
    // TODO: fixme: remove special case if possible, I think it indicates a bug
    if (holder == null) {
      return "Method(nil>>" + getSignature().toString() + ")";
    }

    return "Method(" + getHolder().getName().getString() + ">>" + getSignature().toString()
        + ")";
  }

  public abstract String getIdentifier();

  protected final AbstractInvokable invokable;
  protected final SSymbol           signature;

  @CompilationFinal protected SClass holder;

  public boolean isTrivial() {
    return invokable.isTrivial();
  }

  public PreevaluatedExpression copyTrivialNode() {
    return invokable.copyTrivialNode();
  }

  public AbstractDispatchNode asDispatchNode(final Object rcvr,
      final AbstractDispatchNode next) {
    return invokable.asDispatchNode(rcvr, next);
  }
}
