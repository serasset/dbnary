package org.getalp.wiktionary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persists the outcome of live MediaWiki API lookups for titles missing from a dump, so that a
 * given title is never queried more than once, across runs.
 *
 * <p>
 * A key present with a {@code null} value records a title confirmed not to exist on the live wiki;
 * a key present with a non null value records the (already dump-shaped) content that was fetched
 * for it. A key that is absent has simply never been looked up yet.
 * </p>
 */
class MissingPageCache {
  private static final Logger log = LoggerFactory.getLogger(MissingPageCache.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  private final Path cacheFile;
  private final Map<String, String> entries;

  MissingPageCache(Path cacheFile) {
    this.cacheFile = cacheFile;
    this.entries = load(cacheFile);
  }

  private static Map<String, String> load(Path cacheFile) {
    if (Files.isReadable(cacheFile)) {
      try {
        return new HashMap<>(mapper.readValue(cacheFile.toFile(), new TypeReference<Map<String, String>>() {}));
      } catch (IOException e) {
        log.warn("Could not read live-API cache file {}; starting with an empty cache: {}", cacheFile, e.getLocalizedMessage());
      }
    }
    return new HashMap<>();
  }

  boolean has(String key) {
    return entries.containsKey(key);
  }

  String get(String key) {
    return entries.get(key);
  }

  /**
   * Records the outcome for {@code key} and immediately persists it to disk (write-through), so that
   * an already-paid-for API call is never lost or repeated, even if the process is interrupted before
   * the index is closed.
   */
  void put(String key, String value) {
    entries.put(key, value);
    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(cacheFile.toFile(), entries);
    } catch (IOException e) {
      log.warn("Could not persist live-API cache file {}: {}", cacheFile, e.getLocalizedMessage());
    }
  }
}
