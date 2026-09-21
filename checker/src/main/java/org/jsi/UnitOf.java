package org.jsi;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.jsi.annotations.Units.Dimension;

/**
 * Resolves the {@link SIUnit} carried by an annotated declaration.
 *
 * <p>Throughout, {@code null} means "unannotated", not "dimensionless". Unannotated
 * code must flow through the checker without producing diagnostics, so callers are
 * expected to propagate null silently rather than substituting a default.
 *
 * <p>Conflict reporting is deliberately left to callers: the annotation processor
 * reports through a {@code Messager}, the javac plugin through {@code Trees}, and
 * neither should be a dependency of this class. Use {@link #unitAnnotationsOn} when
 * you want to diagnose a conflict, {@link #unitOf(Element)} when you only want the
 * answer.
 */
public final class UnitOf {
  /**
   * The unit declared by a single annotation, or null if this annotation is not a unit
   * annotation (i.e. it carries no {@code @Dimension} meta-annotation) —
   * {@code @Override}, {@code @Deprecated} and friends all land here.
   */
  public static SIUnit unitOf(AnnotationMirror mirror) {
    TypeElement annoType = (TypeElement) mirror.getAnnotationType().asElement();
    Dimension d = annoType.getAnnotation(Dimension.class);
    if (d == null) return null;
    return new SIUnit(d.s(), d.m(), d.kg(), d.A(), d.K(), d.mol(), d.cd(), d.scalar());
  }

  /**
   * The unit of a declaration, or null if it is unannotated <em>or</em> carries
   * conflicting annotations. Callers that need to distinguish those two cases — and
   * report the second — should use {@link #unitAnnotationsOn} directly.
   */
  public static SIUnit unitOf(Element e) {
    List<AnnotationMirror> units = unitAnnotationsOn(e);
    return units.size() == 1 ? unitOf(units.get(0)) : null;
  }

  /**
   * The unit written in type position on a type, or null if there is none. This is the
   * only way to reach units nested inside a generic argument, e.g. the {@code @m} in
   * {@code List<@m Double>}.
   */
  public static SIUnit unitOf(TypeMirror t) {
    for (AnnotationMirror am : t.getAnnotationMirrors()) {
      SIUnit u = unitOf(am);
      if (u != null) return u;
    }
    return null;
  }

  /**
   * Every distinct unit annotation on this element, in source order, looking at both
   * declaration annotations and annotations written in type position.
   *
   * <p>A unit annotation targeting both {@code FIELD} and {@code TYPE_USE} is recorded
   * by javac in <em>both</em> places for the same field, so the two lists are unioned
   * by annotation type rather than concatenated; otherwise every annotated field would
   * look like it had conflicting units.
   *
   * <p>An empty list means unannotated; more than one entry means the declaration
   * carries genuinely conflicting units and the caller should report an error against
   * the second and later entries.
   */
  public static List<AnnotationMirror> unitAnnotationsOn(Element e) {
    List<AnnotationMirror> found = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();

    for (AnnotationMirror am : e.getAnnotationMirrors()) {
      collect(am, found, seen);
    }
    for (AnnotationMirror am : valueTypeOf(e).getAnnotationMirrors()) {
      collect(am, found, seen);
    }
    return found;
  }

  private static void collect(AnnotationMirror am, List<AnnotationMirror> found, Set<String> seen) {
    if (unitOf(am) == null) return;
    TypeElement annoType = (TypeElement) am.getAnnotationType().asElement();
    if (seen.add(annoType.getQualifiedName().toString())) found.add(am);
  }

  /**
   * The type whose annotations describe this element's own value: the return type for a
   * method, the declared type for anything else. {@code ExecutableElement.asType()}
   * yields the method's signature rather than its return type, so it cannot be used
   * here unconditionally.
   */
  private static TypeMirror valueTypeOf(Element e) {
    return e instanceof ExecutableElement m ? m.getReturnType() : e.asType();
  }
}
