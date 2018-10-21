/**
 * Copyright (c) 2013 Stefan Marr,   stefan.marr@vub.ac.be
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

package trufflesom.vm;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SObject;


public class Shell {

  private final Universe universe;

  public Shell(final Universe universe) {
    this.universe = universe;
  }

  public Object start() {
    BufferedReader in;
    String stmt;
    int counter;
    SClass myClass;
    SObject myObject;
    Object it;

    counter = 0;
    in = new BufferedReader(new InputStreamReader(System.in));
    it = Nil.nilObject;

    Universe.println("SOM Shell. Type \"quit\" to exit.\n");

    while (true) {
      try {
        Universe.print("---> ");

        // Read a statement from the keyboard
        stmt = in.readLine();
        if (stmt.equals("quit")) {
          return it;
        }

        // Generate a temporary class with a run method
        stmt = "Shell_Class_" + counter++ + " = ( run: it = ( | tmp | tmp := ("
            + stmt + " ). 'it = ' print. ^tmp println ) )";

        // Compile and load the newly generated class
        myClass = universe.loadShellClass(stmt);

        // If success
        if (myClass != null) {
          // Create and push a new instance of our class on the stack
          myObject = Universe.newInstance(myClass);

          // Lookup the run: method
          SInvokable shellMethod = myClass.lookupInvokable(universe.symbolFor("run:"));

          // Invoke the run method
          it = shellMethod.invoke(new Object[] {myObject, it});
        }
      } catch (Exception e) {
        Universe.errorPrintln("Caught exception: " + e.getMessage());
      }
    }
  }
}
