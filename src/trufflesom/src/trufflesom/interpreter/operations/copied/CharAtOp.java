package trufflesom.interpreter.operations.copied;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;

import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SSymbol;


@Proxyable
public abstract class CharAtOp extends BinaryMsgExprNode {
  @TruffleBoundary
  private static String substring(final String str, final int start, final int end) {
    return str.substring(start, end);
  }

  @Override
  public SSymbol getSelector() {
    return SymbolTable.symbolFor("charAt:");
  }

  @Specialization
  public static final String doString(final String receiver, final long idx,
      @Shared("profile") @Cached final InlinedBranchProfile profile,
      @Bind final Node self) {
    int index = (int) idx;
    if (0 < index && index <= receiver.length()) {
      return substring(receiver, index - 1, index);
    }

    profile.enter(self);
    return "Error - index out of bounds";
  }

  @Specialization
  public static final String doSSymbol(final SSymbol receiver, final long idx,
      @Shared("profile") @Cached final InlinedBranchProfile profile,
      @Bind final Node self) {
    int index = (int) idx;
    String s = receiver.getString();
    if (0 < index && index <= s.length()) {
      return substring(s, index - 1, index);
    }

    profile.enter(self);
    return "Error - index out of bounds";
  }
}
