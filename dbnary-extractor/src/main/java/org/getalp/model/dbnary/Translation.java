package org.getalp.model.dbnary;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.getalp.dbnary.StructuredGloss;
import org.getalp.iso639.ISO639_3.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Translation {
  private static final Logger log = LoggerFactory.getLogger(Translation.class);

  StructuredGloss gloss;
  Set<AcceptTranslation> isTranslationOf;
  Lang language;
  String writtenForm;
  String usage;

  public Translation(Lang language, Set<AcceptTranslation> isTranslationOf, String writtenForm,
      StructuredGloss gloss, String usage) {
    this.gloss = gloss;
    this.isTranslationOf = isTranslationOf;
    this.language = language;
    this.writtenForm = writtenForm;
    this.usage = usage;
  }

  public Translation(Lang language, Set<AcceptTranslation> isTranslationOf, String writtenForm,
      StructuredGloss gloss) {
    this(language, isTranslationOf, writtenForm, gloss, null);
  }

  public Translation(Lang language, Set<AcceptTranslation> isTranslationOf, String writtenForm) {
    this(language, isTranslationOf, writtenForm, null, null);
  }

  public Translation(Lang language, AcceptTranslation isTranslationOf, String writtenForm,
      StructuredGloss gloss, String usage) {
    this(language, new HashSet<>(List.of(isTranslationOf)), writtenForm, gloss, usage);
  }

  public Translation(Lang language, AcceptTranslation isTranslationOf, String writtenForm,
      StructuredGloss gloss) {
    this(language, isTranslationOf, writtenForm, gloss, null);
  }

  public Translation(Lang language, AcceptTranslation isTranslationOf, String writtenForm) {
    this(language, isTranslationOf, writtenForm, null, null);
  }

  public boolean addTranslationOf(AcceptTranslation entryOrPage) {
    return isTranslationOf.add(entryOrPage);
  }
}
