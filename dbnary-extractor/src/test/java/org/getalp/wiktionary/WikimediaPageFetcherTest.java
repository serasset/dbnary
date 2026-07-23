package org.getalp.wiktionary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Test;

/**
 * Exercises {@link WikimediaPageFetcher} against a local stub HTTP server so that tests never reach
 * the real Wikimedia API.
 */
public class WikimediaPageFetcherTest {

  private HttpServer server;

  @After
  public void stopServer() {
    if (null != server) {
      server.stop(0);
    }
  }

  private String startServer(String responseBody) throws IOException {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext("/w/api.php", exchange -> respond(exchange, 200, responseBody));
    server.start();
    return "http://localhost:" + server.getAddress().getPort() + "/w/api.php";
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  @Test
  public void fetchesExistingPageAsWrappedXml() throws IOException {
    String json =
        "{\"query\":{\"pages\":[{\"ns\":0,\"title\":\"Chat\",\"revisions\":[" + "{\"slots\":{\"main\":{\"content\":\"'''chat''' est un animal\"}}}]}]}}";
    String apiUrl = startServer(json);

    try (WikimediaPageFetcher fetcher = WikimediaPageFetcher.forTesting(apiUrl, 5, 2000L)) {
      String xml = fetcher.fetchPageXml("Chat");
      assertTrue(xml.contains("<title>Chat</title>"));
      assertTrue(xml.contains("'''chat''' est un animal"));
    }
  }

  @Test
  public void returnsNullForMissingPage() throws IOException {
    String json = "{\"query\":{\"pages\":[{\"ns\":0,\"title\":\"NoSuchPage\",\"missing\":true}]}}";
    String apiUrl = startServer(json);

    try (WikimediaPageFetcher fetcher = WikimediaPageFetcher.forTesting(apiUrl, 5, 2000L)) {
      assertNull(fetcher.fetchPageXml("NoSuchPage"));
    }
  }

  @Test
  public void escapesXmlSensitiveCharactersInContent() throws IOException {
    String json = "{\"query\":{\"pages\":[{\"ns\":0,\"title\":\"A & B\",\"revisions\":[" + "{\"slots\":{\"main\":{\"content\":\"1 < 2 & 3 > 1\"}}}]}]}}";
    String apiUrl = startServer(json);

    try (WikimediaPageFetcher fetcher = WikimediaPageFetcher.forTesting(apiUrl, 5, 2000L)) {
      String xml = fetcher.fetchPageXml("A & B");
      assertTrue(xml.contains("A &amp; B"));
      assertTrue(xml.contains("1 &lt; 2 &amp; 3 &gt; 1"));
    }
  }

  @Test
  public void throwsIOExceptionWhenServerIsUnreachable() throws IOException {
    // A port nothing is listening on (server created then immediately stopped).
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    int port = server.getAddress().getPort();
    server.start();
    server.stop(0);
    server = null;

    try (WikimediaPageFetcher fetcher = WikimediaPageFetcher.forTesting("http://localhost:" + port + "/w/api.php", 2, 20L)) {
      fetcher.fetchPageXml("Anything");
      fail("Expected an IOException since nothing is listening on the port");
    } catch (IOException expected) {
      // expected: unreachable server must never be reported as a confirmed-missing page.
    }
  }
}
