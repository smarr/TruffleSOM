/**
 * Copyright (c) 2015 Guido Chari, gchari@dc.uba.ar
 * LaFHIS lab, Universidad de Buenos Aires, Buenos Aires, Argentina
 * http://www.lafhis.dc.uba.ar
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

package som.vmobjects;

import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;


public class SReflectiveObject extends SObject {

  @CompilationFinal protected SObject environment;
  
  protected SReflectiveObject(final SClass instanceClass) {
    super(instanceClass);
  }

  protected SReflectiveObject(final int numFields) {
    super(numFields);
  }
  
  public static SReflectiveObject create(final SClass instanceClass) {
    return new SReflectiveObject(instanceClass);
  }

  public static SReflectiveObject create(final int numFields) {
    return new SReflectiveObject(numFields);
  }


  
  public final SObject getEnvironment() {
    return environment;
  }

  public final void setEnvironment(final SObject value) {
    transferToInterpreterAndInvalidate("SReflectiveObject.setEnvironment");
    assert value != null;
    environment = value;
  }
}
