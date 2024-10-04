/*
 * Copyright (c) 2013 Stefan Marr, stefan.marr@vub.ac.be
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
package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.Source;

import trufflesom.bdt.inlining.ScopeAdaptationVisitor;
import trufflesom.bdt.primitives.nodes.PreevaluatedExpression;
import trufflesom.compiler.Variable.Argument;
import trufflesom.compiler.bc.BytecodeGenerator;
import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgumentReadNode;
import trufflesom.interpreter.nodes.FieldNodeFactory.FieldWriteNodeGen;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.CachedFieldRead;
import trufflesom.interpreter.nodes.dispatch.CachedFieldWriteAndSelf;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.IncrementLongFieldNode;
import trufflesom.interpreter.objectstorage.ObjectLayout;
import trufflesom.interpreter.objectstorage.StorageLocation;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vmobjects.SObject;


public abstract class FieldNode extends ExpressionNode {

  public abstract ExpressionNode getSelf();

  public static final class FieldReadNode extends FieldNode
      implements PreevaluatedExpression {
    @Child private ExpressionNode        self;
    @Child private AbstractReadFieldNode read;

    public FieldReadNode(final ExpressionNode self, final int fieldIndex) {
      this.self = self;
      read = FieldAccessorNode.createRead(fieldIndex);
    }

    public int getFieldIndex() {
      return read.getFieldIndex();
    }

    @Override
    public ExpressionNode getSelf() {
      return self;
    }

    public Object executeEvaluated(final SObject obj) {
      return read.read(obj);
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return executeEvaluated((SObject) arguments[0]);
    }

    @Override
    public long executeLong(final VirtualFrame frame) throws UnexpectedResultException {
      SObject obj = (SObject) self.executeGeneric(frame);
      return read.readLong(obj);
    }

    @Override
    public double executeDouble(final VirtualFrame frame) throws UnexpectedResultException {
      SObject obj = (SObject) self.executeGeneric(frame);
      return read.readDouble(obj);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      SObject obj = (SObject) self.executeGeneric(frame);
      return executeEvaluated(obj);
    }

    @Override
    public boolean isTrivial() {
      return true;
    }

    @Override
    public boolean isTrivialInBlock() {
      if (self instanceof NonLocalArgumentReadNode arg) {
        // it works if we are just 1 level in, but at 2 levels, we get a block
        // object, and this is currently not handled by our trivial method logic
        return arg.contextLevel < 2;
      }
      return true;
    }

    @Override
    public PreevaluatedExpression copyTrivialNode() {
      FieldReadNode node = (FieldReadNode) copy();
      node.self = null;
      node.read = (AbstractReadFieldNode) node.read.deepCopy();
      return node;
    }

    @Override
    public PreevaluatedExpression copyTrivialNodeInBlock() {
      if (self instanceof NonLocalArgumentReadNode arg) {
        // it works if we are just 1 level in, but at 2 levels, we get a block
        // object, and this is currently not handled by our trivial method logic
        if (arg.contextLevel < 2) {
          return copyTrivialNode();
        }
        return null;
      }
      return copyTrivialNode();
    }

    @Override
    public AbstractDispatchNode asDispatchNode(final Object rcvr, final Source source,
        final AbstractDispatchNode next) {
      ObjectLayout layout = ((SObject) rcvr).getObjectLayout();
      StorageLocation storage = layout.getStorageLocation(read.getFieldIndex());
      return new CachedFieldRead(rcvr.getClass(), layout, source, storage, next);
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      Object scope = inliner.getCurrentScope();

      if (scope instanceof BytecodeMethodGenContext) {
        BytecodeMethodGenContext mgenc = (BytecodeMethodGenContext) scope;
        BytecodeGenerator.emitPUSHFIELD(mgenc, (byte) read.getFieldIndex(), (byte) 0);
      }
    }
  }

  @NodeChild(value = "self", type = ExpressionNode.class)
  @NodeChild(value = "value", type = ExpressionNode.class)
  public abstract static class FieldWriteNode extends FieldNode
      implements PreevaluatedExpression {
    @Child private AbstractWriteFieldNode write;

    public FieldWriteNode(final int fieldIndex) {
      write = FieldAccessorNode.createWrite(fieldIndex);
    }

    public int getFieldIndex() {
      return write.getFieldIndex();
    }

    public abstract ExpressionNode getValue();

    @Override
    public boolean isTrivial() {
      ExpressionNode val = getValue();
      // can't be a NonLocalArgumentReadNode, then it wouldn't be a setter
      // can't be a super access either. So that's why we have the == compare here
      return val.getClass() == LocalArgumentReadNode.class;
    }

    @Override
    public boolean isTrivialInSequence() {
      return isTrivial();
    }

    @Override
    public PreevaluatedExpression copyTrivialNode() {
      if (isTrivial()) {
        return new WriteAndReturnSelf(
            FieldWriteNodeGen.create(write.getFieldIndex(), null, null));
      }
      return null;
    }

    @Override
    public PreevaluatedExpression copyTrivialNodeInSequence() {
      return copyTrivialNode();
    }

    @Override
    public AbstractDispatchNode asDispatchNode(final Object rcvr, final Source source,
        final AbstractDispatchNode next) {
      if (!isTrivial()) {
        return null;
      }
      ObjectLayout layout = ((SObject) rcvr).getObjectLayout();
      StorageLocation storage = layout.getStorageLocation(write.getFieldIndex());
      return new CachedFieldWriteAndSelf(rcvr.getClass(), layout, source, storage, next);
    }

    public final Object executeEvaluated(@SuppressWarnings("unused") final VirtualFrame frame,
        final SObject self, final Object value) {
      return write.write(self, value);
    }

    @Override
    public final Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return executeEvaluated(frame, (SObject) arguments[0], arguments[1]);
    }

    @Specialization
    public long doLong(final SObject self, final long value) {
      return write.write(self, value);
    }

    @Specialization
    public double doDouble(final SObject self, final double value) {
      return write.write(self, value);
    }

    @Specialization
    public Object doObject(final VirtualFrame frame, final SObject self,
        final Object value) {
      return executeEvaluated(frame, self, value);
    }

    public static ExpressionNode createForMethod(final int fieldIdx, final Argument self,
        final Argument val) {
      FieldWriteNode node = FieldWriteNodeGen.create(
          fieldIdx,
          new LocalArgumentReadNode(self),
          new LocalArgumentReadNode(val));
      return new WriteAndReturnSelf(node);
    }
  }

  public static final class WriteAndReturnSelf extends ExpressionNode
      implements PreevaluatedExpression {
    @Child ExpressionNode write;

    WriteAndReturnSelf(final FieldWriteNode write) {
      this.write = write;
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame, final Object[] args) {
      write.doPreEvaluated(frame, args);
      return args[0];
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      return doPreEvaluated(frame, frame.getArguments());
    }

    @Override
    public boolean isTrivial() {
      return true;
    }

    @Override
    public PreevaluatedExpression copyTrivialNode() {
      return (PreevaluatedExpression) deepCopy();
    }

    @Override
    public AbstractDispatchNode asDispatchNode(final Object rcvr, final Source source,
        final AbstractDispatchNode next) {
      ObjectLayout layout = ((SObject) rcvr).getObjectLayout();
      StorageLocation storage =
          layout.getStorageLocation(((FieldWriteNode) write).getFieldIndex());
      return new CachedFieldWriteAndSelf(rcvr.getClass(), layout, source, storage, next);
    }
  }
}
