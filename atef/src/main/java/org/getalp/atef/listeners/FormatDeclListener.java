package org.getalp.atef.listeners;

import java.util.List;
import org.getalp.atef.FormatDeclaration.FormatGContext;
import org.getalp.atef.FormatDeclaration.FormatMSContext;
import org.getalp.atef.FormatDeclarationBaseListener;
import org.getalp.atef.model.LinguisticDecoration;
import org.getalp.atef.model.LinguisticFormats;

public class FormatDeclListener extends FormatDeclarationBaseListener {


  private final List<LinguisticDecoration> decorations;
  private LinguisticFormats formats;

  public FormatDeclListener(String filename, List<LinguisticDecoration> decorations) {
    super();
    this.decorations = decorations;
    this.formats = new LinguisticFormats();
  }

  private String getName(String filename) {
    return "Formats_" + filename.replaceAll(".txt$", "").replaceAll("\\.", "_");
  }

  @Override
  public void exitFormatMS(FormatMSContext ctx) {
    System.err.println(ctx);

    super.exitFormatMS(ctx);
  }

  @Override
  public void exitFormatG(FormatGContext ctx) {
    System.err.println(ctx);
    super.exitFormatG(ctx);
  }


  public LinguisticFormats getFormats() {
    return formats;
  }
}
