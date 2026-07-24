package org.getalp.wiktionary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MissingPageCacheTest {

  private Path cacheFile;

  @Before
  public void createCacheFile() throws IOException {
    Path dir = Files.createTempDirectory("missing-page-cache-test");
    cacheFile = dir.resolve("dump.xml.apicache.json");
  }

  @After
  public void deleteCacheFile() throws IOException {
    Files.deleteIfExists(cacheFile);
    Files.deleteIfExists(cacheFile.getParent());
  }

  @Test
  public void unknownTitleIsNotPresent() {
    MissingPageCache cache = new MissingPageCache(cacheFile);
    assertFalse(cache.has("Some title"));
  }

  @Test
  public void foundPageIsPersistedAndReloaded() {
    MissingPageCache cache = new MissingPageCache(cacheFile);
    cache.put("Found title", "<page>...</page>");

    MissingPageCache reloaded = new MissingPageCache(cacheFile);
    assertTrue(reloaded.has("Found title"));
    assertEquals("<page>...</page>", reloaded.get("Found title"));
  }

  @Test
  public void confirmedMissingPageIsPersistedAsNullAndReloaded() {
    MissingPageCache cache = new MissingPageCache(cacheFile);
    cache.put("Missing title", null);

    MissingPageCache reloaded = new MissingPageCache(cacheFile);
    assertTrue(reloaded.has("Missing title"));
    assertEquals(null, reloaded.get("Missing title"));
  }
}
