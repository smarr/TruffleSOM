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

import static trufflesom.vm.Classes.arrayClass;
import static trufflesom.vm.Classes.blockClasses;
import static trufflesom.vm.Classes.booleanClass;
import static trufflesom.vm.Classes.classClass;
import static trufflesom.vm.Classes.doubleClass;
import static trufflesom.vm.Classes.falseClass;
import static trufflesom.vm.Classes.integerClass;
import static trufflesom.vm.Classes.metaclassClass;
import static trufflesom.vm.Classes.methodClass;
import static trufflesom.vm.Classes.nilClass;
import static trufflesom.vm.Classes.objectClass;
import static trufflesom.vm.Classes.primitiveClass;
import static trufflesom.vm.Classes.stringClass;
import static trufflesom.vm.Classes.symbolClass;
import static trufflesom.vm.Classes.trueClass;
import static trufflesom.vm.Globals.getGlobal;
import static trufflesom.vm.Globals.setGlobal;
import static trufflesom.vm.SymbolTable.symNil;
import static trufflesom.vm.SymbolTable.symbolFor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Value;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.Source;

import bdt.basic.ProgramDefinitionError;
import bdt.tools.structure.StructuralProbe;
import trufflesom.compiler.Disassembler;
import trufflesom.compiler.Field;
import trufflesom.compiler.SourcecodeCompiler;
import trufflesom.compiler.Variable;
import trufflesom.interpreter.SomLanguage;
import trufflesom.primitives.Primitives;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public final class Universe {

  public static final boolean FailOnMissingOptimizations = false;

  /**
   * "self" considered to be defined by the Object class
   * we capture the source section here when parsing Object.
   */
  public static long   selfCoord;
  public static Source selfSource;

  private static String[] classPath;

  @CompilationFinal private static int printIR;

  private static StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe;

  @CompilationFinal private static boolean alreadyInitialized;

  @CompilationFinal private static boolean objectSystemInitialized = false;

  @CompilationFinal private static SObject systemObject;
  @CompilationFinal private static SClass  systemClass;

  public static void reset() {
    alreadyInitialized = false;
    objectSystemInitialized = false;
    systemClass = null;
    systemObject = null;
  }

  public static void callerNeedsToBeOptimized(final String msg) {
    if (FailOnMissingOptimizations) {
      CompilerAsserts.neverPartOfCompilation(msg);
    }
  }

  public static void main(final String[] arguments) {
    Value returnCode = eval(arguments);
    if (returnCode.isNumber()) {
      System.exit(returnCode.asInt());
    } else {
      System.exit(0);
    }
  }

  public static Builder createContextBuilder() {
    Builder builder = Context.newBuilder(SomLanguage.LANG_ID)
                             .in(System.in)
                             .out(System.out)
                             .allowAllAccess(true);
    return builder;
  }

  public static Value eval(final String[] arguments) {
    Builder builder = createContextBuilder();
    builder.arguments(SomLanguage.LANG_ID, arguments);
    builder.logHandler(System.err);

    if (!VmSettings.UseJitCompiler) {
      builder.option("engine.Compilation", "false");
    }

    Context context = builder.build();

    Value returnCode = context.eval(SomLanguage.START);
    return returnCode;
  }

  public static Object interpret(String[] arguments) {
    // Check for command line switches
    arguments = handleArguments(arguments);

    // Initialize the known universe
    return execute(arguments);
  }

  private Universe() {}

  public static final class SomExit extends ThreadDeath {
    private static final long serialVersionUID = 485621638205177405L;

    public final int errorCode;

    SomExit(final int errorCode) {
      this.errorCode = errorCode;
    }
  }

  public static void exit(final int errorCode) {
    CompilerDirectives.transferToInterpreter();
    throw new SomExit(errorCode);
  }

  public static void errorExit(final String message) {
    CompilerDirectives.transferToInterpreter();
    errorPrintln("Runtime Error: " + message);
    throw new SomExit(1);
  }

  @TruffleBoundary
  public static String[] handleArguments(final String[] arguments) {
    boolean gotClasspath = false;
    ArrayList<String> remainingArgs = new ArrayList<>();

    // read dash arguments only while we haven't seen other kind of arguments
    boolean sawOthers = false;

    for (int i = 0; i < arguments.length; i++) {
      if (arguments[i].equals("-cp") && !sawOthers) {
        if (i + 1 >= arguments.length) {
          printUsageAndExit();
        }
        setupClassPath(arguments[i + 1]);
        // Checkstyle: stop
        ++i; // skip class path
        // Checkstyle: resume
        gotClasspath = true;
      } else if (arguments[i].equals("-di") && !sawOthers) {
        printIR += 1;
      } else {
        sawOthers = true;
        remainingArgs.add(arguments[i]);
      }
    }

    if (!gotClasspath) {
      // Get the default class path of the appropriate size
      classPath = setupDefaultClassPath(0);
    }

    // check first of remaining args for class paths, and strip file extension
    if (!remainingArgs.isEmpty()) {
      String[] split = getPathClassExt(remainingArgs.get(0));

      if (!("".equals(split[0]))) { // there was a path
        String[] tmp = new String[classPath.length + 1];
        System.arraycopy(classPath, 0, tmp, 1, classPath.length);
        tmp[0] = split[0];
        classPath = tmp;
      }
      remainingArgs.set(0, split[1]);
    }

    return remainingArgs.toArray(new String[remainingArgs.size()]);
  }

  @TruffleBoundary
  // take argument of the form "../foo/Test.som" and return
  // "../foo", "Test", "som"
  private static String[] getPathClassExt(final String arg) {
    File file = new File(arg);

    String path = file.getParent();
    StringTokenizer tokenizer = new StringTokenizer(file.getName(), ".");

    if (tokenizer.countTokens() > 2) {
      errorPrintln("Class with . in its name?");
      exit(1);
    }

    String[] result = new String[3];
    result[0] = (path == null) ? "" : path;
    result[1] = tokenizer.nextToken();
    result[2] = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";

    return result;
  }

  @TruffleBoundary
  public static void setupClassPath(final String cp) {
    // Create a new tokenizer to split up the string of directories
    StringTokenizer tokenizer = new StringTokenizer(cp, File.pathSeparator);

    // Get the default class path of the appropriate size
    classPath = setupDefaultClassPath(tokenizer.countTokens());

    // Get the directories and put them into the class path array
    for (int i = 0; tokenizer.hasMoreTokens(); i++) {
      classPath[i] = tokenizer.nextToken();
    }
  }

  @TruffleBoundary
  private static String[] setupDefaultClassPath(final int directories) {
    // Get the default system class path
    String systemClassPath = System.getProperty("system.class.path");

    // Compute the number of defaults
    int defaults = (systemClassPath != null) ? 2 : 1;

    // Allocate an array with room for the directories and the defaults
    String[] result = new String[directories + defaults];

    // Insert the system class path into the defaults section
    if (systemClassPath != null) {
      result[directories] = systemClassPath;
    }

    // Insert the current directory into the defaults section
    result[directories + defaults - 1] = ".";

    // Return the class path
    return result;
  }

  private static void printUsageAndExit() {
    // Print the usage
    println("Usage: som [-options] [args...]                          ");
    println("                                                         ");
    println("where options include:                                   ");
    println("    -cp <directories separated by " + File.pathSeparator + ">");
    println("                  set search path for application classes");
    println("    -di           enable disassembling, dumping the IR");

    // Exit
    System.exit(0);
  }

  /**
   * Start interpretation by sending the selector to the given class. This is
   * mostly meant for testing currently.
   *
   * @param className
   * @param selector
   * @return
   */
  public static Object interpret(final String className, final String selector) {
    initializeObjectSystem();

    SClass clazz = loadClass(symbolFor(className));

    // Lookup the initialize invokable on the system class
    SInvokable initialize = clazz.getSOMClass().lookupInvokable(symbolFor(selector));
    return initialize.invoke(new Object[] {clazz});
  }

  private static Object execute(final String[] arguments) {
    initializeObjectSystem();

    // Start the shell if no filename is given
    if (arguments.length == 0) {
      return Shell.start();
    }

    Object[] arrStorage = Arrays.copyOfRange(arguments, 0, arguments.length, Object[].class);

    // Lookup the initialize invokable on the system class
    SInvokable initialize = systemClass.lookupInvokable(symbolFor("initialize:"));

    return initialize.invoke(new Object[] {systemObject,
        SArray.create(arrStorage)});
  }

  @TruffleBoundary
  public static void initializeObjectSystem() {
    CompilerAsserts.neverPartOfCompilation();
    if (alreadyInitialized) {
      return;
    } else {
      alreadyInitialized = true;
    }

    // Allocate the nil object
    SObject nilObject = Nil.nilObject;

    // Setup the class reference for the nil object
    nilObject.setClass(nilClass);

    // Initialize the system classes.
    initializeSystemClass(objectClass, null, "Object");
    initializeSystemClass(classClass, objectClass, "Class");
    initializeSystemClass(metaclassClass, classClass, "Metaclass");
    initializeSystemClass(nilClass, objectClass, "Nil");
    initializeSystemClass(arrayClass, objectClass, "Array");
    initializeSystemClass(methodClass, objectClass, "Method");
    initializeSystemClass(stringClass, objectClass, "String");
    initializeSystemClass(symbolClass, stringClass, "Symbol");
    initializeSystemClass(integerClass, objectClass, "Integer");
    initializeSystemClass(primitiveClass, objectClass, "Primitive");
    initializeSystemClass(doubleClass, objectClass, "Double");
    initializeSystemClass(booleanClass, objectClass, "Boolean");

    initializeSystemClass(trueClass, booleanClass, "True");
    initializeSystemClass(falseClass, booleanClass, "False");

    // Load methods and fields into the system classes
    loadSystemClass(objectClass);
    loadSystemClass(classClass);
    loadSystemClass(metaclassClass);
    loadSystemClass(nilClass);
    loadSystemClass(arrayClass);
    loadSystemClass(methodClass);
    loadSystemClass(stringClass);
    loadSystemClass(symbolClass);
    loadSystemClass(integerClass);
    loadSystemClass(primitiveClass);
    loadSystemClass(doubleClass);
    loadSystemClass(booleanClass);
    loadSystemClass(trueClass);
    loadSystemClass(falseClass);

    // Load the generic block class
    blockClasses[0] = loadClass(symbolFor("Block"));

    // Load the system class and create an instance of it
    systemClass = loadClass(symbolFor("System"));
    systemObject = new SObject(systemClass);

    // Put special objects into the dictionary of globals
    setGlobal("nil", nilObject);
    setGlobal("true", true);
    setGlobal("false", false);
    setGlobal("system", systemObject);

    // Load the remaining block classes
    loadBlockClass(1);
    loadBlockClass(2);
    loadBlockClass(3);

    if (null == blockClasses[1]) {
      errorExit("Initialization went wrong for class Blocks");
    }
    objectSystemInitialized = true;
  }

  private static void initializeSystemClass(final SClass systemClass, final SClass superClass,
      final String name) {
    // Initialize the superclass hierarchy
    if (superClass != null) {
      systemClass.setSuperClass(superClass);
      systemClass.getSOMClass().setSuperClass(superClass.getSOMClass());
    } else {
      systemClass.getSOMClass().setSuperClass(classClass);
    }

    // Initialize the array of instance fields
    systemClass.setInstanceFields(Arrays.asList());
    systemClass.getSOMClass().setInstanceFields(Arrays.asList());

    // Initialize the name of the system class
    systemClass.setName(symbolFor(name));
    systemClass.getSOMClass().setName(symbolFor(name + " class"));

    // Insert the system class into the dictionary of globals
    setGlobal(systemClass.getName(), systemClass);
  }

  private static void loadBlockClass(final int numberOfArguments) {
    // Compute the name of the block class with the given number of
    // arguments
    SSymbol name = symbolFor("Block" + numberOfArguments);

    assert getGlobal(name) == null;

    // Get the block class for blocks with the given number of arguments
    SClass result = loadClass(name);

    blockClasses[numberOfArguments] = result;
  }

  @TruffleBoundary
  public static SClass loadShellClass(final String stmt) throws IOException {
    try {
      // Load the class from a stream and return the loaded class
      SClass result = SourcecodeCompiler.compileClass(stmt, null, null);
      if (printIR > 0) {
        Disassembler.dump(result);
      }
      return result;
    } catch (ProgramDefinitionError e) {
      return null;
    }
  }

  @TruffleBoundary
  public static SClass loadClass(final SSymbol name) {
    // Check if the requested class is already in the dictionary of globals
    if (name == symNil) {
      return null;
    }

    SClass result = (SClass) getGlobal(name);
    if (result != null) {
      return result;
    }

    result = Universe.loadClass(name, null);
    loadPrimitives(result, false);

    setGlobal(name, result);

    return result;
  }

  public static void loadPrimitives(final SClass result, final boolean isSystemClass) {
    if (result == null) {
      return;
    }

    // Load primitives if class defines them, or try to load optional
    // primitives defined for system classes.
    if (result.hasPrimitives() || isSystemClass) {
      Primitives.Current.loadPrimitives(result, !isSystemClass, null);
    }
  }

  @TruffleBoundary
  public static void loadSystemClass(final SClass systemClass) {
    // Load the system class
    SClass result = loadClass(systemClass.getName(), systemClass);

    if (result == null) {
      throw new IllegalStateException(systemClass.getName().getString()
          + " class could not be loaded. "
          + "It is likely that the class path has not been initialized properly. "
          + "Please set system property 'system.class.path' or "
          + "pass the '-cp' command-line parameter.");
    }

    loadPrimitives(result, true);
  }

  @TruffleBoundary
  private static SClass loadClass(final SSymbol name, final SClass systemClass) {
    // Skip if classPath is not set
    if (classPath == null) {
      return null;
    }

    // Try loading the class from all different paths
    for (String cpEntry : classPath) {
      try {
        // Load the class from a file and return the loaded class
        SClass result = SourcecodeCompiler.compileClass(
            cpEntry, name.getString(), systemClass, structuralProbe);
        if (printIR > 0) {
          Disassembler.dump(result.getSOMClass());
          Disassembler.dump(result);
        }
        return result;

      } catch (IOException e) {
        // Continue trying different paths
      } catch (ProgramDefinitionError e) {
        Universe.errorExit(e.toString());
      }
    }

    // The class could not be found.
    return null;
  }

  @TruffleBoundary
  public static void errorPrint(final String msg) {
    // Checkstyle: stop
    System.err.print(msg);
    // Checkstyle: resume
  }

  @TruffleBoundary
  public static void errorPrintln(final String msg) {
    // Checkstyle: stop
    System.err.println(msg);
    // Checkstyle: resume
  }

  @TruffleBoundary
  public static void errorPrintln() {
    // Checkstyle: stop
    System.err.println();
    // Checkstyle: resume
  }

  @TruffleBoundary
  public static void print(final String msg) {
    // Checkstyle: stop
    System.out.print(msg);
    // Checkstyle: resume
  }

  @TruffleBoundary
  public static void println(final String msg) {
    // Checkstyle: stop
    System.out.println(msg);
    // Checkstyle: resume
  }

  @TruffleBoundary
  public static void println() {
    // Checkstyle: stop
    System.out.println();
    // Checkstyle: resume
  }

  public static void setStructuralProbe(
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> probe) {
    structuralProbe = probe;
  }
}
