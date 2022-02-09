package org.getalp.dbnary.cli.mixins;

import org.getalp.LangTools;
import org.getalp.dbnary.cli.DBnary;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

public class SingleLanguageAsOptionMixin {
  private static final String DEFAULT_LANGUAGE = "en";

  @Spec(Spec.Target.MIXEE)
  CommandSpec mixee;

  @ParentCommand
  private DBnary parent;

  protected String language = DEFAULT_LANGUAGE;

  @Option(names = {"-l", "--language"}, paramLabel = "LANGUAGE", defaultValue = DEFAULT_LANGUAGE,
      description = "language edition of the dump to be extracted; uses a 2 or 3 iso letter code;"
          + " Default: ${DEFAULT-VALUE}.")
  public void setLanguage(String language) {
    this.language = LangTools.getCode(language);
    if (null == this.language) {
      throw new ParameterException(mixee.commandLine(), String.format(
          "Invalid language '%s' for option '--language': unknown language code.", language));
    }
  }

  public String getLanguage() {
    return language;
  }
}
