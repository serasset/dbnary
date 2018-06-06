package org.getalp.atef.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.getalp.atef.Lex;
import org.getalp.atef.listeners.VariableDeclListener;
import org.getalp.atef.VariableDeclaration;
import org.getalp.atef.codegen.ATEFCodeGenerator;
import org.getalp.atef.model.LinguisticDecoration;

public class CompileVariableDeclarations {

  public static LinguisticDecoration parse(String filename, InputStream is) throws IOException {
    CharStream cs = CharStreams.fromStream(is);
    return parse(filename, cs);
  }

  public static LinguisticDecoration parse(String filename, Reader rdr) throws IOException {
    CharStream cs = CharStreams.fromReader(rdr, filename);
    return parse(filename, cs);
  }

  public static LinguisticDecoration parse(String filename, CharStream cs) {
    // Passing the input to the lexer to create tokens
    Lex lexer = new Lex(cs);

    CommonTokenStream tokens = new CommonTokenStream(lexer);

    // Passing the tokens to the parser to create the parse tree.
    VariableDeclaration parser = new VariableDeclaration(tokens);
    // parser.setTrace(true);
    // Adding the listener to facilitate walking through parse tree.
    VariableDeclListener vdListener = new VariableDeclListener(filename);
    parser.addParseListener(vdListener);

    // invoking the parser.
    parser.dv();

    return vdListener.getDecoration();
  }

  public static void generateCode(LinguisticDecoration deco, String filename, OutputStream out) {
    ATEFCodeGenerator gen = new ATEFCodeGenerator(deco, filename);
    gen.generateCode();
  }

}
