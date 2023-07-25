/**
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package trufflesom.compiler;

import java.io.File;
import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.Source;

import bdt.basic.ProgramDefinitionError;
import bdt.tools.structure.StructuralProbe;
import trufflesom.interpreter.SomLanguage;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


public abstract class SourcecodeCompiler {

  protected SourcecodeCompiler() {}

  public abstract Parser<?> createParser(String code, Source source,
      StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> probe);

  @TruffleBoundary
  public SClass compileClass(final String path, final String file,
      final SClass systemClass,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> probe)
      throws IOException, ProgramDefinitionError {
    String fname = path + File.separator + file + ".som";
    File f = new File(fname);
    Source source = SomLanguage.getSource(f);

    Parser<?> parser = createParser(source.getCharacters().toString(), source, probe);
    SClass result = compile(parser, systemClass);

    SSymbol cname = result.getName();
    String cnameC = cname.getString();

    if (file != cnameC) {
      throw new IllegalStateException("File name " + file
          + " does not match class name " + cnameC);
    }

    return result;
  }

  @TruffleBoundary
  public SClass compileClass(final String stmt, final SClass systemClass,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> probe)
      throws ProgramDefinitionError {
    Parser<?> parser = createParser(stmt, null, probe);
    SClass result = compile(parser, systemClass);
    return result;
  }

  public static SClass compile(final Parser<?> parser, final SClass systemClass)
      throws ProgramDefinitionError {
    ClassGenerationContext cgc =
        new ClassGenerationContext(parser.getSource(), parser.structuralProbe);

    SClass result = systemClass;
    parser.classdef(cgc);

    if (systemClass == null) {
      result = cgc.assemble();
    } else {
      cgc.assembleSystemClass(result);
    }

    return result;
  }

  public static class AstCompiler extends SourcecodeCompiler {
    @Override
    public Parser<?> createParser(final String code, final Source source,
        final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> probe) {
      return new ParserAst(code, source, probe);
    }
  }

  public static class BcCompiler extends SourcecodeCompiler {
    @Override
    public Parser<?> createParser(final String code, final Source source,
        final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> probe) {
      return new ParserBc(code, source, probe);
    }
  }
}
