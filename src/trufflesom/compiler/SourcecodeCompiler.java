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
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;

import bd.basic.ProgramDefinitionError;
import bd.tools.structure.StructuralProbe;
import trufflesom.interpreter.SomLanguage;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


public class SourcecodeCompiler {

  private final SomLanguage language;

  public SourcecodeCompiler(final SomLanguage language) {
    this.language = language;
  }

  @TruffleBoundary
  public DynamicObject compileClass(final String path, final String file,
      final DynamicObject systemClass,
      final StructuralProbe<SSymbol, DynamicObject, SInvokable, Field, Variable> probe,
      final Universe universe)
      throws IOException, ProgramDefinitionError {
    String fname = path + File.separator + file + ".som";
    FileReader stream = new FileReader(fname);

    File f = new File(fname);
    Source source = SomLanguage.getSource(f);
    Parser parser = new Parser(stream, f.length(), source, probe, language.getUniverse());

    DynamicObject result = compile(parser, systemClass, universe);

    SSymbol cname = SClass.getName(result, universe);
    String cnameC = cname.getString();

    if (file != cnameC) {
      throw new IllegalStateException("File name " + file
          + " does not match class name " + cnameC);
    }

    return result;
  }

  @TruffleBoundary
  public DynamicObject compileClass(final String stmt, final DynamicObject systemClass,
      final StructuralProbe<SSymbol, DynamicObject, SInvokable, Field, Variable> probe,
      final Universe universe) throws ProgramDefinitionError {
    Parser parser = new Parser(new StringReader(stmt), stmt.length(), null, probe, universe);

    DynamicObject result = compile(parser, systemClass, universe);
    return result;
  }

  public DynamicObject compile(final Parser parser, final DynamicObject systemClass,
      final Universe universe) throws ProgramDefinitionError {
    ClassGenerationContext cgc = new ClassGenerationContext(universe, parser.structuralProbe);

    DynamicObject result = systemClass;
    parser.classdef(cgc);

    if (systemClass == null) {
      result = cgc.assemble();
    } else {
      cgc.assembleSystemClass(result);
    }

    return result;
  }
}
