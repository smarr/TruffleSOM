package trufflesom.interpreter;

import java.io.File;
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
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags.ExpressionTag;
import com.oracle.truffle.api.instrumentation.StandardTags.RootTag;
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;

import tools.nodestats.Tags.AnyNode;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.Universe;
import trufflesom.vm.Universe.SomExit;


@TruffleLanguage.Registration(id = "som", name = "som", version = "0.1.0",
    defaultMimeType = SomLanguage.MIME_TYPE,
    characterMimeTypes = SomLanguage.MIME_TYPE)
@ProvidedTags({RootTag.class, AnyNode.class, ExpressionTag.class, StatementTag.class})
public class SomLanguage extends TruffleLanguage<SomLanguage> {

  public static final String MIME_TYPE = "application/x-som-smalltalk";
  public static final String LANG_ID   = "som";

  @Option(help = "SOM's classpath", category = OptionCategory.USER) //
  protected static final OptionKey<String> CLASS_PATH = new OptionKey<>("");

  @Option(help = "Test Class name", category = OptionCategory.USER) //
  protected static final OptionKey<String> TEST_CLASS = new OptionKey<>("");

  @Option(help = "Test Selector", category = OptionCategory.USER) //
  protected static final OptionKey<String> TEST_SELECTOR = new OptionKey<>("");

  @CompilationFinal(dimensions = 1) private String[] args;

  private String classPath;
  private String testClass;
  private String testSelector;

  @Override
  protected SomLanguage createContext(final Env env) {
    OptionValues config = env.getOptions();
    args = env.getApplicationArguments();
    classPath = config.get(CLASS_PATH);
    testClass = config.get(TEST_CLASS);
    testSelector = config.get(TEST_SELECTOR);

    return this;
  }

  @Override
  protected void initializeContext(final SomLanguage lang) throws Exception {
    current = this;
  }

  @Override
  protected void disposeContext(final SomLanguage lang) {
    current = null;
  }

  @CompilationFinal private static SomLanguage current;

  /** This is used by the Language Server to get to an initialized instance easily. */
  public static SomLanguage getCurrent() {
    return current;
  }

  public static Source getSyntheticSource(final String text, final String name) {
    return Source.newBuilder(LANG_ID, text, name)
                 .internal(true)
                 .mimeType(MIME_TYPE)
                 .build();
  }

  public static Source getSource(final File file) throws IOException {
    return Source.newBuilder(LANG_ID, file.toURI().toURL())
                 .mimeType(MIME_TYPE)
                 .build();
  }

  private static final String START_STR = "START";
  private static final String INIT_STR  = "INIT";

  /** Marker source used to start execution with command line arguments. */
  public static final org.graalvm.polyglot.Source START =
      org.graalvm.polyglot.Source.newBuilder(LANG_ID, START_STR, START_STR).internal(true)
                                 .buildLiteral();

  public static final org.graalvm.polyglot.Source INIT =
      org.graalvm.polyglot.Source.newBuilder(LANG_ID, INIT_STR, INIT_STR).internal(true)
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
        Universe.setupClassPath(classPath);
        return Universe.interpret(testClass, testSelector);
      } else {
        try {
          return Universe.interpret(args);
        } catch (IllegalStateException e) {
          Universe.errorPrintln("Runtime Error: " + e.getMessage());
          return 1;
        } catch (SomExit e) {
          return e.errorCode;
        }
      }
    }
  }

  private static class InitializeContext extends RootNode {
    protected InitializeContext(final SomLanguage lang) {
      super(lang, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      return true;
    }
  }

  private CallTarget createStartCallTarget() {
    StartInterpretation start = new StartInterpretation();
    CallTarget ct = start.getCallTarget();

    if (ct == null) {
      ct = Truffle.getRuntime().createCallTarget(start);
    }

    assert ct != null : "Expect the StartInterpretation callTarget to be available";
    return ct;
  }

  private CallTarget createInitCallTarget() {
    InitializeContext init = new InitializeContext(this);

    CallTarget ct = init.getCallTarget();
    if (ct == null) {
      ct = Truffle.getRuntime().createCallTarget(init);
    }

    assert ct != null : "Expect the InitializeContext callTarget to be available";
    return ct;
  }

  private static boolean isStartSource(final Source source) {
    return source.isInternal() &&
        source.getName().equals(START_STR) &&
        source.getCharacters().equals(START_STR);
  }

  private static boolean isInitSource(final Source source) {
    return source.isInternal() &&
        source.getName().equals(INIT_STR) &&
        source.getCharacters().equals(INIT_STR);
  }

  @Override
  protected CallTarget parse(final ParsingRequest request) throws IOException {
    Source code = request.getSource();
    if (isStartSource(code)) {
      return createStartCallTarget();
    } else if (isInitSource(code)) {
      return createInitCallTarget();
    } else {
      // This is currently not supported.
      // The only execution mode is using the parameters to the engine and the magic
      // START source to trigger execution.
      throw new NotYetImplementedException();
    }
  }

  @Override
  protected OptionDescriptors getOptionDescriptors() {
    return new SomLanguageOptionDescriptors();
  }
}
