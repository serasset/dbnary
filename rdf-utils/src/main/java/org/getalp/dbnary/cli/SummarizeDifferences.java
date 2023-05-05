package org.getalp.dbnary.cli;

import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedField;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedTitle;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
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
  protected final static String DISCORD_OPTION = "discord";
  protected final static String STDOUT_OPTION = "stdout";


  static {
    options
        .addOption(Option.builder()
            .longOpt(SLACK_OPTION).desc(
                "Display summary on Slack (using $SLACK_BOT_TOKEN and $SLACK_CHANNEL_ID environment variables).")
            .build())
        .addOption(Option.builder()
            .longOpt(DISCORD_OPTION).desc(
                "Display summary on Discord (using DISCORD_CHANNEL_WEBHOOK environment variable).")
            .build())
        .addOption(Option.builder().longOpt(STDOUT_OPTION).desc(
            "Display summary on stdout (default if neither slack nor discord specified).")
            .build());
  }

  boolean useSlack = false;
  boolean useDiscord = false;
  boolean useTerminal = true;
  String originalBranch = System.getenv("DBNARY_CICD_SOURCE_BRANCH");
  String destinationBranch = System.getenv("DBNARY_CICD_TARGET_BRANCH");

  public SummarizeDifferences(String[] args) {
    this.loadArgs(args);
  }

  @Override
  protected void loadArgs(CommandLine cmd) {
    useSlack = cmd.hasOption(SLACK_OPTION);
    useDiscord = cmd.hasOption(DISCORD_OPTION);
    useTerminal = cmd.hasOption(STDOUT_OPTION) || (!useSlack && !useDiscord);
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
      shoutOnSlack();
    }
    if (useDiscord) {
      shoutOnDiscord();
    }
  }

  private void shoutOnDiscord() {
    String channelUrl = System.getenv("DISCORD_CHANNEL_WEBHOOK");

    try (WebhookClient client = WebhookClient.withUrl(channelUrl)) {
      // Send and forget

      WebhookMessage message = createDiscordMessage();
      client.send(message)
          .thenAccept((msg) -> System.err.printf("Message with embed has been sent [%s]%n",
              msg.getId()));
    }
  }

  private WebhookMessage createDiscordMessage() {
    WebhookMessageBuilder builder = new WebhookMessageBuilder();
    builder.setContent(String.format(
        "**Extraction sample comparison between branch:%s and branch:%s**", originalBranch,
        destinationBranch));

    WebhookEmbedBuilder endolexEmbedBuilder =
        new WebhookEmbedBuilder().setColor(0x58b9ff)
            .setTitle(new EmbedTitle("Endolex (editions' languages) datasets", ""));
    WebhookEmbedBuilder exolexEmbedBuilder =
        new WebhookEmbedBuilder().setColor(0x8f07b1)
            .setTitle(new EmbedTitle("Exolex (foreign languages) datasets", ""));
    data.forEach((model, modelData) -> {
      if (model.startsWith("exolex")) {
        exolexEmbedBuilder.addField(
            new EmbedField(true, capitalize(model), modelData.toDiscordMarkdownString()));
      } else {
        endolexEmbedBuilder.addField(
            new EmbedField(true, capitalize(model), modelData.toDiscordMarkdownString()));
      }
    });
    return builder.addEmbeds(endolexEmbedBuilder.build(), exolexEmbedBuilder.build()).build();
  }

  private void shoutOnSlack() {
    try {
      Slack slack = Slack.getInstance();

      // If the token is a bot token, it starts with `xoxb-` while if it's a user token, it starts
      // with `xoxp-`
      String token = System.getenv("SLACK_BOT_TOKEN");
      String channelID = System.getenv("SLACK_CHANNEL_ID");

      // Initialize an API Methods client with the given token
      MethodsClient methods = slack.methods(token);

      // Build a request object
      ChatPostMessageRequest request = ChatPostMessageRequest.builder().channel(channelID)
          .text("I evaluated dbnary " + destinationBranch + " vs " + originalBranch)
          .blocks(createSlackMessage(originalBranch, destinationBranch)).build();

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
  }

  private List<LayoutBlock> createSlackMessage(String source, String target) {
    List<LayoutBlock> blocks = new ArrayList<>();
    SectionBlock mainBlock = section(section -> section.text(markdownText(
            "*Results of extraction sample evaluation*\n" + "Branches: " + target + " vs " + source))
        .fields(new ArrayList<>()));
    data.forEach((model, modelData) -> mainBlock.getFields()
        .add(markdownText(modelData.toSlackMarkdownString())));
    blocks.add(mainBlock);
    return blocks;
  }

  private String createConsoleMessage() {
    StringBuilder out = new StringBuilder();
    out.append("*Results of extraction sample evaluation*\n");
    data.forEach((model, modelData) -> out.append(modelData.toSlackMarkdownString()));
    return out.toString();
  }


  private static class Diff {

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

  private static class ModelData {

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

    public String toSlackMarkdownString() {
      StringBuilder s = new StringBuilder();
      s.append("*").append(capitalize(model)).append("*:\n");
      diffs.forEach((k, v) -> {
        s.append(">*").append(k).append("*: \t");
        long gainCount = v.getGainCount() - 1;
        long lossCount = v.getLossCount() - 1;
        long max = Math.max(gainCount, lossCount);
        if (max == -1) {
          s.append(":interrobang: ");
        } else if (max == 0) {
          s.append(":ok: ");
        } else if (max < 1000) {
          s.append(":vs: ");
        } else {
          s.append(":sos: ");
        }
        if (gainCount == lossCount) {
          s.append(":arrow_right: ");
        } else if (gainCount > lossCount) {
          s.append(":small_red_triangle: ");
        } else {
          s.append(":small_red_triangle_down: ");
        }
        s.append("\t +").append(gainCount).append("(").append(v.getGain()).append(") / -")
            .append(lossCount).append("(").append(v.getLoss()).append(")\n");
      });
      return s.toString();
    }

    public String toDiscordMarkdownString() {
      StringBuilder s = new StringBuilder();
      diffs.forEach((k, v) -> {
        s.append("**").append(capitalize(k)).append("**:  ");
        long gainCount = v.getGainCount() - 1;
        long lossCount = v.getLossCount() - 1;
        long max = Math.max(gainCount, lossCount);
        if (max == -1) {
          s.append("⁉ ");
        } else if (max == 0) {
          s.append(Character.toString(0x1F7E2)); // Green circle
        } else if (max < 1000) {
          s.append(Character.toString(0x1F7E0)); // orange circle
        } else {
          s.append(Character.toString(0x1F534)); // red circle
        }
        s.append(" ");
        if (gainCount == lossCount) {
          s.append("➡ ");
        } else if (gainCount > lossCount) {
          s.append("↗ ");
        } else {
          s.append("↘ ");
        }
        s.append("\t +").append(gainCount).append(" / -")
            .append(lossCount).append("\n");
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
    data.forEach((k, v) -> System.out.println(v.toSlackMarkdownString()));
  }

  // TODO: restructure the modules to create an aggregate one and make the RDF utils module depend
  // on extractor, then iterate over aggregation features (and take an arg to control them).

  Pattern fileNamePattern = Pattern.compile("^(..)_(gain|lost)_(.*)\\.ttl$");

  private long countStatements(Model m) {
    int count = 0;
    StmtIterator it = m.listStatements();
    while (it.hasNext()) {
      it.next();
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
      double rate = Double.NaN;
      Statement property = m.getProperty(RDFDiff.me, RDFDiff.diffRate);
      if (property != null) {
        Literal literal = property.getLiteral();
        if (literal != null) {
          rate = literal.getDouble();
        }
      }
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
