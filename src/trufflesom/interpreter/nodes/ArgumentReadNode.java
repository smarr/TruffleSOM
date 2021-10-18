package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import bd.inlining.ScopeAdaptationVisitor;
import bd.tools.nodes.Invocation;
import trufflesom.compiler.Variable.Argument;
import trufflesom.vmobjects.SSymbol;


public abstract class ArgumentReadNode {

  public abstract static class LocalArgumentRead extends ExpressionNode
      implements Invocation<SSymbol> {
    protected final Argument arg;

    LocalArgumentRead(final Argument arg) {
      this.arg = arg;
    }

    @Override
    public final void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateRead(arg, this, 0);
    }

    @Override
    public final SSymbol getInvocationIdentifier() {
      return arg.name;
    }

    @Override
    public final String toString() {
      return "ArgRead(" + arg.name + ")";
    }

    public abstract boolean isSelfRead();
  }

  public static class LocalArgumentReadNode extends LocalArgumentRead {
    public final int argumentIndex;

    public LocalArgumentReadNode(final Argument arg) {
      super(arg);
      assert arg.index >= 4;
      this.argumentIndex = arg.index;
    }

    /** Only to be used in primitives. */
    public LocalArgumentReadNode(final boolean useInPrim, final int idx) {
      super(null);
      assert idx >= 4;
      this.argumentIndex = idx;
      assert useInPrim;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      return frame.getArguments()[argumentIndex];
    }

    @Override
    public boolean isSelfRead() {
      return false;
    }
  }

  public static class LocalArgument1ReadNode extends LocalArgumentRead {

    public LocalArgument1ReadNode(final Argument arg) {
      super(arg);
    }

    /** Only to be used in primitives. */
    public LocalArgument1ReadNode(final boolean useInPrim) {
      super(null);
      assert useInPrim;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      return frame.getArgument1();
    }

    @Override
    public boolean isSelfRead() {
      return true;
    }
  }

  public static class LocalArgument2ReadNode extends LocalArgumentRead {

    public LocalArgument2ReadNode(final Argument arg) {
      super(arg);
    }

    /** Only to be used in primitives. */
    public LocalArgument2ReadNode(final boolean useInPrim) {
      super(null);
      assert useInPrim;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      return frame.getArgument2();
    }

    @Override
    public boolean isSelfRead() {
      return false;
    }
  }

  public static class LocalArgument3ReadNode extends LocalArgumentRead {

    public LocalArgument3ReadNode(final Argument arg) {
      super(arg);
    }

    /** Only to be used in primitives. */
    public LocalArgument3ReadNode(final boolean useInPrim) {
      super(null);
      assert useInPrim;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      return frame.getArgument3();
    }

    @Override
    public boolean isSelfRead() {
      return false;
    }
  }

  public static class LocalArgument4ReadNode extends LocalArgumentRead {

    public LocalArgument4ReadNode(final Argument arg) {
      super(arg);
    }

    /** Only to be used in primitives. */
    public LocalArgument4ReadNode(final boolean useInPrim) {
      super(null);
      assert useInPrim;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      return frame.getArgument4();
    }

    @Override
    public boolean isSelfRead() {
      return false;
    }
  }

  public static class LocalArgumentWriteNode extends ExpressionNode {
    protected final int      argumentIndex;
    protected final Argument arg;

    @Child protected ExpressionNode valueNode;

    public LocalArgumentWriteNode(final Argument arg, final ExpressionNode valueNode) {
      assert arg.index >= 4;
      this.arg = arg;
      this.argumentIndex = arg.index;
      this.valueNode = valueNode;
    }

    /** Only to be used in primitives. */
    public LocalArgumentWriteNode(final boolean useInPrim, final int idx,
        final ExpressionNode valueNode) {
      assert idx >= 0;
      this.arg = null;
      this.argumentIndex = idx;
      assert useInPrim;
      this.valueNode = valueNode;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      Object value = valueNode.executeGeneric(frame);
      frame.getArguments()[argumentIndex] = value;
      return value;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateWrite(arg, this, valueNode, 0);
    }
  }

  public static class LocalArgument1WriteNode extends ExpressionNode {
    protected final Argument arg;

    @Child protected ExpressionNode valueNode;

    public LocalArgument1WriteNode(final Argument arg, final ExpressionNode valueNode) {
      this.arg = arg;
      this.valueNode = valueNode;
    }

    /** Only to be used in primitives. */
    public LocalArgument1WriteNode(final boolean useInPrim, final int idx,
        final ExpressionNode valueNode) {
      this.arg = null;
      assert useInPrim;
      this.valueNode = valueNode;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      Object value = valueNode.executeGeneric(frame);
      frame.setArgument1(value);
      return value;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateWrite(arg, this, valueNode, 0);
    }
  }

  public static class LocalArgument2WriteNode extends ExpressionNode {
    protected final Argument arg;

    @Child protected ExpressionNode valueNode;

    public LocalArgument2WriteNode(final Argument arg, final ExpressionNode valueNode) {
      this.arg = arg;
      this.valueNode = valueNode;
    }

    /** Only to be used in primitives. */
    public LocalArgument2WriteNode(final boolean useInPrim, final int idx,
        final ExpressionNode valueNode) {
      this.arg = null;
      assert useInPrim;
      this.valueNode = valueNode;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      Object value = valueNode.executeGeneric(frame);
      frame.setArgument2(value);
      return value;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateWrite(arg, this, valueNode, 0);
    }
  }

  public static class LocalArgument3WriteNode extends ExpressionNode {
    protected final Argument arg;

    @Child protected ExpressionNode valueNode;

    public LocalArgument3WriteNode(final Argument arg, final ExpressionNode valueNode) {
      this.arg = arg;
      this.valueNode = valueNode;
    }

    /** Only to be used in primitives. */
    public LocalArgument3WriteNode(final boolean useInPrim, final int idx,
        final ExpressionNode valueNode) {
      this.arg = null;
      assert useInPrim;
      this.valueNode = valueNode;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      Object value = valueNode.executeGeneric(frame);
      frame.setArgument3(value);
      return value;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateWrite(arg, this, valueNode, 0);
    }
  }

  public static class LocalArgument4WriteNode extends ExpressionNode {
    protected final Argument arg;

    @Child protected ExpressionNode valueNode;

    public LocalArgument4WriteNode(final Argument arg, final ExpressionNode valueNode) {
      this.arg = arg;
      this.valueNode = valueNode;
    }

    /** Only to be used in primitives. */
    public LocalArgument4WriteNode(final boolean useInPrim, final int idx,
        final ExpressionNode valueNode) {
      this.arg = null;
      assert useInPrim;
      this.valueNode = valueNode;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      Object value = valueNode.executeGeneric(frame);
      frame.setArgument4(value);
      return value;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateWrite(arg, this, valueNode, 0);
    }
  }

  public static class NonLocalArgumentReadNode extends ContextualNode
      implements Invocation<SSymbol> {
    protected final int      argumentIndex;
    protected final Argument arg;

    public NonLocalArgumentReadNode(final Argument arg, final int contextLevel) {
      super(contextLevel);
      assert contextLevel > 0;
      assert arg.index >= 2;
      this.arg = arg;
      this.argumentIndex = arg.index;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      return determineContext(frame).getArguments()[argumentIndex];
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateRead(arg, this, contextLevel);
    }

    @Override
    public SSymbol getInvocationIdentifier() {
      return arg.name;
    }

    @Override
    public String toString() {
      return "ArgRead(" + arg.name + ", ctx: " + contextLevel + ")";
    }
  }

  public static class NonLocalArgument1ReadNode extends ContextualNode
      implements Invocation<SSymbol> {

    protected final Argument arg;

    public NonLocalArgument1ReadNode(final Argument arg, final int contextLevel) {
      super(contextLevel);
      assert contextLevel > 0;
      this.arg = arg;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      return determineContext(frame).getArgument1();
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateRead(arg, this, contextLevel);
    }

    @Override
    public SSymbol getInvocationIdentifier() {
      return arg.name;
    }

    @Override
    public String toString() {
      return "ArgRead(" + arg.name + ", ctx: " + contextLevel + ")";
    }
  }

  public static class NonLocalArgument2ReadNode extends ContextualNode
      implements Invocation<SSymbol> {

    protected final Argument arg;

    public NonLocalArgument2ReadNode(final Argument arg, final int contextLevel) {
      super(contextLevel);
      assert contextLevel > 0;
      this.arg = arg;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      return determineContext(frame).getArgument2();
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateRead(arg, this, contextLevel);
    }

    @Override
    public SSymbol getInvocationIdentifier() {
      return arg.name;
    }

    @Override
    public String toString() {
      return "ArgRead(" + arg.name + ", ctx: " + contextLevel + ")";
    }
  }

  public static class NonLocalArgument3ReadNode extends ContextualNode
      implements Invocation<SSymbol> {

    protected final Argument arg;

    public NonLocalArgument3ReadNode(final Argument arg, final int contextLevel) {
      super(contextLevel);
      assert contextLevel > 0;
      this.arg = arg;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      return determineContext(frame).getArgument3();
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateRead(arg, this, contextLevel);
    }

    @Override
    public SSymbol getInvocationIdentifier() {
      return arg.name;
    }

    @Override
    public String toString() {
      return "ArgRead(" + arg.name + ", ctx: " + contextLevel + ")";
    }
  }

  public static class NonLocalArgument4ReadNode extends ContextualNode
      implements Invocation<SSymbol> {

    protected final Argument arg;

    public NonLocalArgument4ReadNode(final Argument arg, final int contextLevel) {
      super(contextLevel);
      assert contextLevel > 0;
      this.arg = arg;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      return determineContext(frame).getArgument4();
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateRead(arg, this, contextLevel);
    }

    @Override
    public SSymbol getInvocationIdentifier() {
      return arg.name;
    }

    @Override
    public String toString() {
      return "ArgRead(" + arg.name + ", ctx: " + contextLevel + ")";
    }
  }

  public static class NonLocalArgumentWriteNode extends ContextualNode {
    protected final int      argumentIndex;
    protected final Argument arg;

    @Child protected ExpressionNode valueNode;

    public NonLocalArgumentWriteNode(final Argument arg, final int contextLevel,
        final ExpressionNode valueNode) {
      super(contextLevel);
      assert contextLevel > 0;
      this.arg = arg;
      this.argumentIndex = arg.index;

      this.valueNode = valueNode;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      Object value = valueNode.executeGeneric(frame);
      determineContext(frame).getArguments()[argumentIndex] = value;
      return value;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateWrite(arg, this, valueNode, contextLevel);
    }
  }

  public static class NonLocalArgument1WriteNode extends ContextualNode {
    protected final Argument arg;

    @Child protected ExpressionNode valueNode;

    public NonLocalArgument1WriteNode(final Argument arg, final int contextLevel,
        final ExpressionNode valueNode) {
      super(contextLevel);
      assert contextLevel > 0;
      this.arg = arg;
      this.valueNode = valueNode;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      Object value = valueNode.executeGeneric(frame);
      determineContext(frame).setArgument1(value);
      return value;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateWrite(arg, this, valueNode, contextLevel);
    }
  }

  public static class NonLocalArgument2WriteNode extends ContextualNode {
    protected final Argument arg;

    @Child protected ExpressionNode valueNode;

    public NonLocalArgument2WriteNode(final Argument arg, final int contextLevel,
        final ExpressionNode valueNode) {
      super(contextLevel);
      assert contextLevel > 0;
      this.arg = arg;
      this.valueNode = valueNode;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      Object value = valueNode.executeGeneric(frame);
      determineContext(frame).setArgument2(value);
      return value;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateWrite(arg, this, valueNode, contextLevel);
    }
  }

  public static class NonLocalArgument3WriteNode extends ContextualNode {
    protected final Argument arg;

    @Child protected ExpressionNode valueNode;

    public NonLocalArgument3WriteNode(final Argument arg, final int contextLevel,
        final ExpressionNode valueNode) {
      super(contextLevel);
      assert contextLevel > 0;
      this.arg = arg;
      this.valueNode = valueNode;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      Object value = valueNode.executeGeneric(frame);
      determineContext(frame).setArgument3(value);
      return value;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateWrite(arg, this, valueNode, contextLevel);
    }
  }

  public static class NonLocalArgument4WriteNode extends ContextualNode {
    protected final Argument arg;

    @Child protected ExpressionNode valueNode;

    public NonLocalArgument4WriteNode(final Argument arg, final int contextLevel,
        final ExpressionNode valueNode) {
      super(contextLevel);
      assert contextLevel > 0;
      this.arg = arg;
      this.valueNode = valueNode;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      Object value = valueNode.executeGeneric(frame);
      determineContext(frame).setArgument4(value);
      return value;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateWrite(arg, this, valueNode, contextLevel);
    }
  }
}
