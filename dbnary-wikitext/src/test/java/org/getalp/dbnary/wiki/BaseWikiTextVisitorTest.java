package org.getalp.dbnary.wiki;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.getalp.dbnary.wiki.WikiText.InternalLink;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.visit.BaseWikiTextVisitor;
import org.junit.Test;

public class BaseWikiTextVisitorTest extends CommonWikiTextLoader {

  private static class InternalLinksVisitor extends BaseWikiTextVisitor {
    private final Set<String> expectedLinkTargets;

    public InternalLinksVisitor(Set<String> expectedLinkTargets) {
      this.expectedLinkTargets = expectedLinkTargets;
    }

    @Override
    public Void visit(InternalLink internalLink) {
      assertThat("Unexpected Header.",
          internalLink.getFullTargetText(), is(in(expectedLinkTargets)));
      expectedLinkTargets.remove(internalLink.getFullTargetText());
      super.visit(internalLink);
      return null;
    }

  }

  private static class TemplatesVisitor extends BaseWikiTextVisitor {
    private final Collection<String> expectedTemplates;
    private final ArrayList<Object> visitedTemplates;

    public TemplatesVisitor(Collection<String> expectedTemplates) {
      this.expectedTemplates = expectedTemplates;
      this.visitedTemplates = new ArrayList<>();
    }

    public ArrayList<Object> getVisitedTemplates() {
      return visitedTemplates;
    }

    @Override
    public Void visit(Template template) {
      assertThat("Unexpected Header.",
          template.getName(), is(in(expectedTemplates)));
      visitedTemplates.add(template.getName());
      super.visit(template);
      return null;
    }

  }

  @Test
  public void testBleu() throws IOException {
    WikiText text = getWikiTextFor("bleu_extract_definitions_fr");

    Set<String> expectedTargets = new HashSet<>();
    expectedTargets.add("couleur");
    expectedTargets.add("ciel");
    expectedTargets.add("dégager");
    expectedTargets.add("champ chromatique");
    expectedTargets.add("#fr-nom-1");
    InternalLinksVisitor visitor = new InternalLinksVisitor(expectedTargets);
    visitor.visit(text.content());

    Set<String> expectedTemplates = new HashSet<>();
    expectedTemplates.add("fr-accord-rég");
    expectedTemplates.add("pron");
    expectedTemplates.add("couleur");
    expectedTemplates.add("source");
    expectedTemplates.add("w");
    expectedTemplates.add("ws");
    expectedTemplates.add("par extension");
    expectedTemplates.add("Citation/H. G. Wells/La Guerre dans les airs/1921");
    expectedTemplates.add("nom w pc");
    TemplatesVisitor tVisitor = new TemplatesVisitor(expectedTemplates);
    tVisitor.visit(text.content());
    for (String t : expectedTemplates) {
      assertTrue(t + " should have been visited.", tVisitor.getVisitedTemplates().contains(t));
    }
    assertThat(new HashSet<>(tVisitor.getVisitedTemplates()).size(), is(9));
    assertThat(tVisitor.getVisitedTemplates().size(), is(27));

  }
}
