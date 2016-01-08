package som.interpreter.nodes.dispatch;

import som.vmobjects.SBlock;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SObject;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.object.Shape;


public abstract class DispatchGuard {
  public abstract boolean entryMatches(Object obj) throws InvalidAssumptionException;

  public static DispatchGuard create(final Object obj) {
    if (obj == Boolean.TRUE) {
      return new CheckTrue();
    }

    if (obj == Boolean.FALSE) {
      return new CheckFalse();
    }

    if (obj instanceof SClass) {
      return new CheckSClass(((SClass) obj).getObjectLayout());
    }

    if (obj instanceof SObject) {
      return new CheckSObject(((SObject) obj).getObjectLayout());
    }

    return new CheckClass(obj.getClass());
  }

  public static DispatchGuard createForBlock(final SBlock block) {
    return new BlockMethod(block.getMethod());
  }

  private static final class BlockMethod extends DispatchGuard {
    private final SInvokable expected;

    BlockMethod(final SInvokable method) {
      this.expected = method;
    }

    @Override
    public boolean entryMatches(final Object obj) throws InvalidAssumptionException {
      return ((SBlock) obj).getMethod() == expected;
    }
  }

  private static final class CheckClass extends DispatchGuard {

    private final Class<?> expected;

    public CheckClass(final Class<?> expectedClass) {
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

  private static class CheckSClass extends DispatchGuard {
    protected final Shape expected;
    protected final Assumption valid;

    public CheckSClass(final Shape expected) {
      this.expected = expected;
      valid = expected.getValidAssumption();
    }

    @Override
    public boolean entryMatches(final Object obj) throws InvalidAssumptionException {
      valid.check();
      return obj instanceof SClass &&
          expected.check(((SClass) obj).getDynamicObject());
    }
  }

  private static final class CheckSObject extends CheckSClass {
    public CheckSObject(Shape expected) {
      super(expected);
    }

    @Override
    public boolean entryMatches(final Object obj) throws InvalidAssumptionException {
      valid.check();
      return obj instanceof SObject &&
          expected.check(((SObject) obj).getDynamicObject());
    }
  }
}