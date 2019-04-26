package trufflesom.interpreter;

import java.io.IOException;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;

import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.Universe;
import trufflesom.vm.Universe.SomExit;
import trufflesom.vmobjects.SAbstractObject;


@TruffleLanguage.Registration(id = "som", name = "som", version = "0.1.0",
    mimeType = SomLanguage.MIME_TYPE)
public class SomLanguage extends TruffleLanguage<Universe> {

  protected static final String MIME_TYPE = "application/x-som-smalltalk";
  public static final String    SOM       = "som";

  @Option(help = "SOM's classpath", category = OptionCategory.USER) //
  protected static final OptionKey<String> CLASS_PATH = new OptionKey<>("");

  @Option(help = "Test Class name", category = OptionCategory.USER) //
  protected static final OptionKey<String> TEST_CLASS = new OptionKey<>("");

  @Option(help = "Test Selector", category = OptionCategory.USER) //
  protected static final OptionKey<String> TEST_SELECTOR = new OptionKey<>("");

  @CompilationFinal private Universe universe;

  @CompilationFinal(dimensions = 1) private String[] args;

  private String classPath;
  private String testClass;
  private String testSelector;

  public Universe getUniverse() {
    return universe;
  }

  @Override
  protected Universe createContext(final Env env) {
    OptionValues config = env.getOptions();
    args = env.getApplicationArguments();
    classPath = config.get(CLASS_PATH);
    testClass = config.get(TEST_CLASS);
    testSelector = config.get(TEST_SELECTOR);

    universe = new Universe(this);
    return universe;
  }

  public static Source getSyntheticSource(final String text, final String name) {
    return Source.newBuilder(text).internal().name(name).mimeType(SomLanguage.MIME_TYPE)
                 .build();
  }

  private static final String START_STR = "START";

  /** Marker source used to start execution with command line arguments. */
  public static final org.graalvm.polyglot.Source START =
      org.graalvm.polyglot.Source.newBuilder(SOM, START_STR, START_STR).internal(true)
                                 .buildLiteral();

  private class StartInterpretation extends RootNode {

    protected StartInterpretation() {
      super(SomLanguage.this, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      if (testSelector != null && !testSelector.equals("")) {
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

  private static boolean isStartSource(final Source source) {
    return source.isInternal() &&
        source.getName().equals(START_STR) &&
        source.getCharacters().equals(START_STR);
  }

  @Override
  protected CallTarget parse(final ParsingRequest request) throws IOException {
    Source code = request.getSource();
    if (isStartSource(code)) {
      return createStartCallTarget();
    } else {
      // This is currently not supported.
      // The only execution mode is using the parameters to the engine and the magic
      // START source to trigger execution.
      throw new NotYetImplementedException();
    }
  }

  @Override
  protected boolean isObjectOfLanguage(final Object object) {
    if (object instanceof SAbstractObject) {
      return true;
    }
    throw new NotYetImplementedException();
  }

  public static Universe getCurrentContext() {
    return getCurrentContext(SomLanguage.class);
  }

  public static Universe getCurrentContext(final Node node) {
    return node.getRootNode().getLanguage(SomLanguage.class).getUniverse();
  }

  @Override
  protected OptionDescriptors getOptionDescriptors() {
    return new SomLanguageOptionDescriptors();
  }
}
