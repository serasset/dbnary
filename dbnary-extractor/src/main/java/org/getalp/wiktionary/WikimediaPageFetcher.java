package org.getalp.wiktionary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches the current wikitext of a single page from the live MediaWiki action API, as a fallback
 * for titles missing from a downloaded dump (a known limitation of MediaWiki's XML export).
 *
 * <p>
 * Follows Wikimedia's bot etiquette: a descriptive User-Agent, the {@code maxlag} parameter on
 * every request, and exponential backoff on transient failures. Only ever issues one request at a
 * time -- callers must not invoke this concurrently.
 * </p>
 *
 * @see <a href="https://www.mediawiki.org/wiki/API:Etiquette">API:Etiquette</a>
 */
class WikimediaPageFetcher implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(WikimediaPageFetcher.class);

  private static final String USER_AGENT = "DBnary/1.0 (https://kaiko.getalp.org/about-dbnary gilles.serasset@imag.fr)";
  private static final int DEFAULT_MAX_ATTEMPTS = 5;
  private static final long DEFAULT_INITIAL_BACKOFF_MILLIS = 2000L;

  private final ObjectMapper mapper = new ObjectMapper();
  private final CloseableHttpClient client;
  private final String apiUrl;
  private final int maxAttempts;
  private final long initialBackoffMillis;

  WikimediaPageFetcher(String shortLangCode) {
    this("https://" + shortLangCode + ".wiktionary.org/w/api.php", DEFAULT_MAX_ATTEMPTS, DEFAULT_INITIAL_BACKOFF_MILLIS);
  }

  // Visible for testing: lets tests point this at a local stub server instead of the real
  // Wikimedia API, and shrink the retry/backoff timings so failure tests stay fast.
  static WikimediaPageFetcher forTesting(String apiUrl, int maxAttempts, long initialBackoffMillis) {
    return new WikimediaPageFetcher(apiUrl, maxAttempts, initialBackoffMillis);
  }

  private WikimediaPageFetcher(String apiUrl, int maxAttempts, long initialBackoffMillis) {
    this.apiUrl = apiUrl;
    this.maxAttempts = maxAttempts;
    this.initialBackoffMillis = initialBackoffMillis;
    this.client = HttpClientBuilder.create().setUserAgent(USER_AGENT).build();
  }

  /**
   * @return a minimal synthetic {@code <page>} XML fragment wrapping the page's current wikitext
   *         (shaped so that {@link WiktionaryIndexer#getTextElementContent(String)} can consume it
   *         exactly like dump-sourced content), or {@code null} if MediaWiki confirms the title does
   *         not exist.
   * @throws IOException if the page's status could not be determined (network error, or the server
   *         remained lagged/unavailable after retries). This is NOT evidence that the page is missing
   *         and callers must not cache it as such.
   */
  String fetchPageXml(String title) throws IOException {
    long backoff = initialBackoffMillis;
    IOException lastError = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        JsonNode page = queryPage(title);
        if (page.has("missing") || page.has("invalid")) {
          return null;
        }
        JsonNode contentNode = page.at("/revisions/0/slots/main/content");
        if (contentNode.isMissingNode()) {
          throw new IOException("Unexpected API response shape for title '" + title + "': " + page);
        }
        return wrapAsPageXml(title, contentNode.asText());
      } catch (IOException e) {
        log.warn("Could not fetch '{}' from the live MediaWiki API (attempt {}/{}): {}", title, attempt, maxAttempts, e.getLocalizedMessage());
        lastError = e;
      }
      sleep(backoff);
      backoff *= 2;
    }
    throw new IOException("Could not fetch '" + title + "' from the live MediaWiki API after " + maxAttempts + " attempts", lastError);
  }

  private JsonNode queryPage(String title) throws IOException {
    HttpGet request;
    try {
      URIBuilder uriBuilder = new URIBuilder(apiUrl).addParameter("action", "query").addParameter("prop", "revisions").addParameter("rvprop", "content")
          .addParameter("rvslots", "main").addParameter("titles", title).addParameter("format", "json").addParameter("formatversion", "2")
          .addParameter("maxlag", "5");
      request = new HttpGet(uriBuilder.build());
    } catch (URISyntaxException e) {
      throw new IOException("Could not build API request URI for title '" + title + "'", e);
    }
    try (CloseableHttpResponse response = client.execute(request)) {
      HttpEntity entity = response.getEntity();
      if (null == entity) {
        throw new IOException("Empty response body while querying the live MediaWiki API for '" + title + "'");
      }
      String body = EntityUtils.toString(entity, StandardCharsets.UTF_8);
      JsonNode root = mapper.readTree(body);
      JsonNode error = root.get("error");
      if (null != error && "maxlag".equals(error.path("code").asText())) {
        throw new IOException("MediaWiki API reported maxlag: " + error.path("info").asText("maxlag"));
      }
      JsonNode pages = root.at("/query/pages");
      if (!pages.isArray() || pages.isEmpty()) {
        throw new IOException("No page data in API response for '" + title + "': " + body);
      }
      return pages.get(0);
    }
  }

  private static String wrapAsPageXml(String title, String wikitext) {
    return "<page><title>" + escapeXml(title) + "</title><revision><text xml:space=\"preserve\">" + escapeXml(wikitext) + "</text></revision></page>";
  }

  private static String escapeXml(String s) {
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void close() throws IOException {
    client.close();
  }
}
