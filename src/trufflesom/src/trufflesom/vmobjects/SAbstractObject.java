package trufflesom.vmobjects;

import static trufflesom.vm.SymbolTable.symbolFor;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import trufflesom.interpreter.Types;


@ExportLibrary(InteropLibrary.class)
public abstract class SAbstractObject implements TruffleObject {

  public abstract SClass getSOMClass();

  @Override
  public String toString() {
    CompilerAsserts.neverPartOfCompilation();
    SClass clazz = getSOMClass();
    if (clazz == null) {
      return "an Object(clazz==null)";
    }
    return "a " + clazz.getName().getString();
  }

  private static Object send(final String selectorString, final Object[] arguments) {
    CompilerAsserts.neverPartOfCompilation("SAbstractObject.send()");
    SSymbol selector = symbolFor(selectorString);

    // Lookup the invokable
    SInvokable invokable = Types.getClassOf(arguments[0]).lookupInvokable(selector);

    return invokable.invoke(arguments);
  }

  @TruffleBoundary
  public static final Object sendUnknownGlobal(final Object receiver,
      final SSymbol globalName) {
    Object[] arguments = {receiver, globalName};
    return send("unknownGlobal:", arguments);
  }

  @TruffleBoundary
  @InliningCutoff
  public static final Object sendEscapedBlock(final Object receiver, final SBlock block) {
    Object[] arguments = {receiver, block};
    return send("escapedBlock:", arguments);
  }

  @SuppressWarnings("static-method")
  @ExportMessage
  public final boolean isNull() {
    // can't be null, because our Nil.nilObject is `null`, which is a dynamic object
    return false;
  }
}
