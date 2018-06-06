package org.getalp.atef.listeners;

import static org.getalp.atef.listeners.VariableDeclListener.VariableType.*;
import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.getalp.atef.VariableDeclaration.DvarContext;
import org.getalp.atef.VariableDeclaration.ValContext;
import org.getalp.atef.VariableDeclaration.VarTypeContext;
import org.getalp.atef.VariableDeclarationBaseListener;
import org.getalp.atef.model.types.ArithType;
import org.getalp.atef.model.types.ExclusiveType;
import org.getalp.atef.model.LinguisticDecoration;
import org.getalp.atef.model.types.NonExclusiveType;
import org.getalp.atef.model.types.StringType;
import org.getalp.atef.model.Variable;

public class VariableDeclListener extends VariableDeclarationBaseListener {

  enum VariableType {
    EXC, NEX, ARITH, CHAINE, GEN
  }

  protected VariableType currentBlocType = null;
  protected LinguisticDecoration decoration;

  public VariableDeclListener(String filename) {
    super();
    this.decoration = new LinguisticDecoration(getName(filename));
  }

  private String getName(String filename) {
    return "Deco_" + filename.replaceAll(".txt$", "").replaceAll("\\.", "_");
  }

  public List<String> getNames(List<ValContext> valContexts) {
    return valContexts.stream().map(ValContext::getText).collect(Collectors.toList());
  }

  @Override
  public void exitDvar(DvarContext ctx) {
    Variable v = null;
    switch (currentBlocType) {
      case EXC:
        v = new Variable(ctx.id.getText(), new ExclusiveType(getNames(ctx.val())));
        decoration.addExclusiveVariable(v);
        break;
      case NEX:
        v = new Variable(ctx.id.getText(), new NonExclusiveType(getNames(ctx.val())));
        decoration.addNonExclusiveVariable(v);
        break;
      case ARITH:
        v = new Variable(ctx.id.getText(), new ArithType(0)); // Don't know what the value should
                                                              // be...
        decoration.addArithVariable(v);
        break;
      case CHAINE:
        v = new Variable(ctx.id.getText(), new StringType());
        decoration.addStringVariable(v);
        break;
      case GEN:

        break;
    }
    super.exitDvar(ctx);
  }

  @Override
  public void exitVarType(VarTypeContext ctx) {
    TerminalNode t;
    if (null != (t = ctx.EXC())) {
      currentBlocType = EXC;
    } else if (null != (t = ctx.NEX())) {
      currentBlocType = NEX;
    } else if (null != (t = ctx.ARITH())) {
      currentBlocType = ARITH;
    } else if (null != (t = ctx.CHAINE())) {
      currentBlocType = CHAINE;
    } else if (null != (t = ctx.GEN())) {
      currentBlocType = GEN;
    } else {
      currentBlocType = null;
    }
    super.exitVarType(ctx);
  }

  public LinguisticDecoration getDecoration() {
    return decoration;
  }
}
