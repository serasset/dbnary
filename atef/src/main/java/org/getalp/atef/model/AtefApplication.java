package org.getalp.atef.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.getalp.atef.compiler.AtefFormatNormalizer;
import org.getalp.atef.compiler.CompileFormatDeclarations;
import org.getalp.atef.compiler.CompileVariableDeclarations;

public class AtefApplication {

  String appName = null;
  String appPackage = null;
  boolean doFormatNormalization = true;

  public List<String> decorationFilenames;
  public List<String> formatFilenames;
  public List<String> grammarFilenames;
  public List<String> dictionaryFilenames;


  public List<LinguisticDecoration> decorations;
  public List<LinguisticFormats> formats;
  public List<AtefGrammars> grammars;
  public List<AtefDictionary> dictionaries;

  public AtefApplication(String appName) {
    this(appName, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
  }

  public AtefApplication(String appName, String appPackage) {
    this(appName, appPackage, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
        new ArrayList<>());
  }

  public AtefApplication(String appName, List<String> decorationFilenames,
      List<String> formatFilenames, List<String> grammarFilenames,
      List<String> dictionaryFilenames) {
    this(appName, null, decorationFilenames, formatFilenames, grammarFilenames,
        dictionaryFilenames);
  }

  public AtefApplication(String appName, String appPackage, List<String> decorationFilenames,
      List<String> formatFilenames, List<String> grammarFilenames,
      List<String> dictionaryFilenames) {
    this.appName = appName;
    this.appPackage = appPackage;
    this.decorationFilenames = decorationFilenames;
    this.formatFilenames = formatFilenames;
    this.grammarFilenames = grammarFilenames;
    this.dictionaryFilenames = dictionaryFilenames;
  }

  public void setDoFormatNormalization(boolean doFormatNormalization) {
    this.doFormatNormalization = doFormatNormalization;
  }

  public void preloadDeclarations() throws IOException {
    // Compile decorations
    for (String decorationFilename : decorationFilenames) {
      // Open the reader to the file
      Reader rdr = openReader(decorationFilename);
      decorations.add(CompileVariableDeclarations.parse(decorationFilename, rdr));
    }

    // then formats
    for (String formatFilename : formatFilenames) {
      // Open the reader to the file
      Reader rdr = openReader(formatFilename);
      if (doFormatNormalization) {
        rdr = AtefFormatNormalizer.normalizedReader(rdr);
      }
      CompileFormatDeclarations.parse(formatFilename, rdr, decorations);
    }



  }

  private Reader openReader(String filename) {
    InputStream is = null;
    // First try if the file exists on disk
    File f = new File(filename);
    if (f.exists() && f.canRead()) {
      try {
        is = new FileInputStream(f);
      } catch (FileNotFoundException e) {
        // This should not happen as the file existance has been previously tested
        e.printStackTrace();
      }
    } else {
      // Else try to retreive the file from the classloader
      is = this.getClass().getResourceAsStream(filename);
    }

    return null == is ? null : new InputStreamReader(is);
  }

}
