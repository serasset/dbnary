package org.getalp.atef.example;

// Generated from FILAMALX.VARBS.txt by ATEF v0.1-SNAPSHOT
import java.util.Set;
import java.util.HashSet;
import org.getalp.atef.runtime.Decoration;

public class Deco_FILAMALX_VARBS extends Decoration {
  public enum Type_DRV {
    val_VN1, val_VN2, val_VN3, val_VN4, val_VA1, val_VA2, val_VA3, val_AN1, val_NN1, val_DRA1, val_DRA2
  };

  public Type_DRV var_DRV = null;

  public enum Type_SUBV {
    val_RFL, val_HAB, val_RST, val_PAI
  };

  public Type_SUBV var_SUBV = null;

  public enum Type_RS {
    val_CAUSE, val_COND, val_FINAL, val_CONSEQ, val_ANALOG, val_CONCES, val_QFIER, val_QFOBJ, val_LOCAL, val_QUAL, val_ID
  };

  public Type_RS var_RS = null;

  public enum Type_TOURN {
    val_TENC, val_TSU
  };

  public Set<Type_TOURN> var_TOURN = new HashSet();

  public enum Type_VAL1 {
    val_NOM, val_ACC, val_DAT, val_GEN
  };

  public Set<Type_VAL1> var_VAL1 = new HashSet();

  public enum Type_VAL2A {
    val_NOM, val_ACC, val_DAT, val_GEN
  };

  public Set<Type_VAL2A> var_VAL2A = new HashSet();

  public enum Type_VAL2B {
    val_ALS, val_AN, val_AUF, val_AUS, val_BEI, val_DURCH, val_FUER, val_GEGEN, val_HINTER, val_IN, val_MIT, val_NACH, val_UEBER, val_UM, val_UNTER, val_VON, val_VOR, val_WIE, val_ZU, val_ZWISCHEN, val_DOUB
  };

  public Set<Type_VAL2B> var_VAL2B = new HashSet();

  public enum Type_VAL3 {
    val_PHSUB, val_PHINF, val_PHINF1, val_GPPA, val_QUEST
  };

  public Set<Type_VAL3> var_VAL3 = new HashSet();

  public enum Type_VAL4 {
    val_NOM, val_ACC, val_DAT, val_GEN
  };

  public Set<Type_VAL4> var_VAL4 = new HashSet();

}
