package org.jsi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class Units {
  @Retention(RetentionPolicy.RUNTIME)      
  @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
  public @interface Dimension {
    double s()      default 0;
    double m()      default 0;
    double kg()     default 0;
    double A()      default 0;
    double K()      default 0;
    double mol()    default 0;
    double cd()     default 0;
    double scalar() default 1;
  } //base unit

  //Time
  @Dimension(s = 1)
  @Retention(RetentionPolicy.CLASS)
  @Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
  public @interface s {} //second

  //Length
  @Dimension(m = 1)
  @Retention(RetentionPolicy.CLASS)
  @Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
  public @interface m {} //meters 
  
  //Mass
  @Dimension(kg = 1)
  @Retention(RetentionPolicy.CLASS)
  @Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
  public @interface kg {} //kilogram
  
  //Electric Current
  @Dimension(A = 1)
  @Retention(RetentionPolicy.CLASS)
  @Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
  public @interface A {} //ampere

  //Thermodynamic Temperature
  @Dimension(K = 1)
  @Retention(RetentionPolicy.CLASS)
  @Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
  public @interface K {} //kelvin
  
  //Amount of Substance
  @Dimension(mol = 1)
  @Retention(RetentionPolicy.CLASS)
  @Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
  public @interface mol {} //mol
  
  //Luminous Intensity
  @Dimension(cd = 1)
  @Retention(RetentionPolicy.CLASS)
  @Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
  public @interface cd {} //candela
}

