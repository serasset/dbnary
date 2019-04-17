package org.getalp.dbnary.deu;

import info.bliki.extensions.scribunto.template.Invoke;
import info.bliki.wiki.model.IWikiModel;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GermanInvoke extends Invoke {

  private Logger log = LoggerFactory.getLogger(GermanTableExtractorWikiModel.class);

  @Override
  public String parseFunction(List<String> parts, IWikiModel model, char[] src, int beginIndex,
      int endIndex, boolean isSubst) throws IOException {
    log.trace("#invoke:{}|{}", parts.get(0), parts.get(1));
    return super.parseFunction(parts, model, src, beginIndex, endIndex, isSubst);
  }

}
