package trufflesom.interpreter.nodes.supernodes;

import bd.inlining.ScopeAdaptationVisitor;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import trufflesom.compiler.Variable;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.LocalVariableNode;
import trufflesom.primitives.arithmetic.MultiplicationPrim;

/**
 * Matches the following AST:
 * <pre>
 * LocalVariableWriteNode
 *     LocalVariableReadNode
 *     LocalVariableReadNode (same as the previous read)
 *     MultiplicationPrim
 * </pre>
 *
 * ...and replaces it with:
 * <pre>
 * AssignLocalSquareToLocalNode
 * </pre>
 */
public abstract class AssignLocalSquareToLocalNode extends LocalVariableNode {
    private final FrameSlot squaredVarSlot;
    private final LocalVariableNode originalSubtree;

    public AssignLocalSquareToLocalNode(final Variable.Local variable,
                                        final Variable.Local squaredVar,
                                        final LocalVariableNode originalSubtree) {
        super(variable);
        this.squaredVarSlot = squaredVar.getSlot();
        this.originalSubtree = originalSubtree;
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class, ArithmeticException.class})
    public final long writeLong(final VirtualFrame frame) throws FrameSlotTypeException {
        long localVarValue = frame.getLong(this.squaredVarSlot);
        long newValue = Math.multiplyExact(localVarValue, localVarValue);
        frame.setLong(slot, newValue);
        return newValue;
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class, ArithmeticException.class})
    public final double writeDouble(final VirtualFrame frame) throws FrameSlotTypeException {
        double localVarValue = frame.getDouble(this.squaredVarSlot);
        double newValue = localVarValue * localVarValue;
        frame.setDouble(slot, newValue);
        return newValue;
    }

    @Specialization(replaces = {"writeLong", "writeDouble"})
    public final Object writeGeneric(final VirtualFrame frame) {
        Object result = originalSubtree.executeGeneric(frame);
        replace(originalSubtree);
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[ " + local.name + " ]";
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
        /*
         * This should never happen because ``replaceAfterScopeChange`` is only called in the
         * parsing stage, whereas the ``IncrementOperationNode`` superinstruction is only inserted
         * into the AST *after* parsing.
         */
        throw new RuntimeException("replaceAfterScopeChange: This should never happen!");
    }

    /**
     * Check if the AST subtree has the shape of an increment operation.
     */
    public static boolean isSquareAssignmentOperation(ExpressionNode exp) {
        if (exp instanceof MultiplicationPrim) {
            MultiplicationPrim mulPrim = (MultiplicationPrim) exp;
            if (mulPrim.getReceiver() instanceof LocalVariableReadNode
                    && mulPrim.getArgument() instanceof LocalVariableReadNode) {
                LocalVariableReadNode var1 = (LocalVariableReadNode) mulPrim.getReceiver();
                LocalVariableReadNode var2 = (LocalVariableReadNode) mulPrim.getArgument();
                return var1.getLocal().equals(var2.getLocal());
            }
        }
        return false;
    }

    /**
     * Replace ``node`` with a superinstruction. Assumes that the AST subtree has the correct shape.
     */
    public static void replaceNode(final LocalVariableWriteNode node) {
        MultiplicationPrim mulPrim = (MultiplicationPrim) node.getExp();
        if (mulPrim.getArgument() instanceof LocalVariableReadNode) {
            LocalVariableReadNode localVarNode = (LocalVariableReadNode) mulPrim.getArgument();
            AssignLocalSquareToLocalNode newNode = AssignLocalSquareToLocalNodeGen
                    .create(node.getLocal(), localVarNode.getLocal(), node)
                    .initialize(node.getSourceCoordinate());
            node.replace(newNode);
        }
    }
}
