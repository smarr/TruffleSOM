package trufflesom.vm;

public final class NotYetImplementedException extends RuntimeException {
  private static final long serialVersionUID = -1862914873966278087L;

  public NotYetImplementedException() {
    super();
  }

  public NotYetImplementedException(final String message) {
    super(message);
  }
}
