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
package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;

import bdt.inlining.ScopeAdaptationVisitor;
import bdt.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Internal;
import trufflesom.interpreter.FrameOnStackMarker;
import trufflesom.interpreter.ReturnException;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SBlock;


public final class ReturnNonLocalNode extends ContextualNode {

  @Child private ExpressionNode expression;
  private final BranchProfile   blockEscaped;
  private final Internal        onStackMarkerVar;

  private final int onStackMarkerIndex;

  public ReturnNonLocalNode(final ExpressionNode expression, final Internal onStackMarkerVar,
      final int outerSelfContextLevel) {
    super(outerSelfContextLevel);
    assert outerSelfContextLevel > 0;
    this.expression = expression;
    this.blockEscaped = BranchProfile.create();
    this.onStackMarkerVar = onStackMarkerVar;
    this.onStackMarkerIndex = onStackMarkerVar.getIndex();
  }

  public ReturnNonLocalNode(final ReturnNonLocalNode node) {
    this(node.expression, node.onStackMarkerVar, node.contextLevel);
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object result = expression.executeGeneric(frame);

    MaterializedFrame ctx = determineContext(frame);
    FrameOnStackMarker marker = (FrameOnStackMarker) ctx.getObject(onStackMarkerIndex);

    if (marker.isOnStack()) {
      throw new ReturnException(result, marker);
    } else {
      blockEscaped.enter();
      SBlock block = (SBlock) frame.getArguments()[0];
      Object self = ctx.getArguments()[0];
      return SAbstractObject.sendEscapedBlock(self, block);
    }
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    ScopeElement<ExpressionNode> se = inliner.getAdaptedVar(onStackMarkerVar);

    if (se.var != onStackMarkerVar || se.contextLevel < contextLevel) {
      ExpressionNode node;
      if (se.contextLevel == 0) {
        node = new ReturnLocalNode(expression, (Internal) se.var);
      } else {
        node = new ReturnNonLocalNode(expression, (Internal) se.var, se.contextLevel);
      }
      node.initialize(sourceCoord);
      replace(node);
    }
  }

  /**
   * Normally, there are no local returns in SOM. However, after
   * inlining/embedding of blocks, we need this ReturnLocalNode to replace
   * previous non-local returns.
   *
   * @author Stefan Marr
   */
  public static final class ReturnLocalNode extends NoPreEvalExprNode {
    @Child private ExpressionNode expression;

    private final Internal onStackMarkerVar;

    private final int onStackMarkerIndex;

    private ReturnLocalNode(final ExpressionNode exp, final Internal onStackMarker) {
      this.expression = exp;

      this.onStackMarkerVar = onStackMarker;
      this.onStackMarkerIndex = onStackMarker.getIndex();
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      Object result = expression.executeGeneric(frame);
      FrameOnStackMarker marker = (FrameOnStackMarker) frame.getObject(onStackMarkerIndex);

      // this ReturnLocalNode should only become part of an AST because of
      // inlining a literal block, and that block, should never be
      // captured as a value and passed around. Because, we should only ever
      // do the inlining for blocks where we know this doesn't happen.
      assert marker.isOnStack();
      throw new ReturnException(result, marker);
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      ScopeElement<ExpressionNode> se = inliner.getAdaptedVar(onStackMarkerVar);
      if (se.var != onStackMarkerVar) {
        ReturnLocalNode node = new ReturnLocalNode(expression, (Internal) se.var);
        node.initialize(sourceCoord);
        replace(node);
      }
    }
  }

  public static final class CatchNonLocalReturnNode extends NoPreEvalExprNode {
    @Child protected ExpressionNode methodBody;

    private final BranchProfile nonLocalReturnHandler;
    private final BranchProfile doCatch;
    private final BranchProfile doPropagate;
    private final Internal      onStackMarkerVar;

    private final int onStackMarkerIndex;

    public CatchNonLocalReturnNode(final ExpressionNode methodBody,
        final Internal onStackMarker) {
      this.methodBody = methodBody;
      this.nonLocalReturnHandler = BranchProfile.create();
      this.onStackMarkerVar = onStackMarker;
      this.onStackMarkerIndex = onStackMarker.getIndex();

      this.doCatch = BranchProfile.create();
      this.doPropagate = BranchProfile.create();
    }

    @Override
    public ExpressionNode getFirstMethodBodyNode() {
      return methodBody;
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      FrameOnStackMarker marker = new FrameOnStackMarker();
      frame.setObject(onStackMarkerIndex, marker);

      try {
        return methodBody.executeGeneric(frame);
      } catch (ReturnException e) {
        nonLocalReturnHandler.enter();
        if (!e.reachedTarget(marker)) {
          doPropagate.enter();
          throw e;
        } else {
          doCatch.enter();
          return e.result();
        }
      } finally {
        marker.frameNoLongerOnStack();
      }
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      ScopeElement<ExpressionNode> se = inliner.getAdaptedVar(onStackMarkerVar);
      if (se.var != onStackMarkerVar) {
        replace(new CatchNonLocalReturnNode(
            methodBody, (Internal) se.var).initialize(sourceCoord));
      }
    }
  }
}
