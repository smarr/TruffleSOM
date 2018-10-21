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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.api.vm.PolyglotEngine.Builder;
import com.oracle.truffle.api.vm.PolyglotEngine.Value;

import trufflesom.compiler.Disassembler;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.TruffleCompiler;
import trufflesom.primitives.Primitives;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SInvokable.SPrimitive;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public final class Universe {

  public static final boolean FailOnMissingOptimizations = false;

  public static void callerNeedsToBeOptimized(final String msg) {
    if (FailOnMissingOptimizations) {
      CompilerAsserts.neverPartOfCompilation(msg);
    }
  }

  /**
   * Associations are handles for globals with a fixed
   * SSymbol and a mutable value.
   */
  public static final class Association {
    private final SSymbol            key;
    @CompilationFinal private Object value;

    public Association(final SSymbol key, final Object value) {
      this.key = key;
      this.value = value;
    }

    public SSymbol getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }

    public void setValue(final Object value) {
      TruffleCompiler.transferToInterpreterAndInvalidate("Changed global");
      this.value = value;
    }
  }

  public static void main(final String[] arguments) {
    Value returnCode = eval(arguments);
    Object o = returnCode.get();
    if (o instanceof SObject) {
      System.exit(0);
    } else {
      System.exit(returnCode.as(Integer.class));
    }
  }

  public static Value eval(final String[] arguments) {
    Builder builder = PolyglotEngine.newBuilder();
    builder.config(SomLanguage.MIME_TYPE, SomLanguage.VM_ARGS, arguments);

    PolyglotEngine engine = builder.build();

    Value returnCode = engine.eval(SomLanguage.START);
    return returnCode;
  }

  public Object interpret(String[] arguments) {
    // Check for command line switches
    arguments = handleArguments(arguments);

    // Initialize the known universe
    return execute(arguments);
  }

  public Universe(final SomLanguage language) {
    this.language = language;
    this.globals = new HashMap<SSymbol, Association>();
    this.symbolTable = new HashMap<>();
    this.alreadyInitialized = false;

    this.blockClasses = new SClass[4];

    // Allocate the Metaclass classes
    metaclassClass = newMetaclassClass();

    // Allocate the rest of the system classes

    objectClass = newSystemClass();
    nilClass = newSystemClass();
    classClass = newSystemClass();
    arrayClass = newSystemClass();
    symbolClass = newSystemClass();
    methodClass = newSystemClass();
    integerClass = newSystemClass();
    primitiveClass = newSystemClass();
    stringClass = newSystemClass();
    doubleClass = newSystemClass();
    booleanClass = newSystemClass();

    this.primitives = new Primitives(this);
  }

  public static final class SomExit extends ThreadDeath {
    private static final long serialVersionUID = 485621638205177405L;

    public final int errorCode;

    SomExit(final int errorCode) {
      this.errorCode = errorCode;
    }
  }

  public void exit(final int errorCode) {
    TruffleCompiler.transferToInterpreter("exit");
    throw new SomExit(errorCode);
  }

  public SomLanguage getLanguage() {
    return language;
  }

  public static void errorExit(final String message) {
    TruffleCompiler.transferToInterpreter("errorExit");
    errorPrintln("Runtime Error: " + message);
    throw new SomExit(1);
  }

  @TruffleBoundary
  public String[] handleArguments(String[] arguments) {
    boolean gotClasspath = false;
    String[] remainingArgs = new String[arguments.length];
    int cnt = 0;

    for (int i = 0; i < arguments.length; i++) {
      if (arguments[i].equals("-cp")) {
        if (i + 1 >= arguments.length) {
          printUsageAndExit();
        }
        setupClassPath(arguments[i + 1]);
        // Checkstyle: stop
        ++i; // skip class path
        // Checkstyle: resume
        gotClasspath = true;
      } else if (arguments[i].equals("-d")) {
        printAST = true;
      } else {
        remainingArgs[cnt++] = arguments[i];
      }
    }

    if (!gotClasspath) {
      // Get the default class path of the appropriate size
      classPath = setupDefaultClassPath(0);
    }

    // Copy the remaining elements from the original array into the new
    // array
    arguments = new String[cnt];
    System.arraycopy(remainingArgs, 0, arguments, 0, cnt);

    // check remaining args for class paths, and strip file extension
    for (int i = 0; i < arguments.length; i++) {
      String[] split = getPathClassExt(arguments[i]);

      if (!("".equals(split[0]))) { // there was a path
        String[] tmp = new String[classPath.length + 1];
        System.arraycopy(classPath, 0, tmp, 1, classPath.length);
        tmp[0] = split[0];
        classPath = tmp;
      }
      arguments[i] = split[1];
    }

    return arguments;
  }

  @TruffleBoundary
  // take argument of the form "../foo/Test.som" and return
  // "../foo", "Test", "som"
  private String[] getPathClassExt(final String arg) {
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
  public void setupClassPath(final String cp) {
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
  private String[] setupDefaultClassPath(final int directories) {
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

  private void printUsageAndExit() {
    // Print the usage
    println("Usage: som [-options] [args...]                          ");
    println("                                                         ");
    println("where options include:                                   ");
    println("    -cp <directories separated by " + File.pathSeparator + ">");
    println("                  set search path for application classes");
    println("    -d            enable disassembling");

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
  public Object interpret(final String className, final String selector) {
    initializeObjectSystem();

    SClass clazz = loadClass(symbolFor(className));

    // Lookup the initialize invokable on the system class
    SInvokable initialize = clazz.getSOMClass(this).lookupInvokable(symbolFor(selector));
    return initialize.invoke(new Object[] {clazz});
  }

  private Object execute(final String[] arguments) {
    initializeObjectSystem();

    // Start the shell if no filename is given
    if (arguments.length == 0) {
      Shell shell = new Shell(this);
      return shell.start();
    }

    // Lookup the initialize invokable on the system class
    SInvokable initialize = systemClass.lookupInvokable(symbolFor("initialize:"));

    return initialize.invoke(new Object[] {systemObject,
        SArray.create(arguments)});
  }

  protected void initializeObjectSystem() {
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

    trueClass = newSystemClass();
    falseClass = newSystemClass();

    initializeSystemClass(trueClass, booleanClass, "True");
    initializeSystemClass(falseClass, booleanClass, "False");

    // Load methods and fields into the system classes
    loadSystemClass(objectClass);
    loadSystemClass(classClass);
    loadSystemClass(metaclassClass);
    loadSystemClass(nilClass);
    loadSystemClass(arrayClass);
    loadSystemClass(methodClass);
    loadSystemClass(symbolClass);
    loadSystemClass(integerClass);
    loadSystemClass(primitiveClass);
    loadSystemClass(stringClass);
    loadSystemClass(doubleClass);
    loadSystemClass(booleanClass);
    loadSystemClass(trueClass);
    loadSystemClass(falseClass);

    // Load the generic block class
    blockClasses[0] = loadClass(symbolFor("Block"));

    // Setup the true and false objects
    trueObject = newInstance(trueClass);
    falseObject = newInstance(falseClass);

    // Load the system class and create an instance of it
    systemClass = loadClass(symbolFor("System"));
    systemObject = newInstance(systemClass);

    // Put special objects into the dictionary of globals
    setGlobal("nil", nilObject);
    setGlobal("true", trueObject);
    setGlobal("false", falseObject);
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

  @TruffleBoundary
  public SSymbol symbolFor(final String string) {
    String interned = string.intern();
    // Lookup the symbol in the symbol table
    SSymbol result = symbolTable.get(interned);
    if (result != null) {
      return result;
    }

    return newSymbol(interned);
  }

  public static SBlock newBlock(final SMethod method, final SClass blockClass,
      final MaterializedFrame context) {
    return new SBlock(method, blockClass, context);
  }

  @TruffleBoundary
  public SClass newClass(final SClass classClass) {
    return new SClass(classClass);
  }

  @TruffleBoundary
  public static SInvokable newMethod(final SSymbol signature,
      final Invokable truffleInvokable, final boolean isPrimitive,
      final SMethod[] embeddedBlocks) {
    if (isPrimitive) {
      return new SPrimitive(signature, truffleInvokable);
    } else {
      return new SMethod(signature, truffleInvokable, embeddedBlocks);
    }
  }

  public static SObject newInstance(final SClass instanceClass) {
    return SObject.create(instanceClass);
  }

  @TruffleBoundary
  private static SClass newMetaclassClass() {
    // Allocate the metaclass classes
    SClass result = new SClass(0);
    result.setClass(new SClass(0));

    // Setup the metaclass hierarchy
    result.getSOMClass(null).setClass(result);
    return result;
  }

  private SSymbol newSymbol(final String string) {
    SSymbol result = new SSymbol(string);
    symbolTable.put(string, result);
    return result;
  }

  @TruffleBoundary
  private SClass newSystemClass() {
    // Allocate the new system class
    SClass systemClass = new SClass(0);

    // Setup the metaclass hierarchy
    systemClass.setClass(new SClass(0));
    systemClass.getSOMClass(this).setClass(metaclassClass);

    // Return the freshly allocated system class
    return systemClass;
  }

  private void initializeSystemClass(final SClass systemClass, final SClass superClass,
      final String name) {
    // Initialize the superclass hierarchy
    if (superClass != null) {
      systemClass.setSuperClass(superClass);
      systemClass.getSOMClass(this).setSuperClass(superClass.getSOMClass(this));
    } else {
      systemClass.getSOMClass(this).setSuperClass(classClass);
    }

    // Initialize the array of instance fields
    systemClass.setInstanceFields(SArray.create(new Object[0]));
    systemClass.getSOMClass(this).setInstanceFields(SArray.create(new Object[0]));

    // Initialize the array of instance invokables
    systemClass.setInstanceInvokables(SArray.create(new Object[0]));
    systemClass.getSOMClass(this).setInstanceInvokables(SArray.create(new Object[0]));

    // Initialize the name of the system class
    systemClass.setName(symbolFor(name));
    systemClass.getSOMClass(this).setName(symbolFor(name + " class"));

    // Insert the system class into the dictionary of globals
    setGlobal(systemClass.getName(), systemClass);
  }

  @TruffleBoundary
  public boolean hasGlobal(final SSymbol name) {
    return globals.containsKey(name);
  }

  @TruffleBoundary
  public Object getGlobal(final SSymbol name) {
    Association assoc = globals.get(name);
    if (assoc == null) {
      return null;
    }
    return assoc.getValue();
  }

  @TruffleBoundary
  public Association getGlobalsAssociation(final SSymbol name) {
    return globals.get(name);
  }

  public void setGlobal(final String name, final Object value) {
    setGlobal(symbolFor(name), value);
  }

  @TruffleBoundary
  public void setGlobal(final SSymbol name, final Object value) {
    Association assoc = globals.get(name);
    if (assoc == null) {
      assoc = new Association(name, value);
      globals.put(name, assoc);
    } else {
      assoc.setValue(value);
    }
  }

  public SClass getBlockClass(final int numberOfArguments) {
    SClass result = blockClasses[numberOfArguments];
    assert result != null || numberOfArguments == 0;
    return result;
  }

  private void loadBlockClass(final int numberOfArguments) {
    // Compute the name of the block class with the given number of
    // arguments
    SSymbol name = symbolFor("Block" + numberOfArguments);

    assert getGlobal(name) == null;

    // Get the block class for blocks with the given number of arguments
    SClass result = loadClass(name);

    blockClasses[numberOfArguments] = result;
  }

  @TruffleBoundary
  public SClass loadClass(final SSymbol name) {
    // Check if the requested class is already in the dictionary of globals
    SClass result = (SClass) getGlobal(name);
    if (result != null) {
      return result;
    }

    result = loadClass(name, null);
    loadPrimitives(result, false);

    setGlobal(name, result);

    return result;
  }

  private void loadPrimitives(final SClass result, final boolean isSystemClass) {
    if (result == null) {
      return;
    }

    // Load primitives if class defines them, or try to load optional
    // primitives defined for system classes.
    if (result.hasPrimitives() || isSystemClass) {
      primitives.loadPrimitives(result, !isSystemClass);
    }
  }

  @TruffleBoundary
  private void loadSystemClass(final SClass systemClass) {
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
  private SClass loadClass(final SSymbol name, final SClass systemClass) {
    // Try loading the class from all different paths
    for (String cpEntry : classPath) {
      try {
        // Load the class from a file and return the loaded class
        SClass result = som.compiler.SourcecodeCompiler.compileClass(cpEntry,
            name.getString(), systemClass, this);
        if (printAST) {
          Disassembler.dump(result.getSOMClass(this));
          Disassembler.dump(result);
        }
        return result;

      } catch (IOException e) {
        // Continue trying different paths
      }
    }

    // The class could not be found.
    return null;
  }

  @TruffleBoundary
  public SClass loadShellClass(final String stmt) throws IOException {
    // Load the class from a stream and return the loaded class
    SClass result = som.compiler.SourcecodeCompiler.compileClass(stmt, null,
        this);
    if (printAST) {
      Disassembler.dump(result);
    }
    return result;
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

  public SObject getTrueObject() {
    return trueObject;
  }

  public SObject getFalseObject() {
    return falseObject;
  }

  public SObject getSystemObject() {
    return systemObject;
  }

  public SClass getTrueClass() {
    return trueClass;
  }

  public SClass getFalseClass() {
    return falseClass;
  }

  public SClass getSystemClass() {
    return systemClass;
  }

  public Primitives getPrimitives() {
    return primitives;
  }

  public final SClass objectClass;
  public final SClass classClass;
  public final SClass metaclassClass;

  public final SClass nilClass;
  public final SClass integerClass;
  public final SClass arrayClass;
  public final SClass methodClass;
  public final SClass symbolClass;
  public final SClass primitiveClass;
  public final SClass stringClass;
  public final SClass doubleClass;

  public final SClass booleanClass;

  @CompilationFinal private SObject trueObject;
  @CompilationFinal private SObject falseObject;
  @CompilationFinal private SObject systemObject;

  @CompilationFinal private SClass trueClass;
  @CompilationFinal private SClass falseClass;
  @CompilationFinal private SClass systemClass;

  private final HashMap<SSymbol, Association> globals;

  private String[]                  classPath;
  @CompilationFinal private boolean printAST;

  private final SomLanguage language;

  private final HashMap<String, SSymbol> symbolTable;

  // Optimizations
  @CompilationFinal(dimensions = 1) private final SClass[] blockClasses;

  private final Primitives primitives;

  @CompilationFinal private boolean alreadyInitialized;

  @CompilationFinal private boolean objectSystemInitialized = false;

  public boolean isObjectSystemInitialized() {
    return objectSystemInitialized;
  }
}
