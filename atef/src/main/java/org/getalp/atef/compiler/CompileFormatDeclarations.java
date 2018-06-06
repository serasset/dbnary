package org.getalp.atef.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.List;
import java.util.Set;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.getalp.atef.FormatDeclaration;
import org.getalp.atef.FormatsLexer;
import org.getalp.atef.Lex;
import org.getalp.atef.VariableDeclaration;
import org.getalp.atef.codegen.ATEFCodeGenerator;
import org.getalp.atef.listeners.FormatDeclListener;
import org.getalp.atef.listeners.VariableDeclListener;
import org.getalp.atef.model.LinguisticDecoration;
import org.getalp.atef.model.LinguisticFormats;

public class CompileFormatDeclarations {

  public static LinguisticFormats parse(String filename, InputStream is,
      List<LinguisticDecoration> decorations) throws IOException {
    CharStream cs = CharStreams.fromStream(is);
    return parse(filename, cs, decorations);
  }

  public static LinguisticFormats parse(String filename, Reader rdr,
      List<LinguisticDecoration> decorations) throws IOException {
    CharStream cs = CharStreams.fromReader(rdr, filename);
    return parse(filename, cs, decorations);
  }

  public static LinguisticFormats parse(String filename, CharStream cs,
      List<LinguisticDecoration> decorations) {
    // Passing the input to the lexer to create tokens
    FormatsLexer lexer = new FormatsLexer(cs);

    CommonTokenStream tokens = new CommonTokenStream(lexer);

    // Passing the tokens to the parser to create the parse tree.
    FormatDeclaration parser = new FormatDeclaration(tokens);
    // parser.setTrace(true);
    // Adding the listener to facilitate walking through parse tree.
    FormatDeclListener fmtListener = new FormatDeclListener(filename, decorations);
    parser.addParseListener(fmtListener);

    // invoking the parser.
    parser.formats();

    return fmtListener.getFormats();
  }

  public static void generateCode(LinguisticDecoration deco, String filename, OutputStream out) {
    ATEFCodeGenerator gen = new ATEFCodeGenerator(deco, filename);
    gen.generateCode();
  }

}
