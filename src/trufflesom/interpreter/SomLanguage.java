package som.interpreter;

import java.io.IOException;
import java.util.Map;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;

import som.vm.NotYetImplementedException;
import som.vm.Universe;
import som.vm.Universe.SomExit;


@TruffleLanguage.Registration(name = "SOM", version = "0.1.0",
    mimeType = SomLanguage.MIME_TYPE)
public class SomLanguage extends TruffleLanguage<Universe> {

  public static final String MIME_TYPE = "application/x-som-smalltalk";
  public static final String VM_ARGS   = "vm-arguments";

  public static final String CLASS_PATH    = "class-path";
  public static final String TEST_CLASS    = "test-class";
  public static final String TEST_SELECTOR = "test-selector";

  @CompilationFinal private Universe                 universe;
  @CompilationFinal(dimensions = 1) private String[] args;

  private String classPath;
  private String testClass;
  private String testSelector;

  public Universe getUniverse() {
    return universe;
  }

  @Override
  protected Universe createContext(final Env env) {
    Map<String, Object> config = env.getConfig();
    args = (String[]) config.get(VM_ARGS);
    classPath = (String) config.get(CLASS_PATH);
    testClass = (String) config.get(TEST_CLASS);
    testSelector = (String) config.get(TEST_SELECTOR);

    universe = new Universe(this);
    return universe;
  }

  public static Source getSyntheticSource(final String text, final String name) {
    return Source.newBuilder(text).internal().name(name).mimeType(SomLanguage.MIME_TYPE)
                 .build();
  }

  /** Marker source used to start execution with command line arguments. */
  public static final Source START = getSyntheticSource("", "START");

  private class StartInterpretation extends RootNode {

    protected StartInterpretation() {
      super(SomLanguage.this, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      if (testSelector != null) {
        assert classPath != null;
        assert testClass != null;
        universe.setupClassPath(classPath);
        return universe.interpret(testClass, testSelector);
      } else {
        try {
          return universe.interpret(args);
        } catch (IllegalStateException e) {
          Universe.errorPrintln("Runtime Error: " + e.getMessage());
          return 1;
        } catch (SomExit e) {
          return e.errorCode;
        }
      }
    }
  }

  private CallTarget createStartCallTarget() {
    return Truffle.getRuntime().createCallTarget(new StartInterpretation());
  }

  @Override
  protected CallTarget parse(final ParsingRequest request) throws IOException {
    Source code = request.getSource();
    assert code == START || (code.getLength() == 0 && code.getName().equals("START"));
    return createStartCallTarget();
  }

  @Override
  protected Object findExportedSymbol(final Universe context,
      final String globalName, final boolean onlyExplicit) {
    throw new NotYetImplementedException();
  }

  @Override
  protected Object getLanguageGlobal(final Universe context) {
    return null;
  }

  @Override
  protected boolean isObjectOfLanguage(final Object object) {
    throw new NotYetImplementedException();
  }

  @Override
  protected Object evalInContext(final Source source, final Node node,
      final MaterializedFrame mFrame) throws IOException {
    throw new NotYetImplementedException();
  }

  public static Universe getCurrentContext() {
    return getCurrentContext(SomLanguage.class);
  }
}
