package org.getalp.atef.cli;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import org.getalp.atef.compiler.AtefFormatNormalizer;

public class NormalizeFormatsDeclaration {

  public static void main(String args[]) throws FileNotFoundException {
    InputStream in = System.in;
    OutputStream out = System.out;
    switch (args.length) {
      case 2:
        out = new FileOutputStream(args[1]);
      case 1:
        in = new FileInputStream(args[0]);
      default:
        break;
    }

    Reader inr = new InputStreamReader(in);
    Writer outw = new OutputStreamWriter(out);

    AtefFormatNormalizer.normalizeStreams(inr, outw);
  }

}
