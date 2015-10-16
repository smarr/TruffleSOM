package som.compiler;

import java.io.Reader;
import som.vm.Universe;
import com.oracle.truffle.api.source.Source;


public class MateParser extends Parser {

  public MateParser(Reader reader, long fileSize, Source source,
      Universe universe) {
    super(reader, fileSize, source, universe);
  }
}
