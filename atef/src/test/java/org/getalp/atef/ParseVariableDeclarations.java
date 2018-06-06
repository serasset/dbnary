package org.getalp.atef;

import java.io.IOException;
import java.io.InputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.getalp.atef.codegen.ATEFCodeGenerator;
import org.getalp.atef.listeners.VariableDeclListener;
import org.junit.Test;

public class ParseVariableDeclarations {

  public void launchDVParser(String filename) throws IOException {
    InputStream is = this.getClass().getResourceAsStream(filename);

    // Loading the DSL script into the ANTLR stream.
    CharStream cs = CharStreams.fromStream(is);

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
    ParserRuleContext variableDeclarations = parser.dv();

    ATEFCodeGenerator gen = new ATEFCodeGenerator(vdListener.getDecoration(), filename);
    gen.generateCode();

    System.out.println(vdListener.getDecoration());

  }

  @Test
  public void testVarBMParsing() throws IOException {
    launchDVParser("FILAMALX.VARBM.txt");
  }

  @Test
  public void testVarBSParsing() throws IOException {
    launchDVParser("FILAMALX.VARBS.txt");
  }

}
