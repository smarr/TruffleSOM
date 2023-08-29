package trufflesom.primitives.basics;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Object", primitive = "hashcode")
@Primitive(className = "String", primitive = "hashcode")
public abstract class HashPrim extends UnaryExpressionNode {
  @Specialization
  public static final long doString(final String receiver) {
    return receiver.hashCode();
  }

  @Specialization
  @TruffleBoundary
  public static final long doSSymbol(final SSymbol receiver) {
    return receiver.getString().hashCode();
  }

  @Specialization
  @TruffleBoundary
  public static final long doSAbstractObject(final SAbstractObject receiver) {
    return receiver.hashCode();
  }
}
