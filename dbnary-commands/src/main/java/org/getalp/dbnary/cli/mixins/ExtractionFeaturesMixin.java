package org.getalp.dbnary.cli.mixins;

import java.util.HashSet;
import java.util.Set;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.cli.utils.RDFFormats;
import org.getalp.dbnary.model.DbnaryModel;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

public class ExtractionFeaturesMixin {

  @Spec(Spec.Target.MIXEE)
  CommandSpec mixee;

  private static final String DEFAULT_OUTPUT_FORMAT = "Turtle";
  private String outputFormat;

  private Set<ExtractionFeature> endolexFeatures = new HashSet<>();;

  @Option(names = {"--endolex"}, split = ",", defaultValue = "ontolex",
      description = "Enable the specified features for the endolex extraction "
          + "(valid features : ${COMPLETION-CANDIDATES}).")
  private void setEndolexFeatures(Set<ExtractionFeature> features) {
    if (null == endolexFeatures)
      endolexFeatures = new HashSet<>();
    endolexFeatures.addAll(features);
    endolexFeatures.add(ExtractionFeature.MAIN);
  }

  private Set<ExtractionFeature> exolexFeatures = new HashSet<>();

  @Option(names = {"--exolex"}, split = ",",
      description = "Enable the specified features for the exolex (foreign entries) extraction "
          + "(valid features : ${COMPLETION-CANDIDATES}).")
  private void setExolexFeatures(Set<ExtractionFeature> features) {
    if (null == exolexFeatures)
      exolexFeatures = new HashSet<>();
    exolexFeatures.addAll(features);
    exolexFeatures.add(ExtractionFeature.MAIN);
  }

  @Option(names = {"-f", "--format"}, paramLabel = "OUTPUT-FORMAT",
      defaultValue = DEFAULT_OUTPUT_FORMAT, completionCandidates = RDFFormats.class,
      description = "format used for all models (${COMPLETION-CANDIDATES}); "
          + "Default: ${DEFAULT-VALUE}.")
  private void setOutputFormat(String format) {
    this.outputFormat = format;
    if (!RDFFormats.getKnownFormats().contains(outputFormat)) {
      throw new ParameterException(mixee.commandLine(),
          String.format("Invalid format '%s' for option '--format': unknown format.", format));
    }
  }

  @Option(names = {"-p", "--prefix"}, paramLabel = "DBNARY-URI-PREFIX",
      description = "Use the specified prefix for all URIs. (use with care).")
  private void setDBnaryURIPrefix(String prefix) {
    DbnaryModel.setGlobalDbnaryPrefix(prefix);
  }

  public Set<ExtractionFeature> getEndolexFeatures() {
    return endolexFeatures;
  }

  public Set<ExtractionFeature> getExolexFeatures() {
    return exolexFeatures;
  }

  public String getOutputFormat() {
    return outputFormat;
  }
}
