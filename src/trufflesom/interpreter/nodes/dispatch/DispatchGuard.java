package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.nodes.InvalidAssumptionException;

import trufflesom.interpreter.objectstorage.ObjectLayout;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SObject;


public abstract class DispatchGuard {
  public abstract boolean entryMatches(Object obj) throws InvalidAssumptionException;

  public static DispatchGuard create(final Object obj) {
    if (obj == Boolean.TRUE) {
      return new CheckTrue();
    }

    if (obj == Boolean.FALSE) {
      return new CheckFalse();
    }

    if (obj.getClass() == SClass.class) {
      return new CheckSClass(((SClass) obj).getObjectLayout());
    }

    if (obj.getClass() == SObject.class) {
      return new CheckSObject(((SObject) obj).getObjectLayout());
    }

    return new CheckClass(obj.getClass());
  }

  private static final class CheckClass extends DispatchGuard {

    private final Class<?> expected;

    CheckClass(final Class<?> expectedClass) {
      this.expected = expectedClass;
    }

    @Override
    public boolean entryMatches(final Object obj) throws InvalidAssumptionException {
      return obj.getClass() == expected;
    }
  }

  private static final class CheckTrue extends DispatchGuard {
    @Override
    public boolean entryMatches(final Object obj) throws InvalidAssumptionException {
      return obj == Boolean.TRUE;
    }
  }

  private static final class CheckFalse extends DispatchGuard {
    @Override
    public boolean entryMatches(final Object obj) throws InvalidAssumptionException {
      return obj == Boolean.FALSE;
    }
  }

  private static final class CheckSClass extends DispatchGuard {

    private final ObjectLayout expected;

    CheckSClass(final ObjectLayout expected) {
      this.expected = expected;
    }

    @Override
    public boolean entryMatches(final Object obj) throws InvalidAssumptionException {
      expected.checkIsLatest();
      return obj.getClass() == SClass.class &&
          ((SClass) obj).getObjectLayout() == expected;
    }
  }

  private static final class CheckSObject extends DispatchGuard {

    private final ObjectLayout expected;

    CheckSObject(final ObjectLayout expected) {
      this.expected = expected;
    }

    @Override
    public boolean entryMatches(final Object obj) throws InvalidAssumptionException {
      expected.checkIsLatest();
      return obj.getClass() == SObject.class &&
          ((SObject) obj).getObjectLayout() == expected;
    }
  }
}
