package org.jsi;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

public class UnitTreeScanner extends TreePathScanner<SIUnit, Object> {
  private final Trees trees;
  private final CompilationUnitTree unit;

  public UnitTreeScanner(Trees trees, CompilationUnitTree unit){
    this.trees = trees;
    this.unit = unit;
  }

  @Override
  public SIUnit visitIdentifier(IdentifierTree node, Object p) {
    Element e = trees.getElement(getCurrentPath());
    return e == null ? null : UnitOf.unitOf(e);
  }

  @Override
  public SIUnit visitParenthesized(ParenthesizedTree node, Object p) {
    return scan(node.getExpression(), p);
  }

  @Override
  public SIUnit visitLiteral(LiteralTree node, Object p) {
    return SIUnit.DIMENSIONLESS;
  }


  /**
   * Checks a declaration's initializer against its declared unit, and yields the
   * declared unit as the variable's own.
   *
   * <p>A dimensionless initializer is accepted for any declared unit, so that
   * {@code @m double d = 10;} is how a united value is introduced. Without that
   * exception no annotated variable could ever be given a starting value.
   */
  @Override
  public SIUnit visitVariable(VariableTree node, Object p) {
    SIUnit init = scan(node.getInitializer(), p);
    SIUnit u = UnitOf.unitOf(trees.getElement(getCurrentPath()));

    // No initializer (parameters, catch clauses) or an unannotated side: nothing to check.
    if (init == null || u == null) return u;

    return assignable(node.getInitializer(), init, u) ? u : null;
  }


  /**
   * {@code d = t} carries the same rule as a declaration: the value must fit the
   * variable. Yields the variable's unit, since a Java assignment is an expression
   * whose value is the one assigned.
   */
  @Override
  public SIUnit visitAssignment(AssignmentTree node, Object p) {
    SIUnit target = scan(node.getVariable(), p);
    SIUnit value  = scan(node.getExpression(), p);
    if (target == null || value == null) return target;

    return assignable(node.getExpression(), value, target) ? target : null;
  }

  @Override
  public SIUnit visitMemberSelect(MemberSelectTree node, Object p) {
      scan(node.getExpression(), p);
      Element e = trees.getElement(getCurrentPath());
      return e == null ? null : UnitOf.unitOf(e);
  }

  /**
   * A method call such as {@code speed()}, {@code car.speed(t)} or {@code Math.abs(x)}.
   * The node holds the method select (the {@code speed} or {@code car.speed} part, an
   * identifier or a member select) and the argument list.
   *
   * <p>A call's unit should be the unit on the invoked method's return type. For now this
   * defers to {@code TreeScanner}, which visits the method select and every argument and
   * yields whatever the last argument yielded, or null when there are no arguments. That
   * result is not the return unit, so a call is not yet reliably checked.
   */
  @Override
  public SIUnit visitMethodInvocation(MethodInvocationTree node, Object p) {
      SIUnit method = scan(node.getMethodSelect(), p);
      SIUnit args = scan(node.getArguments(), p);
      if (method == null || args ==  null) return null;
      if (method.sameDimension(args)) {
        return null;
      }
      return method;
  }

  @Override
  public SIUnit visitReturn(ReturnTree node, Object p) {
      return super.visitReturn(node, p);
  }

  @Override
  public SIUnit visitUnary(UnaryTree node, Object p) {
      return super.visitUnary(node, p);
  }

  @Override
  public SIUnit visitTypeCast(TypeCastTree node, Object p) {
      return super.visitTypeCast(node, p);
  }

  @Override
  public SIUnit visitConditionalExpression(ConditionalExpressionTree node, Object p) {
      return super.visitConditionalExpression(node, p);
  }

  @Override
  public SIUnit visitNewClass(NewClassTree node, Object p) {
      return super.visitNewClass(node, p);
  }

  @Override
  public SIUnit visitArrayAccess(ArrayAccessTree node, Object p) {
      return super.visitArrayAccess(node, p);
  }

  @Override
  public SIUnit visitNewArray(NewArrayTree node, Object p) {
      return super.visitNewArray(node, p);
  }

  @Override
  public SIUnit visitEnhancedForLoop(EnhancedForLoopTree node, Object p) {
      return super.visitEnhancedForLoop(node, p);
  }

  @Override
  public SIUnit visitMethod(MethodTree node, Object p) {
      return super.visitMethod(node, p);
  }

  @Override
  public SIUnit visitLambdaExpression(LambdaExpressionTree node, Object p) {
      return super.visitLambdaExpression(node, p);
  }

  @Override
  public SIUnit visitMemberReference(MemberReferenceTree node, Object p) {
      return super.visitMemberReference(node, p);
  }


  /**
   * {@code d op= t} means {@code d = d op t}, so unlike the matching binary operator the
   * result has to fit back into {@code d}, whose unit is fixed by its declaration.
   *
   * <p>That splits the operators in two. {@code +=}, {@code -=} and {@code %=} demand
   * agreeing operands exactly as {@code +} does. {@code *=} and {@code /=} cannot mint a
   * new unit the way {@code *} and {@code /} freely do — {@code dist * time} is a
   * perfectly good {@code m·s}, but {@code dist *= time} has nowhere to put it — so the
   * right-hand side must be dimensionless. Scaling a length is fine; re-dimensioning one
   * in place is not.
   */
  @Override
  public SIUnit visitCompoundAssignment(CompoundAssignmentTree node, Object p) {
    // `msg += dist` is concatenation, not arithmetic. Checked before anything else,
    // since there is no BinaryTree here for visitBinary to catch.
    if (isStringConcat()) return null;

    SIUnit target = scan(node.getVariable(), p);
    SIUnit value  = scan(node.getExpression(), p);
    if (target == null || value == null) return target;

    Tree at = node.getExpression();

    return switch (node.getKind()) {
      case PLUS_ASSIGNMENT, MINUS_ASSIGNMENT ->
          compatible(at, target, value,
              node.getKind() == Kind.PLUS_ASSIGNMENT ? "add" : "subtract") ? target : null;

      case REMAINDER_ASSIGNMENT ->
          compatible(at, target, value, "take the remainder of") ? target : null;

      case MULTIPLY_ASSIGNMENT, DIVIDE_ASSIGNMENT -> {
        if (value.isDimensionless()) yield target; // scaling leaves the dimension alone
        boolean times = node.getKind() == Kind.MULTIPLY_ASSIGNMENT;
        SIUnit result = times ? target.multiply(value) : target.divide(value);
        error(at, "cannot " + (times ? "multiply" : "divide") + " " + target + " by "
                + value + " in place: result " + result + " does not fit " + target);
        yield null;
      }

      default -> target; // shifts and bitwise ops are integer-only; no units involved
    };
  }


  @Override
  public SIUnit reduce(SIUnit r1, SIUnit r2) {
      return null;
  }

  @Override
  public SIUnit visitBinary(BinaryTree node, Object p) {
    SIUnit l = scan(node.getLeftOperand(), p);
    SIUnit r = scan(node.getRightOperand(), p);
    if (l == null || r == null) return null; //unannotated

    return switch (node.getKind()) {
      case PLUS, MINUS -> {
          if (isStringConcat()) yield null;
          yield compatible(node, l, r, node.getKind() == Kind.PLUS ? "add" : "subtract") ? l : null;
      }

      case REMAINDER ->
          compatible(node, l, r, "take the remainder of") ? l : null;

      case MULTIPLY -> l.multiply(r);
      case DIVIDE   -> l.divide(r);

      case LESS_THAN, GREATER_THAN, LESS_THAN_EQUAL,
            GREATER_THAN_EQUAL, EQUAL_TO, NOT_EQUAL_TO -> {
        compatible(node, l, r, "compare");
        yield SIUnit.DIMENSIONLESS; // the boolean result carries no unit
      }

      default -> SIUnit.DIMENSIONLESS; // &&, ||, shifts, bitwise
    };
  }

  /**
   * Whether a value may be stored in a target of the given unit, reporting if not.
   *
   * <p>A dimensionless value is accepted into any target. That exception is how a united
   * value is first introduced ({@code @m double d = 10;}) and later re-set
   * ({@code d = 20;}); without it nothing could ever bootstrap a unit. It deliberately
   * does not extend to arithmetic, where {@code d + 1.0} stays an error.
   */
  private boolean assignable(Tree at, SIUnit value, SIUnit target) {
    if (value.isDimensionless() && !target.isDimensionless()) return true;
    return compatible(at, value, target, "assign", "to");
  }

  /**
   * Reports a mismatch between two operands that must agree, and says whether they do.
   * Dimension and scale are separate diagnostics: the first is a mistake, the second
   * just needs an explicit conversion.
   */
  private boolean compatible(Tree at, SIUnit l, SIUnit r, String verb) {
    return compatible(at, l, r, verb, "and");
  }

  /**
   * As {@link #compatible(Tree, SIUnit, SIUnit, String)}, but with the word joining the
   * two units spelled out, so directional checks read as "cannot assign m to s" rather
   * than the symmetric "and" that suits operands.
   */
  private boolean compatible(Tree at, SIUnit l, SIUnit r, String verb, String joiner) {
    if (!l.sameDimension(r)) {
      error(at, "cannot " + verb + " " + l + " " + joiner + " " + r);
      return false;
    }
    if (!l.sameScalar(r)) {
      error(at, "explicit conversion required between " + l + " and " + r);
      return false;
    }
    return true;
  }

  private void error(Tree at, String msg) {
    trees.printMessage(Diagnostic.Kind.ERROR, msg, at, unit);
  }

  private boolean isStringConcat() {
    TypeMirror t = trees.getTypeMirror(getCurrentPath());
    return t != null &&
      t.getKind() == TypeKind.DECLARED &&
      ((TypeElement) ((DeclaredType) t).asElement())
        .getQualifiedName().contentEquals("java.lang.String");
  }
}
