package org.getalp.dbnary.cli;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;

public class SummarizeDifferences extends VerboseCommand {
  protected final static String SLACK_OPTION = "slack";
  static {
    options.addOption(Option.builder().longOpt(SLACK_OPTION).desc(
        "Display summary on Slack (using $SLACK_BOT_TOKEN and $SLACK_CHANNEL_ID environment variables).")
        .build());
  }

  boolean useSlack = false;

  public SummarizeDifferences(String[] args) {
    this.loadArgs(args);
  }

  @Override
  protected void loadArgs(CommandLine cmd) {
    useSlack = cmd.hasOption(SLACK_OPTION);
    if (remainingArgs.length != 1) {
      printUsage();
      System.exit(1);
    }
  }

  @Override
  protected void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(
        "java -cp /path/to/dbnary.jar "
            + this.getClass().getCanonicalName() + " [OPTIONS] diffFolder",
        "With OPTIONS in:", options,
        "diffFolder should contains turtle files describing the differences computed. "
            + "Each file should be named <lg>_{gain,lost}_<model>.ttl "
            + "  - where <lg> is the 2 letter language code"
            + "  - and model is one of: ontolex, morphology, etymology, etc.",
        false);
  }

  public static void main(String[] args) {
    SummarizeDifferences cli = new SummarizeDifferences(args);
    cli.summarize();
    cli.publishSummary();
  }

  private void publishSummary() {
    if (useSlack) {
      try {
        Slack slack = Slack.getInstance();

        // If the token is a bot token, it starts with `xoxb-` while if it's a user token, it starts
        // with `xoxp-`
        String token = System.getenv("SLACK_BOT_TOKEN");
        String channelID = System.getenv("SLACK_CHANNEL_ID");

        String originalBranch = System.getenv("BITBUCKET_BRANCH");
        String destinationBranch = System.getenv("BITBUCKET_PR_DESTINATION_BRANCH");

        // Initialize an API Methods client with the given token
        MethodsClient methods = slack.methods(token);

        // Build a request object
        ChatPostMessageRequest request = ChatPostMessageRequest.builder().channel(channelID)
            .text("I evaluated dbnary " + destinationBranch + " vs " + originalBranch)
            .blocks(createSlackMessage()).build();

        // Get a response as a Java object
        System.err.println("Posting message : " + request);
        ChatPostMessageResponse response = methods.chatPostMessage(request);
        if (!response.isOk()) {
          System.err.println("Error received from Slack API.");
          System.err.println(response.getError());
        }

      } catch (SlackApiException e) {
        System.err.println("Slack API exception.");
        System.err.println(e.getLocalizedMessage());
        e.printStackTrace();
      } catch (IOException e) {
        System.err.println("IOException while accessing Slack API.");
        System.err.println(e.getLocalizedMessage());
        e.printStackTrace();
      }
    } else {
      System.out.println(createConsoleMessage());
    }
  }

  private List<LayoutBlock> createSlackMessage() {
    List<LayoutBlock> blocks = new ArrayList<>();
    SectionBlock mainBlock = section(section -> section
        .text(markdownText("*Results of extraction sample evaluation*")).fields(new ArrayList<>()));
    data.forEach((model, modelData) -> mainBlock.getFields()
        .add(markdownText(modelData.toMarkdownString())));
    blocks.add(mainBlock);
    return blocks;
  }

  private String createConsoleMessage() {
    StringBuilder out = new StringBuilder();
    out.append("*Results of extraction sample evaluation*\n");
    data.forEach((model, modelData) -> out.append(modelData.toMarkdownString()));
    return out.toString();
  }


  private class Diff {
    double gain;
    double loss;
    long gainCount;
    long lossCount;

    public double getGain() {
      return gain;
    }

    public long getGainCount() {
      return gainCount;
    }

    public void setGain(double gain, long count) {
      this.gain = gain;
      this.gainCount = count;
    }

    public double getLoss() {
      return loss;
    }

    public long getLossCount() {
      return lossCount;
    }

    public void setLoss(double loss, long count) {
      this.loss = loss;
      this.lossCount = count;
    }
  }

  private class ModelData {
    String model;
    Map<String, Diff> diffs = new TreeMap<>();

    public ModelData(String model) {
      this.model = model;
    }

    public void setDiff(String language, Diff diff) {
      diffs.put(language, diff);
    }

    public Optional<Diff> getDiff(String language) {
      return Optional.ofNullable(diffs.get(language));
    }

    public String toMarkdownString() {
      StringBuilder s = new StringBuilder();
      s.append("*").append(capitalize(model)).append("*:\n");
      diffs.forEach((k, v) -> {
        s.append(">*").append(k).append("*: \t");
        long max = Math.max(v.getGainCount(), v.getLossCount());
        if (max == 0) {
          s.append(":ok: ");
        } else if (max < 1000) {
          s.append(":ng: ");
        } else {
          s.append(":sos: ");
        }
        if (v.getGainCount() == v.getLossCount()) {
          s.append(":arrow_right: ");
        } else if (v.getGainCount() > v.getLossCount()) {
          s.append(":small_red_triangle: ");
        } else {
          s.append(":small_red_triangle_down: ");
        }
        s.append("\t +").append(v.getGainCount()).append("(").append(v.getGain()).append(") / -")
            .append(v.getLossCount()).append("(").append(v.getLoss()).append(")\n");
      });
      return s.toString();
    }
  }

  private static String capitalize(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }

  Map<String, ModelData> data = new TreeMap<>();

  private void summarize() {
    try (Stream<Path> stream = Files.list(Paths.get(remainingArgs[0]))) {
      stream.map(String::valueOf).filter(path -> path.endsWith(".ttl")).forEach(this::analyseDiff);
    } catch (IOException e) {
      e.printStackTrace();
    }
    data.forEach((k, v) -> System.out.println(v.toMarkdownString()));
  }

  // TODO: restructure the modules to create an aggregate one and make the RDF utils module depend
  // on extractor, then iterate over aggregation features (and take an arg to control them).

  Pattern fileNamePattern = Pattern.compile("(..)_(gain|lost)_([^_.]*).ttl");

  private long countStatements(Model m) {
    int count = 0;
    StmtIterator it = m.listStatements();
    while (it.hasNext()) {
      Statement current = it.next();
      if (!RDFDiff.diffRate.getLocalName().equals(current.getPredicate().getLocalName()))
        count++;
    }
    return count;
  }

  private void analyseDiff(String s) {
    Path p = Paths.get(s);
    Path basename = p.getFileName();

    Matcher fileNameMatcher = fileNamePattern.matcher(basename.toString());
    if (fileNameMatcher.matches()) {
      Model m = RDFDataMgr.loadModel(s);
      String lg = fileNameMatcher.group(1);
      String direction = fileNameMatcher.group(2);
      String model = fileNameMatcher.group(3);
      double rate = Optional.ofNullable(m.getProperty(RDFDiff.me, RDFDiff.diffRate))
          .map(Statement::getLiteral).map(Literal::getDouble).orElse(Double.NaN);
      long nbStatements = countStatements(m);

      // System.err.println("" + lg + "/" + direction + "/" + model + " -> " + rate);
      ModelData md = data.getOrDefault(model, new ModelData(model));
      data.put(model, md);
      Diff d = md.getDiff(lg).orElse(new Diff());
      md.setDiff(lg, d);
      if ("gain".equals(direction)) {
        d.setGain(rate, nbStatements);
      } else {
        d.setLoss(rate, nbStatements);
      }
    }
  }

}
