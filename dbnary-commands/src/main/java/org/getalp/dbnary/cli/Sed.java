package org.getalp.dbnary.cli;

import com.github.rwitzel.streamflyer.core.Modifier;
import com.github.rwitzel.streamflyer.core.ModifyingReader;
import com.github.rwitzel.streamflyer.regex.RegexModifier;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.regex.Pattern;

public class Sed {

  public static void main(String args[]) throws IOException {

    if (args.length != 2) {
      System.err.println("USAGE : gised <matching_regex> <replacement>");
    }

    // choose the character stream to modify
    Reader originalReader = new InputStreamReader(System.in);

    // select the modifier of your choice
    Modifier myModifier = new RegexModifier(args[0], Pattern.MULTILINE, args[1]);

    // create the modifying reader that wraps the original reader
    Reader modifyingReader = new ModifyingReader(originalReader, myModifier);

    Writer out = new OutputStreamWriter(System.out);
    char[] buf = new char[4096];
    int length;
    while ((length = modifyingReader.read(buf)) > 0) {
      out.write(buf, 0, length);
    }
    out.flush();
    out.close();
  }
}
