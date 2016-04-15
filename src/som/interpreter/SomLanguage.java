package som.interpreter;

import java.io.IOException;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;

import som.vm.NotYetImplementedException;


@TruffleLanguage.Registration(name = "SOM", version = "0.1.0", mimeType = SomLanguage.MIME_TYPE)
public class SomLanguage extends TruffleLanguage<Object> {

  public static final String MIME_TYPE = "application/x-newspeak-som";

  public static final SomLanguage INSTANCE = new SomLanguage();

  @Override
  protected Object createContext(final Env env) {
    throw new NotYetImplementedException();
  }

  @Override
  protected CallTarget parse(final Source code, final Node context,
      final String... argumentNames) throws IOException {
    throw new NotYetImplementedException();
  }

  @Override
  protected Object findExportedSymbol(final Object context,
      final String globalName, final boolean onlyExplicit) {
    throw new NotYetImplementedException();
  }

  @Override
  protected Object getLanguageGlobal(final Object context) {
    throw new NotYetImplementedException();
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
}
