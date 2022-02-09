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

package trufflesom.compiler;

import static trufflesom.interpreter.SNodeFactory.createCatchNonLocalReturn;
import static trufflesom.interpreter.SNodeFactory.createFieldRead;
import static trufflesom.interpreter.SNodeFactory.createFieldWrite;
import static trufflesom.interpreter.SNodeFactory.createNonLocalReturn;
import static trufflesom.vm.SymbolTable.symBlockSelf;
import static trufflesom.vm.SymbolTable.symFrameOnStack;
import static trufflesom.vm.SymbolTable.symSelf;
import static trufflesom.vm.SymbolTable.symbolFor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.source.Source;

import bd.basic.ProgramDefinitionError;
import bd.inlining.Scope;
import bd.inlining.ScopeBuilder;
import bd.inlining.nodes.Inlinable;
import bd.source.SourceCoordinate;
import bd.tools.structure.StructuralProbe;
import trufflesom.compiler.Variable.Argument;
import trufflesom.compiler.Variable.Internal;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.LexicalScope;
import trufflesom.interpreter.Method;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.LocalVariableNode.LocalVariableWriteNode;
import trufflesom.interpreter.nodes.ReturnNonLocalNode;
import trufflesom.interpreter.nodes.SequenceNode;
import trufflesom.interpreter.nodes.literals.BlockNode;
import trufflesom.interpreter.ubernodes.BenchmarkHarnessDoRuns;
import trufflesom.interpreter.ubernodes.BenchmarkInnerBenchmarkLoop;
import trufflesom.interpreter.ubernodes.BounceBenchmark.BallBounce;
import trufflesom.interpreter.ubernodes.BounceBenchmark.BallInitialize;
import trufflesom.interpreter.ubernodes.CollisionDetectorClass.CDIsInVoxel;
import trufflesom.interpreter.ubernodes.CollisionDetectorClass.CDRecurse;
import trufflesom.interpreter.ubernodes.CollisionDetectorClass.CallSignCompareTo;
import trufflesom.interpreter.ubernodes.DictIdEntry.DictIdEntryMatchKey;
import trufflesom.interpreter.ubernodes.DictIdEntry.DictIdEntryNewKeyValueNext;
import trufflesom.interpreter.ubernodes.DictionaryClass.DictAt;
import trufflesom.interpreter.ubernodes.DictionaryClass.DictContainsKey;
import trufflesom.interpreter.ubernodes.HavlakLoopFinder.DoDFSCurrent;
import trufflesom.interpreter.ubernodes.HavlakLoopFinder.IsAncestor;
import trufflesom.interpreter.ubernodes.JsonParserClass.JPEndCapture;
import trufflesom.interpreter.ubernodes.JsonParserClass.JPIsDigit;
import trufflesom.interpreter.ubernodes.JsonParserClass.JPIsWhiteSpace;
import trufflesom.interpreter.ubernodes.JsonParserClass.JPRead;
import trufflesom.interpreter.ubernodes.JsonParserClass.JPReadChar;
import trufflesom.interpreter.ubernodes.JsonParserClass.JPReadValue;
import trufflesom.interpreter.ubernodes.JsonParserClass.JPStartCapture;
import trufflesom.interpreter.ubernodes.JsonValues.JNumberNew;
import trufflesom.interpreter.ubernodes.JsonValues.JStringNew;
import trufflesom.interpreter.ubernodes.ListBenchmark.ListBenchmarkMethod;
import trufflesom.interpreter.ubernodes.ListBenchmark.ListElementLength;
import trufflesom.interpreter.ubernodes.ListBenchmark.ListElementNew;
import trufflesom.interpreter.ubernodes.ListBenchmark.ListIsShorter;
import trufflesom.interpreter.ubernodes.ListBenchmark.ListMakeList;
import trufflesom.interpreter.ubernodes.ListBenchmark.ListTail;
import trufflesom.interpreter.ubernodes.ListBenchmark.ListVerifyResult;
import trufflesom.interpreter.ubernodes.MandelbrotBenchmark.MandelbrotInnerBenchmarkLoop;
import trufflesom.interpreter.ubernodes.MandelbrotBenchmark.MandelbrotMandelbrot;
import trufflesom.interpreter.ubernodes.MandelbrotBenchmark.MandelbrotVerifyInner;
import trufflesom.interpreter.ubernodes.NBodyBenchmark.NBodyAdvanceBlock;
import trufflesom.interpreter.ubernodes.RandomClass.RandomClassInitialize;
import trufflesom.interpreter.ubernodes.RandomClass.RandomClassNext;
import trufflesom.interpreter.ubernodes.RandomClass.RandomInitialize;
import trufflesom.interpreter.ubernodes.RandomClass.RandomNext;
import trufflesom.interpreter.ubernodes.RedBlackTreeClass.IRInit;
import trufflesom.interpreter.ubernodes.RedBlackTreeClass.NodeInit;
import trufflesom.interpreter.ubernodes.RedBlackTreeClass.RBTAtPut;
import trufflesom.interpreter.ubernodes.RichardsBenchmark.RBHoldSelf;
import trufflesom.interpreter.ubernodes.RichardsBenchmark.RBOAppendHead;
import trufflesom.interpreter.ubernodes.RichardsBenchmark.RBRelease;
import trufflesom.interpreter.ubernodes.RichardsBenchmark.RBWait;
import trufflesom.interpreter.ubernodes.RichardsBenchmark.SchedulerFindTask;
import trufflesom.interpreter.ubernodes.RichardsBenchmark.TCBAddInputCheckPriority;
import trufflesom.interpreter.ubernodes.RichardsBenchmark.TSIsTask;
import trufflesom.interpreter.ubernodes.RichardsBenchmark.TSIsWaitingWithPacket;
import trufflesom.interpreter.ubernodes.RichardsBenchmark.TSPacketPending;
import trufflesom.interpreter.ubernodes.RichardsBenchmark.TSRunning;
import trufflesom.interpreter.ubernodes.SomDictionaryClass.SomDictAt;
import trufflesom.interpreter.ubernodes.SomDictionaryClass.SomDictAtPut;
import trufflesom.interpreter.ubernodes.SomDictionaryClass.SomDictBucket;
import trufflesom.interpreter.ubernodes.SomDictionaryClass.SomDictBucketIdx;
import trufflesom.interpreter.ubernodes.SomDictionaryClass.SomDictHash;
import trufflesom.interpreter.ubernodes.SomDictionaryClass.SomDictInsertBucketEntry;
import trufflesom.interpreter.ubernodes.SomIdentitySet.IsObject;
import trufflesom.interpreter.ubernodes.SomSomBenchmark.BytecodesLength;
import trufflesom.interpreter.ubernodes.SomSomBenchmark.FrameArgument;
import trufflesom.interpreter.ubernodes.SomSomBenchmark.FrameClearPrevious;
import trufflesom.interpreter.ubernodes.SomSomBenchmark.FrameContext;
import trufflesom.interpreter.ubernodes.SomSomBenchmark.FrameHasContext;
import trufflesom.interpreter.ubernodes.SomSomBenchmark.FrameLocal;
import trufflesom.interpreter.ubernodes.SomSomBenchmark.FrameLocalPut;
import trufflesom.interpreter.ubernodes.SomSomBenchmark.FramePop;
import trufflesom.interpreter.ubernodes.SomSomBenchmark.FramePush;
import trufflesom.interpreter.ubernodes.SomSomBenchmark.FrameStackElement;
import trufflesom.interpreter.ubernodes.SomSomBenchmark.SMethodBytecode;
import trufflesom.interpreter.ubernodes.SomSomBenchmark.SMethodConstant;
import trufflesom.interpreter.ubernodes.SomSomBenchmark.SMethodNumArgs;
import trufflesom.interpreter.ubernodes.SuperNewInit;
import trufflesom.interpreter.ubernodes.UnionFindNodeClass.UFNInitNode;
import trufflesom.interpreter.ubernodes.UnionFindNodeClass.UFNInitialize;
import trufflesom.interpreter.ubernodes.Vector2DClass.Vector2dCompareAnd;
import trufflesom.interpreter.ubernodes.Vector2DClass.Vector2dCompareTo;
import trufflesom.interpreter.ubernodes.Vector2DClass.Vector2dInitXY;
import trufflesom.interpreter.ubernodes.VectorClass.VectorAppend;
import trufflesom.interpreter.ubernodes.VectorClass.VectorAt;
import trufflesom.interpreter.ubernodes.VectorClass.VectorForEach;
import trufflesom.interpreter.ubernodes.VectorClass.VectorHasSome;
import trufflesom.interpreter.ubernodes.VectorClass.VectorInitialize;
import trufflesom.interpreter.ubernodes.VectorClass.VectorIsEmpty;
import trufflesom.interpreter.ubernodes.VectorClass.VectorNew2;
import trufflesom.interpreter.ubernodes.VectorClass.VectorSize;
import trufflesom.primitives.Primitives;
import trufflesom.vm.VmSettings;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


@SuppressWarnings("unchecked")
public class MethodGenerationContext
    implements ScopeBuilder<MethodGenerationContext>, Scope<LexicalScope, Method> {

  protected final ClassGenerationContext  holderGenc;
  protected final MethodGenerationContext outerGenc;
  private final boolean                   blockMethod;

  protected SSymbol signature;
  private boolean   primitive;
  private boolean   needsToCatchNonLocalReturn;

  // does directly or indirectly a non-local return
  protected boolean throwsNonLocalReturn;

  protected boolean accessesVariablesOfOuterScope;

  protected final LinkedHashMap<SSymbol, Argument> arguments;
  protected final LinkedHashMap<SSymbol, Local>    locals;

  private Internal             frameOnStack;
  protected final LexicalScope currentScope;

  private final List<SMethod> embeddedBlockMethods;

  public final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe;

  public MethodGenerationContext(final ClassGenerationContext holderGenc,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe) {
    this(holderGenc, null, false, structuralProbe);
  }

  public MethodGenerationContext(
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe) {
    this(null, null, false, structuralProbe);
  }

  public MethodGenerationContext(final ClassGenerationContext holderGenc,
      final MethodGenerationContext outerGenc) {
    this(holderGenc, outerGenc, true, outerGenc.structuralProbe);
  }

  protected MethodGenerationContext(final ClassGenerationContext holderGenc,
      final MethodGenerationContext outerGenc, final boolean isBlockMethod,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe) {
    this.holderGenc = holderGenc;
    this.outerGenc = outerGenc;
    this.blockMethod = isBlockMethod;
    this.structuralProbe = structuralProbe;

    LexicalScope outer = (outerGenc != null) ? outerGenc.getCurrentLexicalScope() : null;
    this.currentScope = new LexicalScope(new FrameDescriptor(Nil.nilObject), outer);

    accessesVariablesOfOuterScope = false;
    throwsNonLocalReturn = false;
    needsToCatchNonLocalReturn = false;
    embeddedBlockMethods = new ArrayList<SMethod>();

    arguments = new LinkedHashMap<SSymbol, Argument>();
    locals = new LinkedHashMap<SSymbol, Local>();
  }

  @Override
  public Source getSource() {
    return holderGenc.getSource();
  }

  public void markAccessingOuterScopes() {
    MethodGenerationContext context = this;
    while (context != null) {
      context.accessesVariablesOfOuterScope = true;
      context = context.outerGenc;
    }
  }

  public void addEmbeddedBlockMethod(final SMethod blockMethod) {
    embeddedBlockMethods.add(blockMethod);
    AbstractInvokable ivk = blockMethod.getInvokable();
    if (ivk instanceof Method) {
      currentScope.addEmbeddedScope(((Method) ivk).getScope());
    }
  }

  public LexicalScope getCurrentLexicalScope() {
    return currentScope;
  }

  public Internal getFrameOnStackMarker(final long coord) {
    if (outerGenc != null) {
      return outerGenc.getFrameOnStackMarker(coord);
    }

    if (frameOnStack == null) {
      assert needsToCatchNonLocalReturn;
      assert !locals.containsKey(symFrameOnStack);

      frameOnStack = new Internal(symFrameOnStack, coord);
      frameOnStack.init(
          currentScope.getFrameDescriptor().addFrameSlot(frameOnStack, FrameSlotKind.Object),
          currentScope.getFrameDescriptor());
      currentScope.addVariable(frameOnStack);
    }
    return frameOnStack;
  }

  public void makeOuterCatchNonLocalReturn() {
    throwsNonLocalReturn = true;

    MethodGenerationContext ctx = markOuterContextsToRequireContextAndGetRootContext();
    assert ctx != null;
    ctx.needsToCatchNonLocalReturn = true;
  }

  public boolean requiresContext() {
    return throwsNonLocalReturn || accessesVariablesOfOuterScope;
  }

  private MethodGenerationContext markOuterContextsToRequireContextAndGetRootContext() {
    MethodGenerationContext ctx = outerGenc;
    while (ctx.outerGenc != null) {
      ctx.throwsNonLocalReturn = true;
      ctx = ctx.outerGenc;
    }
    return ctx;
  }

  public boolean needsToCatchNonLocalReturn() {
    // only the most outer method needs to catch
    return needsToCatchNonLocalReturn && outerGenc == null;
  }

  private String getMethodIdentifier() {
    String cls = holderGenc.getName().getString();
    if (holderGenc.isClassSide()) {
      cls += "_class";
    }
    return cls + ">>" + signature.toString();
  }

  public final SInvokable assemble(final ExpressionNode body, final long coord) {
    if (primitive) {
      return Primitives.constructEmptyPrimitive(
          signature, holderGenc.getSource(), coord, structuralProbe);
    }

    return assembleMethod(body, coord);
  }

  private SMethod smethod(final AbstractInvokable invokable) {
    return new SMethod(signature, invokable, embeddedBlockMethods.toArray(new SMethod[0]));
  }

  protected SMethod assembleMethod(ExpressionNode body, final long coord) {
    String className = holderGenc.getName().getString();
    String methodName = signature.getString();
    Source source = holderGenc.getSource();

    if (VmSettings.UseAstInterp) {
      if (className.equals("BenchmarkHarness")) {
        if (methodName.equals("doRuns:")) {
          return smethod(new BenchmarkHarnessDoRuns(source, coord));
        }
      } else if (className.equals("Benchmark")) {
        if (methodName.equals("innerBenchmarkLoop:")) {
          return smethod(new BenchmarkInnerBenchmarkLoop(source, coord));
        }
      } else if (className.equals("List")) {
        if (methodName.equals("benchmark")) {
          return smethod(new ListBenchmarkMethod(source, coord));
        }

        if (methodName.equals("verifyResult:")) {
          return smethod(new ListVerifyResult(source, coord));
        }

        if (methodName.equals("makeList:")) {
          return smethod(new ListMakeList(source, coord));
        }

        if (methodName.equals("isShorter:than:")) {
          return smethod(new ListIsShorter(source, coord));
        }

        if (methodName.equals("tailWithX:withY:withZ:")) {
          return smethod(new ListTail(source, coord));
        }
      } else if (className.equals("ListElement")) {
        if (methodName.equals("length")) {
          return smethod(new ListElementLength(source, coord));
        }

        if (holderGenc.isClassSide() && methodName.equals("new:")) {
          return smethod(new ListElementNew(source, coord));
        }
      } else if (className.equals("Mandelbrot")) {
        if (methodName.equals("innerBenchmarkLoop:")) {
          return smethod(new MandelbrotInnerBenchmarkLoop(source, coord));
        }

        if (methodName.equals("verify:inner:")) {
          return smethod(new MandelbrotVerifyInner(source, coord));
        }

        if (methodName.equals("mandelbrot:")) {
          return smethod(MandelbrotMandelbrot.create(source, coord));
        }
      } else if (className.equals("Ball")) {
        if (holderGenc.isClassSide() && methodName.equals("new")) {
          return smethod(new SuperNewInit(source, coord));
        }

        if (methodName.equals("initialize")) {
          return smethod(new BallInitialize(source, coord));
        }

        if (methodName.equals("bounce")) {
          return smethod(new BallBounce(source, coord));
        }
      } else if (className.equals("Random")) {
        if (holderGenc.isClassSide()) {
          if (methodName.equals("new")) {
            return smethod(new SuperNewInit(source, coord));
          }

          if (methodName.equals("initialize")) {
            return smethod(new RandomClassInitialize(source, coord));
          }

          if (methodName.equals("next")) {
            return smethod(new RandomClassNext(source, coord));
          }
        } else {
          // instance methods
          if (methodName.equals("initialize")) {
            return smethod(new RandomInitialize(source, coord));
          }

          if (methodName.equals("next")) {
            return smethod(new RandomNext(source, coord));
          }
        }
      } else if (className.equals("Vector")) {
        if (holderGenc.isClassSide()) {
          if (methodName.equals("new:")) {
            return smethod(new VectorNew2(source, coord));
          }
        } else {
          if (methodName.equals("initialize:")) {
            return smethod(new VectorInitialize(source, coord));
          }
          if (methodName.equals("size")) {
            return smethod(new VectorSize(source, coord));
          }
          if (methodName.equals("isEmpty")) {
            return smethod(new VectorIsEmpty(source, coord));
          }
          if (methodName.equals("append:")) {
            return smethod(VectorAppend.create(source, coord));
          }
          if (methodName.equals("hasSome:")) {
            return smethod(new VectorHasSome(source, coord));
          }
          if (methodName.equals("forEach:")) {
            return smethod(new VectorForEach(source, coord));
          }
          if (methodName.equals("at:")) {
            if (body instanceof SequenceNode) {
              SequenceNode seq = (SequenceNode) body;
              ExpressionNode[] seqExp = seq.getExpressions();
              if (seqExp.length == 2 && !(seqExp[0] instanceof LocalVariableWriteNode)) {
                // this is only for the AWFY Vector
                return smethod(new VectorAt(source, coord));
              }
            }

          }
        }
      } else if (className.equals("DictIdEntry")) {
        if (holderGenc.isClassSide()) {
          if (methodName.equals("new:key:value:next:")) {
            return smethod(new DictIdEntryNewKeyValueNext(source, coord));
          }
        } else {
          if (methodName.equals("match:key:")) {
            return smethod(new DictIdEntryMatchKey(source, coord));
          }
        }
      } else if (className.equals("SomDictionary")) {
        if (methodName.equals("hash:")) {
          return smethod(new SomDictHash(source, coord));
        }
        if (methodName.equals("at:")) {
          return smethod(new SomDictAt(source, coord));
        }
        if (methodName.equals("at:put:")) {
          return smethod(new SomDictAtPut(source, coord));
        }
        if (methodName.equals("bucketIdx:")) {
          return smethod(new SomDictBucketIdx(source, coord));
        }
        if (methodName.equals("bucket:")) {
          return smethod(new SomDictBucket(source, coord));
        }
        if (methodName.equals("insertBucketEntry:value:hash:head:")) {
          return smethod(new SomDictInsertBucketEntry(source, coord));
        }
      } else if (className.equals("SomIdentitySet")) {
        if (blockMethod && methodName.contains("contains")) {
          return smethod(new IsObject(source, coord));
        }
      } else if (className.equals("HavlakLoopFinder")) {
        if (methodName.equals("isAncestor:v:")) {
          return smethod(new IsAncestor(source, coord));
        }
        if (methodName.equals("doDFS:current:")) {
          return smethod(new DoDFSCurrent(source, coord));
        }
      } else if (className.equals("Vector2D")) {
        if (methodName.equals("compare:and:")) {
          return smethod(new Vector2dCompareAnd(source, coord));
        }
        if (methodName.equals("compareTo:")) {
          return smethod(new Vector2dCompareTo(source, coord));
        }
        if (methodName.equals("initX:y:")) {
          return smethod(new Vector2dInitXY(source, coord));
        }
      } else if (className.equals("UnionFindNode")) {
        if (methodName.equals("initialize")) {
          return smethod(new UFNInitialize(source, coord));
        }
        if (methodName.equals("initNode:dfs:")) {
          return smethod(new UFNInitNode(source, coord));
        }
      } else if (className.equals("JsonParser")) {
        if (methodName.equals("read")) {
          return smethod(new JPRead(source, coord));
        }
        if (methodName.equals("readChar:")) {
          return smethod(new JPReadChar(source, coord));
        }
        if (methodName.equals("isDigit")) {
          return smethod(new JPIsDigit(source, coord));
        }
        if (methodName.equals("isWhiteSpace")) {
          return smethod(new JPIsWhiteSpace(source, coord));
        }
        if (methodName.equals("readValue")) {
          return smethod(new JPReadValue(source, coord));
        }
        if (methodName.equals("startCapture")) {
          return smethod(new JPStartCapture(source, coord));
        }
        if (methodName.equals("endCapture")) {
          return smethod(new JPEndCapture(source, coord));
        }
      } else if (className.equals("JsonString")) {
        if (holderGenc.isClassSide() && methodName.equals("new:")) {
          return smethod(new JStringNew(source, coord));
        }
      } else if (className.equals("JsonNumber")) {
        if (holderGenc.isClassSide() && methodName.equals("new:")) {
          return smethod(new JNumberNew(source, coord));
        }
      } else if (className.equals("CollisionDetector")) {
        if (methodName.equals("isInVoxel:motion:")) {
          return smethod(new CDIsInVoxel(source, coord));
        }
        if (methodName.equals("recurse:seen:voxel:motion:")) {
          return smethod(new CDRecurse(source, coord));
        }
      } else if (className.equals("RedBlackTree")) {
        if (methodName.equals("at:put:")) {
          return smethod(new RBTAtPut(source, coord));
        }
      } else if (className.equals("InsertResult")) {
        if (methodName.equals("init:node:value:")) {
          return smethod(new IRInit(source, coord));
        }
      } else if (className.equals("CallSign")) {
        if (methodName.equals("compareTo:")) {
          return smethod(new CallSignCompareTo(source, coord));
        }
      } else if (className.equals("Node")) {
        if (methodName.equals("init:value:")) {
          return smethod(new NodeInit(source, coord));
        }
      } else if (className.equals("Scheduler") || className.equals("RichardsBenchmarks")) {
        if (methodName.equals("findTask:")) {
          return smethod(new SchedulerFindTask(source, coord));
        }
        if (methodName.equals("release:")) {
          return smethod(new RBRelease(source, coord));
        }
        if (methodName.equals("wait")) {
          return smethod(new RBWait(source, coord));
        }
        if (methodName.equals("holdSelf")) {
          return smethod(new RBHoldSelf(source, coord));
        }
      } else if (className.equals("TaskState")) {
        if (!holderGenc.isClassSide()) {
          if (methodName.equals("isTaskHoldingOrWaiting")) {
            return smethod(new TSIsTask(source, coord));
          }
          if (methodName.equals("isWaitingWithPacket")) {
            return smethod(new TSIsWaitingWithPacket(source, coord));
          }
          if (methodName.equals("packetPending")) {
            return smethod(new TSPacketPending(source, coord));
          }
          if (methodName.equals("running")) {
            return smethod(new TSRunning(source, coord));
          }
        }
      } else if (className.equals("RBObject")) {
        if (methodName.equals("append:head:")) {
          return smethod(new RBOAppendHead(source, coord));
        }
      } else if (className.equals("TaskControlBlock")) {
        if (methodName.equals("addInput:checkPriority:")) {
          return smethod(new TCBAddInputCheckPriority(source, coord));
        }
      } else if (className.equals("Bytecodes")) {
        if (methodName.equals("length:")) {
          return smethod(new BytecodesLength(source, coord));
        }
      } else if (className.equals("SMethod")) {
        if (methodName.equals("bytecode:")) {
          return smethod(new SMethodBytecode(source, coord));
        }
        if (methodName.equals("constant:")) {
          return smethod(new SMethodConstant(source, coord));
        }
        if (methodName.equals("numberOfArguments")) {
          return smethod(new SMethodNumArgs(source, coord));
        }
      } else if (className.equals("Dictionary")) {
        if (!blockMethod) {
          if (methodName.equals("at:")) {
            return smethod(DictAt.create(source, coord));
          }
          if (methodName.equals("containsKey:")) {
            return smethod(DictContainsKey.create(source, coord));
          }
        }
      } else if (className.equals("Frame")) {
        if (methodName.equals("pop")) {
          return smethod(new FramePop(source, coord));
        }
        if (methodName.equals("push:")) {
          return smethod(new FramePush(source, coord));
        }
        if (methodName.equals("hasContext")) {
          return smethod(new FrameHasContext(source, coord));
        }
        if (methodName.equals("stackElement:")) {
          return smethod(new FrameStackElement(source, coord));
        }
        if (methodName.equals("clearPreviousFrame")) {
          return smethod(new FrameClearPrevious(source, coord));
        }
        if (methodName.equals("local:")) {
          return smethod(new FrameLocal(source, coord));
        }
        if (methodName.equals("local:put:")) {
          return smethod(new FrameLocalPut(source, coord));
        }
        if (methodName.equals("argument:")) {
          return smethod(new FrameArgument(source, coord));
        }
        if (methodName.equals("context:")) {
          return smethod(new FrameContext(source, coord));
        }
      } else if (className.equals("NBodySystem")) {
        if (blockMethod) {
          if (methodName.startsWith("λadvance") && body instanceof SequenceNode
              && ((SequenceNode) body).getExpressions().length == 3) {
            return smethod(new NBodyAdvanceBlock(source, coord));
          }
        }
      }
    }

    if (needsToCatchNonLocalReturn()) {
      body = createCatchNonLocalReturn(body, getFrameOnStackMarker(coord));
    }

    Method truffleMethod =
        new Method(getMethodIdentifier(), source, coord,
            body, currentScope, (ExpressionNode) body.deepCopy());

    SMethod meth = new SMethod(signature, truffleMethod,
        embeddedBlockMethods.toArray(new SMethod[0]));

    if (structuralProbe != null) {
      String id = meth.getIdentifier();
      structuralProbe.recordNewMethod(symbolFor(id), meth);
    }

    // return the method - the holder field is to be set later on!
    return meth;
  }

  @Override
  public Variable[] getVariables() {
    int numVars = arguments.size() + locals.size();
    if (frameOnStack != null) {
      numVars += 1;
    }
    Variable[] vars = new Variable[numVars];
    int i = 0;
    for (Argument a : arguments.values()) {
      vars[i] = a;
      i += 1;
    }

    for (Local l : locals.values()) {
      vars[i] = l;
      i += 1;
    }

    if (frameOnStack != null) {
      vars[i] = frameOnStack;
    }

    return vars;
  }

  public void setVarsOnMethodScope() {
    currentScope.setVariables(getVariables());
  }

  public void markAsPrimitive() {
    primitive = true;
  }

  public void setSignature(final SSymbol sig) {
    signature = sig;
  }

  private void addArgument(final SSymbol arg, final long coord) {
    if ((symSelf == arg || symBlockSelf == arg) && arguments.size() > 0) {
      throw new IllegalStateException(
          "The self argument always has to be the first argument of a method");
    }

    Argument argument = new Argument(arg, arguments.size(), coord);
    arguments.put(arg, argument);

    if (structuralProbe != null) {
      structuralProbe.recordNewVariable(argument);
    }
  }

  public void addArgumentIfAbsent(final SSymbol arg, final long coord) {
    if (arguments.containsKey(arg)) {
      return;
    }

    addArgument(arg, coord);
  }

  public boolean hasLocal(final SSymbol local) {
    return locals.containsKey(local);
  }

  public int getNumberOfLocals() {
    return locals.size();
  }

  public Local addLocal(final SSymbol local, final long coord) {
    Local l = new Local(local, coord);
    l.init(
        currentScope.getFrameDescriptor().addFrameSlot(l),
        currentScope.getFrameDescriptor());
    assert !locals.containsKey(local);
    locals.put(local, l);

    if (structuralProbe != null) {
      structuralProbe.recordNewVariable(l);
    }
    return l;
  }

  private Local addLocalAndUpdateScope(final SSymbol name, final long coord)
      throws ProgramDefinitionError {
    Local l = addLocal(name, coord);
    currentScope.addVariable(l);
    return l;
  }

  public boolean isBlockMethod() {
    return blockMethod;
  }

  public ClassGenerationContext getHolder() {
    return holderGenc;
  }

  private int getOuterSelfContextLevel() {
    int level = 0;
    MethodGenerationContext ctx = outerGenc;
    while (ctx != null) {
      ctx = ctx.outerGenc;
      level++;
    }
    return level;
  }

  public int getContextLevel(final SSymbol varName) {
    if (locals.containsKey(varName) || arguments.containsKey(varName)) {
      return 0;
    }

    if (outerGenc != null) {
      return 1 + outerGenc.getContextLevel(varName);
    }

    return 0;
  }

  public int getContextLevel(final Variable var) {
    if (locals.containsValue(var) || arguments.containsValue(var)) {
      return 0;
    }

    if (outerGenc != null) {
      return 1 + outerGenc.getContextLevel(var);
    }

    return 0;
  }

  public Local getEmbeddedLocal(final SSymbol embeddedName) {
    return locals.get(embeddedName);
  }

  protected Variable getVariable(final SSymbol varName) {
    if (locals.containsKey(varName)) {
      return locals.get(varName);
    }

    if (arguments.containsKey(varName)) {
      return arguments.get(varName);
    }

    if (outerGenc != null) {
      Variable outerVar = outerGenc.getVariable(varName);
      if (outerVar != null) {
        accessesVariablesOfOuterScope = true;
      }
      return outerVar;
    }
    return null;
  }

  public ExpressionNode getLocalReadNode(final Variable variable, final long coord) {
    return variable.getReadNode(getContextLevel(variable), coord);
  }

  public ExpressionNode getLocalWriteNode(final Variable variable,
      final ExpressionNode valExpr, final long coord) {
    return variable.getWriteNode(getContextLevel(variable), valExpr, coord);
  }

  protected Local getLocal(final SSymbol varName) {
    if (locals.containsKey(varName)) {
      return locals.get(varName);
    }

    if (outerGenc != null) {
      Local outerLocal = outerGenc.getLocal(varName);
      if (outerLocal != null) {
        accessesVariablesOfOuterScope = true;
      }
      return outerLocal;
    }
    return null;
  }

  public ReturnNonLocalNode getNonLocalReturn(final ExpressionNode expr,
      final long coord) {
    makeOuterCatchNonLocalReturn();
    return createNonLocalReturn(expr, getFrameOnStackMarker(coord),
        getOuterSelfContextLevel(), coord);
  }

  private ExpressionNode getSelfRead(final long coord) {
    return getVariable(symSelf).getReadNode(getContextLevel(symSelf), coord);
  }

  public FieldReadNode getObjectFieldRead(final SSymbol fieldName,
      final long coord) {
    if (!holderGenc.hasField(fieldName)) {
      return null;
    }
    return createFieldRead(getSelfRead(coord),
        holderGenc.getFieldIndex(fieldName), coord);
  }

  public FieldNode getObjectFieldWrite(final SSymbol fieldName, final ExpressionNode exp,
      final long coord) {
    if (!holderGenc.hasField(fieldName)) {
      return null;
    }

    return createFieldWrite(getSelfRead(coord), exp,
        holderGenc.getFieldIndex(fieldName), coord);
  }

  protected void addLocal(final Local l, final SSymbol name) {
    assert !locals.containsKey(name);
    locals.put(name, l);
    currentScope.addVariable(l);
  }

  public void mergeIntoScope(final LexicalScope scope, final SMethod toBeInlined) {
    for (Variable v : scope.getVariables()) {
      Local l = v.splitToMergeIntoOuterScope(currentScope.getFrameDescriptor());
      if (l != null) { // can happen for instance for the block self, which we omit
        SSymbol name = l.getQualifiedName(holderGenc.getSource());
        addLocal(l, name);
      }
    }

    SMethod[] embeddedBlocks = toBeInlined.getEmbeddedBlocks();
    LexicalScope[] embeddedScopes = scope.getEmbeddedScopes();

    assert ((embeddedBlocks == null || embeddedBlocks.length == 0) &&
        (embeddedScopes == null || embeddedScopes.length == 0)) ||
        embeddedBlocks.length == embeddedScopes.length;

    if (embeddedScopes != null) {
      for (LexicalScope e : embeddedScopes) {
        currentScope.addEmbeddedScope(e.split(currentScope));
      }

      for (SMethod m : embeddedBlocks) {
        embeddedBlockMethods.add(m);
      }
    }

    boolean removed = embeddedBlockMethods.remove(toBeInlined);
    assert removed;
    currentScope.removeMerged(scope);
  }

  @Override
  public bd.inlining.Variable<?> introduceTempForInlinedVersion(
      final Inlinable<MethodGenerationContext> blockOrVal, final long coord)
      throws ProgramDefinitionError {
    Local loopIdx;
    if (blockOrVal instanceof BlockNode) {
      Argument[] args = ((BlockNode) blockOrVal).getArguments();
      assert args.length == 2;
      loopIdx = getLocal(args[1].getQualifiedName(holderGenc.getSource()));
    } else {
      // if it is a literal, we still need a memory location for counting, so,
      // add a synthetic local
      loopIdx = addLocalAndUpdateScope(symbolFor(
          "!i" + SourceCoordinate.getLocationQualifier(
              holderGenc.getSource(), coord)),
          coord);
    }
    return loopIdx;
  }

  public boolean isFinished() {
    throw new UnsupportedOperationException(
        "You'll need the BytecodeMethodGenContext. "
            + "This method should only be used when creating bytecodes.");
  }

  public void markFinished() {
    throw new UnsupportedOperationException(
        "You'll need the BytecodeMethodGenContext. "
            + "This method should only be used when creating bytecodes.");
  }

  /**
   * @return number of explicit arguments,
   *         i.e., excluding the implicit 'self' argument
   */
  public int getNumberOfArguments() {
    return arguments.size();
  }

  public SSymbol getSignature() {
    return signature;
  }

  private String stripColonsAndSourceLocation(String str) {
    int startOfSource = str.indexOf('@');
    if (startOfSource > -1) {
      str = str.substring(0, startOfSource);
    }

    // replacing classic colons with triple colons to still indicate them without breaking
    // selector semantics based on colon counting
    return str.replace(":", "⫶");
  }

  public void setBlockSignature(final Source source, final long coord) {
    String outerMethodName =
        stripColonsAndSourceLocation(outerGenc.getSignature().getString());

    int numArgs = getNumberOfArguments();
    int line = SourceCoordinate.getLine(source, coord);
    int column = SourceCoordinate.getColumn(source, coord);
    String blockSig = "λ" + outerMethodName + "@" + line + "@" + column;

    for (int i = 1; i < numArgs; i++) {
      blockSig += ":";
    }

    setSignature(symbolFor(blockSig));
  }

  @Override
  public String toString() {
    String sig = signature == null ? "" : signature.toString();
    return "MethodGenC(" + holderGenc.getName().getString() + ">>" + sig + ")";
  }

  @Override
  public LexicalScope getOuterScopeOrNull() {
    return currentScope.getOuterScopeOrNull();
  }

  @Override
  public LexicalScope getScope(final Method method) {
    return currentScope.getScope(method);
  }

  @Override
  public String getName() {
    return getMethodIdentifier();
  }
}
