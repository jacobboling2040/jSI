package org.jsi;

public class SIUnit {
  private final double time;
  private final double length;
  private final double mass;
  private final double electricCurrent;
  private final double thermodynamicTemperature;
  private final double amountOfSubstance;
  private final double luminousIntensity;
  private final double scalar;

  public static final SIUnit DIMENSIONLESS = new SIUnit(0, 0, 0, 0, 0, 0, 0, 1);

  public SIUnit(
    double time, 
    double length, 
    double mass, 
    double electricCurrent, 
    double thermodynamicTemperature, 
    double amountOfSubstance, 
    double luminoisIntensity,
    double scalar
  ) {
      this.time = time;
      this.length = length;
      this.mass = mass;
      this.electricCurrent = electricCurrent;
      this.thermodynamicTemperature = thermodynamicTemperature;
      this.amountOfSubstance = amountOfSubstance;
      this.luminousIntensity = luminoisIntensity;
      this.scalar = scalar;
    }

  public boolean sameDimension(SIUnit unit) {
    return 
      this.time == unit.time && 
      this.length == unit.length &&
      this.mass == unit.mass &&
      this.electricCurrent == unit.electricCurrent &&
      this.thermodynamicTemperature == unit.thermodynamicTemperature &&
      this.amountOfSubstance == unit.amountOfSubstance &&
      this.luminousIntensity == unit.luminousIntensity;
  }

  /** True when every base-unit exponent is zero, whatever the scalar. */
  public boolean isDimensionless() {
    return sameDimension(DIMENSIONLESS);
  }

  public boolean sameScalar(SIUnit unit) {
    return this.scalar == unit.scalar;
  }

  public SIUnit multiply(SIUnit unit) {
    return new SIUnit(
      time + unit.time, 
      length + unit.length, 
      mass + unit.mass, 
      electricCurrent + unit.electricCurrent, 
      thermodynamicTemperature + unit.thermodynamicTemperature, 
      amountOfSubstance + unit.amountOfSubstance, 
      luminousIntensity + unit.luminousIntensity, 
      scalar * unit.scalar
    );
  }

  public SIUnit divide(SIUnit unit) {
    return new SIUnit(
      time - unit.time, 
      length - unit.length, 
      mass - unit.mass, 
      electricCurrent - unit.electricCurrent, 
      thermodynamicTemperature - unit.thermodynamicTemperature, 
      amountOfSubstance - unit.amountOfSubstance, 
      luminousIntensity - unit.luminousIntensity, 
      scalar / unit.scalar
    );
  }

  /**
   * Renders the unit the way it would be written by hand — {@code kg·m·s⁻²} — in the
   * conventional SI order, omitting base units with a zero exponent. Every diagnostic
   * the checker emits interpolates this, so it is the user-facing name of a unit.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    append(sb, mass,                     "kg");
    append(sb, length,                   "m");
    append(sb, time,                     "s");
    append(sb, electricCurrent,          "A");
    append(sb, thermodynamicTemperature, "K");
    append(sb, amountOfSubstance,        "mol");
    append(sb, luminousIntensity,        "cd");

    if (sb.length() == 0) return scalar == 1 ? "dimensionless" : plain(scalar);
    if (scalar != 1) sb.insert(0, plain(scalar) + "·");
    return sb.toString();
  }

  private static void append(StringBuilder sb, double exponent, String symbol) {
    if (exponent == 0) return;
    if (sb.length() > 0) sb.append('·');
    sb.append(symbol).append(exponentOf(exponent));
  }

  /**
   * An exponent as a suffix: empty for 1, superscript digits for whole numbers, and a
   * caret form for fractional ones, since there are no superscript decimal points.
   */
  private static String exponentOf(double exponent) {
    if (exponent == 1) return "";
    if (exponent != Math.rint(exponent) || Double.isInfinite(exponent)) {
      return "^" + plain(exponent);
    }
    StringBuilder out = new StringBuilder(exponent < 0 ? "⁻" : "");
    for (char c : Long.toString(Math.abs((long) exponent)).toCharArray()) {
      out.append("⁰¹²³⁴⁵⁶⁷⁸⁹".charAt(c - '0'));
    }
    return out.toString();
  }

  /** Drops the trailing {@code .0} that {@link Double#toString} adds to whole numbers. */
  private static String plain(double d) {
    return d == Math.rint(d) && !Double.isInfinite(d) ? Long.toString((long) d)
                                                      : Double.toString(d);
  }
}
