package trufflesom.tests;

import java.lang.reflect.Field;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.impl.AbstractPolyglotImpl.AbstractValueImpl;

import com.oracle.truffle.api.object.DynamicObject;

import trufflesom.interpreter.SomLanguage;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SObject;


public class TruffleAccessor {

  public static boolean isDynamicObject(final Value actualResult)
      throws ReflectiveOperationException {
    Object o = readValue(actualResult);
    return o instanceof DynamicObject;
  }

  public static String getSomClassName(final Value actualResult) throws NoSuchMethodException,
      IllegalAccessException, NoSuchFieldException {
    DynamicObject o = toObject(actualResult);

    Universe universe = getContext(actualResult);

    String actual = SClass.getName(o, universe).getString();
    return actual;
  }

  public static String getSomObjectClassName(final Value obj)
      throws ReflectiveOperationException {
    DynamicObject o = toObject(obj);
    Universe universe = getContext(obj);
    String actual = SClass.getName(SObject.getSOMClass(o), universe).getString();
    return actual;
  }

  public static Object readValue(final Value actualResult)
      throws NoSuchFieldException, SecurityException, IllegalArgumentException,
      IllegalAccessException {
    Field f = actualResult.getClass().getDeclaredField("receiver");
    f.setAccessible(true);
    return f.get(actualResult);
  }

  private static DynamicObject toObject(final Value actualResult) throws NoSuchMethodException,
      IllegalAccessException, NoSuchFieldException, SecurityException {
    return (DynamicObject) readValue(actualResult);
  }

  private static Universe getContext(final Value actualResult)
      throws NoSuchFieldException, IllegalAccessException {
    Field implField = actualResult.getClass().getDeclaredField("impl");
    implField.setAccessible(true);
    AbstractValueImpl valImpl = (AbstractValueImpl) implField.get(actualResult);

    Field langCtxField =
        valImpl.getClass().getSuperclass().getSuperclass().getDeclaredField("languageContext");
    langCtxField.setAccessible(true);

    Object languageContext = langCtxField.get(valImpl);

    Field lazyField = languageContext.getClass().getDeclaredField("lazy");
    lazyField.setAccessible(true);

    Object lazy = lazyField.get(languageContext);

    Field languageInstanceField = lazy.getClass().getDeclaredField("languageInstance");
    languageInstanceField.setAccessible(true);

    Object languageInstance = languageInstanceField.get(lazy);
    Field spiField = languageInstance.getClass().getDeclaredField("spi");
    spiField.setAccessible(true);

    SomLanguage lang = (SomLanguage) spiField.get(languageInstance);
    return lang.getUniverse();
  }
}
