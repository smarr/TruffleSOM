package som.vmobjects;

import som.interpreter.Types;
import som.vm.Universe;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.MateClasses;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.object.basic.DynamicObjectBasic;


public abstract class SAbstractObject {

  public abstract DynamicObjectBasic getSOMClass();

  @Override
  public String toString() {
    CompilerAsserts.neverPartOfCompilation();
    DynamicObjectBasic clazz = getSOMClass();
    if (clazz == null) {
      return "an Object(clazz==null)";
    }
    return "a " + SClass.getName(clazz).getString();
  }

  public static final Object send(
      final String selectorString,
      final Object[] arguments,
      final DynamicObjectBasic environment,
      final ExecutionLevel exLevel) {
    CompilerAsserts.neverPartOfCompilation("SAbstractObject.send()");
    SSymbol selector = Universe.current().symbolFor(selectorString);

    // Lookup the invokable
    SInvokable invokable = SClass.lookupInvokable(Types.getClassOf(arguments[0]), selector);

    return invokable.invoke(environment, exLevel, arguments);
  }

  public static final Object sendUnknownGlobal(final Object receiver,
      final SSymbol globalName, final DynamicObjectBasic environment, final ExecutionLevel exLevel) {
    Object[] arguments = {receiver, globalName};
    return send("unknownGlobal:", arguments, environment, exLevel);
  }

  public static final Object sendEscapedBlock(final Object receiver,
      final SBlock block) {
    Object[] arguments = {receiver, block};
    /*Must fix and check what to do in this case since we have no context to do the send with the corresponding semantics and execution levels*/
    return send("escapedBlock:", arguments, MateClasses.STANDARD_ENVIRONMENT, ExecutionLevel.Base);
  }

}
