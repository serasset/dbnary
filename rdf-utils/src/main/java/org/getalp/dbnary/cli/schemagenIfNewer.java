package org.getalp.dbnary.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.regex.Pattern;

public class schemagenIfNewer {

  public static void main(String[] args) {
    String ontologyFile = getOption(args, "-i");
    String outputDir = getOption(args, "-o", ".");
    String packageDir = getOption(args, "--package", "").replaceAll("\\.", "/");
    String className = getOption(args, "-n");
    Path outputFile = Path.of(outputDir).resolve(packageDir).resolve(className + ".java");

    FileTime ontologyModificationDate = fileModificationDate(ontologyFile);
    FileTime outputModificationDate = fileModificationDate(outputFile);

    if (Pattern.matches("^\\p{Alpha}:.*", ontologyFile)) {
      ontologyFile = ontologyFile.replaceAll("\\\\", "/");
    }
    setOption(args, "-i", ontologyFile);
    // System.err.format("Ontology: %s / output %s%n", ontologyFile, outputFile);
    // System.err.format("Ontology Date: %s / output date %s%n", ontologyModificationDate,
    // outputModificationDate);

    if (null != ontologyModificationDate && null != outputModificationDate
        && ontologyModificationDate.compareTo(outputModificationDate) < 0) {
      return;
    }
    jena.schemagen.main(args);
  }

  private static String getOption(String[] args, String s, String defaultValue) {
    String option = getOption(args, s);
    return option == null ? defaultValue : option;
  }

  private static String getOption(String[] args, String s) {
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals(s)) {
        return args[i + 1];
      }
    }
    return null;
  }

  private static void setOption(String[] args, String s, String value) {
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals(s)) {
        args[i + 1] = value;
      }
    }
  }

  private static FileTime fileModificationDate(String filename) {
    return fileModificationDate(Paths.get(filename));
  }

  private static FileTime fileModificationDate(Path file) {
    if (Files.exists(file)) {
      try {
        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
        return attr.lastModifiedTime();
      } catch (IOException e) {
        return null;
      }
    }
    return null;
  }
}
