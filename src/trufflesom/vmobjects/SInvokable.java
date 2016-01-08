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
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.SomLanguage;
import trufflesom.vm.Universe;


public abstract class SInvokable extends SAbstractObject {

  public SInvokable(final SSymbol signature, final Invokable invokable,
      final SourceSection source) {
    this.signature = signature;
    this.sourceSection = source;

    this.invokable = invokable;
    this.callTarget = invokable.createCallTarget();
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
    public void setHolder(final DynamicObject value) {
      super.setHolder(value);
      for (SMethod m : embeddedBlocks) {
        m.setHolder(value);
      }
    }

    @Override
    public DynamicObject getSOMClass(final Universe universe) {
      return universe.methodClass;
    }

    @Override
    public String getIdentifier(final Universe u) {
      if (holder != null) {
        return SClass.getName(holder, u).getString() + "." + signature.getString();
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
    public DynamicObject getSOMClass(final Universe universe) {
      return universe.primitiveClass;
    }

    @Override
    public String getIdentifier(final Universe u) {
      if (holder != null) {
        return SClass.getName(holder, u).getString() + "." + signature.getString();
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

  public final RootCallTarget getCallTarget() {
    return callTarget;
  }

  public final Invokable getInvokable() {
    return invokable;
  }

  public final SSymbol getSignature() {
    return signature;
  }

  public final DynamicObject getHolder() {
    return holder;
  }

  public void setHolder(final DynamicObject value) {
    transferToInterpreterAndInvalidate("SMethod.setHolder");
    holder = value;
  }

  public final int getNumberOfArguments() {
    return getSignature().getNumberOfSignatureArguments();
  }

  public final Object invoke(final Object[] arguments) {
    return callTarget.call(arguments);
  }

  public final Object invoke(final IndirectCallNode node,
      final Object[] arguments) {
    return node.call(callTarget, arguments);
  }

  @Override
  public final String toString() {
    // TODO: fixme: remove special case if possible, I think it indicates a bug
    if (holder == null) {
      return "Method(nil>>" + getSignature().toString() + ")";
    }

    Universe u = SomLanguage.getCurrentContext();
    return "Method(" + SClass.getName(getHolder(), u).getString() + ">>"
        + getSignature().toString() + ")";
  }

  public abstract String getIdentifier(Universe u);

  // Private variable holding Truffle runtime information
  private final SourceSection sourceSection;

  protected final Invokable    invokable;
  private final RootCallTarget callTarget;
  protected final SSymbol      signature;

  @CompilationFinal protected DynamicObject holder;
}
