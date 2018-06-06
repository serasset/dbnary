package org.getalp.atef.codegen;

import org.getalp.atef.model.LinguisticDecoration;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public class ATEFCodeGenerator {
  public static final String TEMPLATE_ROOT = "org/getalp/atef/templates/codegen";

  public LinguisticDecoration decoration;
  public CodegenFile file;
  private STGroup templates;

  public ATEFCodeGenerator(LinguisticDecoration decoration, String filename) {
    this.decoration = decoration;
    this.file = new CodegenFile(filename);
  }

  public void generateCode() {
    STGroup templates = getTemplates();

    ST st = templates.getInstanceOf("DecorationFile");
    st.add("file", this.file);
    st.add("decoration", this.decoration);
    System.out.println(st.render());
  }

  public STGroup getTemplates() {
    if (templates == null) {
      templates = loadTemplates();
    }

    return templates;
  }

  protected STGroup loadTemplates() {
    String groupFileName = ATEFCodeGenerator.TEMPLATE_ROOT + "/Atef.stg";
    STGroup result = null;
    try {
      result = new STGroupFile(groupFileName);
    } catch (IllegalArgumentException iae) {
      System.err.println(iae.getLocalizedMessage());
      iae.printStackTrace(System.err);
    }
    // if ( result==null ) return null;
    // result.registerRenderer(Integer.class, new NumberRenderer());
    // result.registerRenderer(String.class, new StringRenderer());
    // result.setListener(new STErrorListener() {
    // @Override
    // public void compileTimeError(STMessage msg) {
    // reportError(msg);
    // }
    //
    // @Override
    // public void runTimeError(STMessage msg) {
    // reportError(msg);
    // }
    //
    // @Override
    // public void IOError(STMessage msg) {
    // reportError(msg);
    // }
    //
    // @Override
    // public void internalError(STMessage msg) {
    // reportError(msg);
    // }
    //
    // private void reportError(STMessage msg) {
    // getCodeGenerator().tool.errMgr.toolError(ErrorType.STRING_TEMPLATE_WARNING, msg.cause,
    // msg.toString());
    // }
    // });

    return result;
  }

}
