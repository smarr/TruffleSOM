package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedCountingConditionProfile;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


public abstract class IfMessageNode extends BinaryMsgExprNode {

  protected static final int LIMIT = 3;

  @GenerateNodeFactory
  @Primitive(selector = "ifTrue:")
  public abstract static class IfTrueMessageNode extends IfMessageNode {
    public IfTrueMessageNode() {
      super(true);
    }

    @Override
    public SSymbol getSelector() {
      return SymbolTable.symbolFor("ifTrue:");
    }
  }

  @GenerateNodeFactory
  @Primitive(selector = "ifFalse:")
  public abstract static class IfFalseMessageNode extends IfMessageNode {
    public IfFalseMessageNode() {
      super(false);
    }

    @Override
    public SSymbol getSelector() {
      return SymbolTable.symbolFor("ifFalse:");
    }
  }

  private final boolean expected;

  protected IfMessageNode(final boolean expected) {
    this.expected = expected;
  }

  @Override
  public SSymbol getSelector() {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("truffle-static-method")
  @Specialization(guards = {"arg.getMethod() == method"}, limit = "LIMIT")
  public final Object cachedBlock(final boolean rcvr, final SBlock arg,
      @SuppressWarnings("unused") @Cached("arg.getMethod()") final SInvokable method,
      @Cached("create(method.getCallTarget())") final DirectCallNode callTarget,
      @Shared("all") @Cached final InlinedCountingConditionProfile condProf,
      @Bind("this") final Node node) {
    if (condProf.profile(node, rcvr == expected)) {
      return callTarget.call(new Object[] {arg});
    } else {
      return Nil.nilObject;
    }
  }

  @Specialization(replaces = "cachedBlock")
  public final Object fallback(final boolean rcvr, final SBlock arg,
      @Cached final IndirectCallNode callNode,
      @Shared("all") @Cached final InlinedCountingConditionProfile condProf,
      @Bind("this") final Node node) {
    if (condProf.profile(node, rcvr == expected)) {
      return callNode.call(arg.getMethod().getCallTarget(), new Object[] {arg});
    } else {
      return Nil.nilObject;
    }
  }

  protected static final boolean notABlock(final Object arg) {
    return !(arg instanceof SBlock);
  }

  @Specialization(guards = {"notABlock(arg)"})
  public final Object literal(final boolean rcvr, final Object arg,
      @Shared("all") @Cached final InlinedCountingConditionProfile condProf,
      @Bind("this") final Node node) {
    if (condProf.profile(node, rcvr == expected)) {
      return arg;
    } else {
      return Nil.nilObject;
    }
  }
}
