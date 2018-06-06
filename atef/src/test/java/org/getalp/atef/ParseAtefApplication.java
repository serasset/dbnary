package org.getalp.atef;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.getalp.atef.compiler.AtefFormatNormalizer;
import org.getalp.atef.listeners.FormatDeclListener;
import org.getalp.atef.model.AtefApplication;
import org.junit.Before;
import org.junit.Test;

public class ParseAtefApplication {

  private String appName = "FILAMALX";
  private AtefApplication app;

  @Before
  public void initializeApp() {
    List<String> decorations = new ArrayList<>();
    List<String> formats = new ArrayList<>();
    List<String> dicts = new ArrayList<>();
    List<String> grams = new ArrayList<>();

    decorations.add(appName + ".VARBM.txt");
    decorations.add(appName + ".VARBMS.txt");

    formats.add(appName + ".FMATM.txt");
    formats.add(appName + ".FMATS.txt");
    formats.add(appName + ".FMATG.txt");

    app = new AtefApplication(appName, decorations, formats, grams, dicts);
  }

  @Test
  public void testParsing() throws IOException {
    app.preloadDeclarations();
  }

  @Test
  public void testFMATSParsing() throws IOException {
    // launchFormatParser("FILAMALX.FMATS.txt");
  }

}
