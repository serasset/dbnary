package org.getalp.dbnary.wiki;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class CommonWikiTextLoader {

  protected WikiText getWikiTextFor(String pagename) throws IOException {
    InputStream inputStream =
        CommonWikiTextLoader.class.getResourceAsStream(pagename + ".wiki");
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length;
    while ((length = inputStream.read(buffer)) != -1) {
      result.write(buffer, 0, length);
    }
    String pageContent = result.toString(StandardCharsets.UTF_8);
    return new WikiText(pagename, pageContent);
  }
}
