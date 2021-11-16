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

import static trufflesom.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.nodes.PreevaluatedExpression;
import trufflesom.interpreter.Invokable;
import trufflesom.vm.Universe;


public abstract class SInvokable extends SAbstractObject {

  public SInvokable(final SSymbol signature, final Invokable invokable,
      final SourceSection source) {
    this.signature = signature;
    this.sourceSection = source;

    this.invokable = invokable;
  }

  public static final class SMethod extends SInvokable {
    private final SMethod[] embeddedBlocks;

    public SMethod(final SSymbol signature, final Invokable invokable,
        final SMethod[] embeddedBlocks, final SourceSection source) {
      super(signature, invokable, source);
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
    public SClass getSOMClass(final Universe universe) {
      return universe.methodClass;
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
    public SPrimitive(final SSymbol signature, final Invokable invokable,
        final SourceSection source) {
      super(signature, invokable, source);
    }

    @Override
    public SClass getSOMClass(final Universe universe) {
      return universe.primitiveClass;
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

  public final SourceSection getSourceSection() {
    return sourceSection;
  }

  @TruffleBoundary
  public final RootCallTarget getCallTarget() {
    RootCallTarget ct = invokable.getCallTarget();
    if (ct == null) {
      ct = Truffle.getRuntime().createCallTarget(invokable);
    }
    return ct;
  }

  public final Invokable getInvokable() {
    return invokable;
  }

  public final SSymbol getSignature() {
    return signature;
  }

  public final SClass getHolder() {
    return holder;
  }

  public void setHolder(final SClass value) {
    transferToInterpreterAndInvalidate("SMethod.setHolder");
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

  private final SourceSection sourceSection;
  protected final Invokable   invokable;
  protected final SSymbol     signature;

  @CompilationFinal protected SClass holder;

  public boolean isTrivial() {
    return invokable.isTrivial();
  }

  public PreevaluatedExpression copyTrivialNode() {
    if (!isTrivial()) {
      throw new IllegalStateException();
    }
    PreevaluatedExpression n = invokable.copyTrivialNode();
    assert n != null;
    return n;
  }
}
