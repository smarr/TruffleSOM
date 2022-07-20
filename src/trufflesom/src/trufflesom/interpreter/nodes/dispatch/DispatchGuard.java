package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;

import trufflesom.interpreter.objectstorage.ObjectLayout;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SObject;


public abstract class DispatchGuard {
  private static final Assumption[] NO_ASSUMPTIONS = new Assumption[0];

  public abstract boolean entryMatches(Object obj) throws InvalidAssumptionException;

  public abstract boolean entryMatchesNoAssumptionCheck(Object obj);

  public abstract Assumption[] getAssumptions();

  public static DispatchGuard create(final Object obj) {
    if (obj == Boolean.TRUE) {
      return new CheckTrue();
    }

    if (obj == Boolean.FALSE) {
      return new CheckFalse();
    }

    Class<?> clazz = obj.getClass();

    if (clazz == SClass.class) {
      return new CheckSClass(((SClass) obj).getObjectLayout());
    }

    if (clazz == SObject.class) {
      return new CheckSObject(((SObject) obj).getObjectLayout());
    }

    return new CheckClass(clazz);
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

    @Override
    public boolean entryMatchesNoAssumptionCheck(final Object obj) {
      return obj.getClass() == expected;
    }

    @Override
    public Assumption[] getAssumptions() {
      return NO_ASSUMPTIONS;
    }
  }

  private static final class CheckTrue extends DispatchGuard {
    @Override
    public boolean entryMatches(final Object obj) throws InvalidAssumptionException {
      return obj == Boolean.TRUE;
    }

    @Override
    public boolean entryMatchesNoAssumptionCheck(final Object obj) {
      return obj == Boolean.TRUE;
    }

    @Override
    public Assumption[] getAssumptions() {
      return NO_ASSUMPTIONS;
    }
  }

  private static final class CheckFalse extends DispatchGuard {
    @Override
    public boolean entryMatches(final Object obj) throws InvalidAssumptionException {
      return obj == Boolean.FALSE;
    }

    @Override
    public boolean entryMatchesNoAssumptionCheck(final Object obj) {
      return obj == Boolean.FALSE;
    }

    @Override
    public Assumption[] getAssumptions() {
      return NO_ASSUMPTIONS;
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

    @Override
    public boolean entryMatchesNoAssumptionCheck(final Object obj) {
      return obj.getClass() == SClass.class &&
          ((SClass) obj).getObjectLayout() == expected;
    }

    @Override
    public Assumption[] getAssumptions() {
      return new Assumption[] {expected.getAssumption()};
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

    @Override
    public boolean entryMatchesNoAssumptionCheck(final Object obj) {
      return obj.getClass() == SObject.class &&
          ((SObject) obj).getObjectLayout() == expected;
    }

    @Override
    public Assumption[] getAssumptions() {
      return new Assumption[] {expected.getAssumption()};
    }
  }
}
