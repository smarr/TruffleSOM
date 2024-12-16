package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
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
import trufflesom.interpreter.nodes.nary.TernaryMsgExprNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


/**
 * This node implements the correct message semantics and uses sends to the
 * blocks' methods instead of inlining the code directly.
 */
@GenerateNodeFactory
@Primitive(selector = "ifTrue:ifFalse:", requiresArguments = true)
public abstract class IfTrueIfFalseMessageNode extends TernaryMsgExprNode {

  private final SInvokable trueMethod;
  private final SInvokable falseMethod;

  @Child protected DirectCallNode trueValueSend;
  @Child protected DirectCallNode falseValueSend;

  @Child private IndirectCallNode call;

  public IfTrueIfFalseMessageNode(final Object[] args) {
    if (args[1] instanceof SBlock) {
      SBlock trueBlock = (SBlock) args[1];
      trueMethod = trueBlock.getMethod();
      trueValueSend = Truffle.getRuntime().createDirectCallNode(
          trueMethod.getCallTarget());
    } else {
      trueMethod = null;
    }

    if (args[2] instanceof SBlock) {
      SBlock falseBlock = (SBlock) args[2];
      falseMethod = falseBlock.getMethod();
      falseValueSend = Truffle.getRuntime().createDirectCallNode(
          falseMethod.getCallTarget());
    } else {
      falseMethod = null;
    }

    call = Truffle.getRuntime().createIndirectCallNode();
  }

  protected final boolean hasSameArguments(final Object firstArg, final Object secondArg) {
    return (trueMethod == null || ((SBlock) firstArg).getMethod() == trueMethod)
        && (falseMethod == null || ((SBlock) secondArg).getMethod() == falseMethod);
  }

  @Override
  public final SSymbol getSelector() {
    return SymbolTable.symbolFor("ifTrue:ifFalse:");
  }

  @Specialization(guards = "hasSameArguments(trueBlock, falseBlock)")
  public final Object doIfTrueIfFalseWithInliningTwoBlocks(final boolean receiver,
      final SBlock trueBlock, final SBlock falseBlock,
      @Shared("all") @Cached final InlinedCountingConditionProfile condProf,
      @Bind final Node node) {
    if (condProf.profile(node, receiver)) {
      return trueValueSend.call(new Object[] {trueBlock});
    } else {
      return falseValueSend.call(new Object[] {falseBlock});
    }
  }

  @Specialization(replaces = {"doIfTrueIfFalseWithInliningTwoBlocks"})
  @TruffleBoundary
  public final Object doIfTrueIfFalse(final boolean receiver, final SBlock trueBlock,
      final SBlock falseBlock,
      @Shared("all") @Cached final InlinedCountingConditionProfile condProf,
      @Bind final Node node) {
    CompilerAsserts.neverPartOfCompilation("IfTrueIfFalseMessageNode.10");
    if (condProf.profile(node, receiver)) {
      return trueBlock.getMethod().invoke(call, new Object[] {trueBlock});
    } else {
      return falseBlock.getMethod().invoke(call, new Object[] {falseBlock});
    }
  }

  @Specialization(guards = "hasSameArguments(trueValue, falseBlock)")
  public final Object doIfTrueIfFalseWithInliningTrueValue(final boolean receiver,
      final Object trueValue, final SBlock falseBlock,
      @Shared("all") @Cached final InlinedCountingConditionProfile condProf,
      @Bind final Node node) {
    if (condProf.profile(node, receiver)) {
      return trueValue;
    } else {
      return falseValueSend.call(new Object[] {falseBlock});
    }
  }

  @Specialization(guards = "hasSameArguments(trueBlock, falseValue)")
  public final Object doIfTrueIfFalseWithInliningFalseValue(final boolean receiver,
      final SBlock trueBlock, final Object falseValue,
      @Shared("all") @Cached final InlinedCountingConditionProfile condProf,
      @Bind final Node node) {
    if (condProf.profile(node, receiver)) {
      return trueValueSend.call(new Object[] {trueBlock});
    } else {
      return falseValue;
    }
  }

  @Specialization(replaces = {"doIfTrueIfFalseWithInliningTrueValue"})
  @TruffleBoundary
  public final Object doIfTrueIfFalseTrueValue(final boolean receiver, final Object trueValue,
      final SBlock falseBlock,
      @Shared("all") @Cached final InlinedCountingConditionProfile condProf,
      @Bind final Node node) {
    if (condProf.profile(node, receiver)) {
      return trueValue;
    } else {
      CompilerAsserts.neverPartOfCompilation("IfTrueIfFalseMessageNode.20");
      return falseBlock.getMethod().invoke(call, new Object[] {falseBlock});
    }
  }

  @Specialization(replaces = {"doIfTrueIfFalseWithInliningFalseValue"})
  @TruffleBoundary
  public final Object doIfTrueIfFalseFalseValue(final boolean receiver, final SBlock trueBlock,
      final Object falseValue,
      @Shared("all") @Cached final InlinedCountingConditionProfile condProf,
      @Bind final Node node) {
    if (condProf.profile(node, receiver)) {
      CompilerAsserts.neverPartOfCompilation("IfTrueIfFalseMessageNode.30");
      return trueBlock.getMethod().invoke(call, new Object[] {trueBlock});
    } else {
      return falseValue;
    }
  }

  @Specialization
  public static final Object doIfTrueIfFalseTwoValues(
      final boolean receiver, final Object trueValue, final Object falseValue,
      @Shared("all") @Cached final InlinedCountingConditionProfile condProf,
      @Bind final Node node) {
    if (condProf.profile(node, receiver)) {
      return trueValue;
    } else {
      return falseValue;
    }
  }
}
