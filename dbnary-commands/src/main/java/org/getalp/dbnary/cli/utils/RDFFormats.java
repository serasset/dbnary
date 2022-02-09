package org.getalp.dbnary.cli.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.jena.riot.RDFLanguages;

public class RDFFormats extends ArrayList<String> {
  private static final HashSet<String> names = new HashSet<>();
  private static final HashMap<String, String> name2Extension = new HashMap<String, String>();

  static {
    RDFLanguages.getRegisteredLanguages().forEach(l -> {
      Optional<String> extension = l.getFileExtensions().stream().findFirst();
      if (extension.isPresent()) {
        names.add(l.getName());
        name2Extension.put(l.getName(), extension.get());
      }
    });
  }

  public RDFFormats() {
    super(new ArrayList<>(names));
  }

  public static Set<String> getKnownFormats() {
    return names;
  }

  public static String getFormatsForHelp() {
    return String.join(", ", names);
  }

  public static String getExtension(String model) {
    return name2Extension.getOrDefault(model, "ttl");
  }
}
