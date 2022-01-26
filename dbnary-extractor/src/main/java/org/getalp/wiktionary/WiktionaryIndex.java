package org.getalp.wiktionary;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.getalp.dbnary.OffsetValue;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WiktionaryIndex is a Persistent HashMap designed to hold an index on a Wiktionary dump file.
 *
 * @author serasset
 */
public class WiktionaryIndex implements WiktionaryPageSource {
  private static final Logger log = LoggerFactory.getLogger(WiktionaryIndex.class);

  private static final CacheManager cacheManager = CacheManager.newInstance();
  private static final Ehcache cache = cacheManager.getEhcache("wiktcache");

  // TODO: Create a static map to hold shared instances (1 per dump file) and avoid allocating more
  // than one WiktionaryIndexer per wiktionary language.

  private static final double AVERAGE_PAGE_SIZE = 730.; // figure taken from French Wiktionary
  private static final String INDEX_SIGNATURE = "Wkt!01";

  Path dump;
  Path index;
  Charset encoding;
  HashMap<String, OffsetValue> map;
  SeekableByteChannel xmlf;

  /**
   * returns Path of the index file corresponding to passed dump.
   * @param dump Path refering to the dump file.
   * @return the Path refering to the index file.
   */
  public static Path indexFile(Path dump) {
    return dump.resolveSibling(dump.getFileName() + ".idx");
  }

  /**
   * Creates a WiktionaryIndex for the wiktionary dump whose path is passed as a parameter
   *
   * @param dump the path to file containing the wiktionary dump.
   * @throws WiktionaryIndexerException thrown if any error occur during index initialization
   */
  public WiktionaryIndex(Path dump) throws WiktionaryIndexerException {
    this.dump = dump;
    index = indexFile(dump);
    if (this.isAValidIndexFile()) {
      this.loadIndex();
    } else {
      this.initIndex();
    }
    try {
      xmlf = FileChannel.open(dump, READ);
      // xmlf = new RandomAccessFile(dumpFile, "r");
      // Read the BOM at the start of the file to determine the correct encoding.
      ByteBuffer bom = ByteBuffer.allocate(2);
      xmlf.read(bom);
      bom.flip();
      byte b1 = bom.get();
      byte b2 = bom.get();
      if (b1 == (byte) 0xFE && b2 == (byte) 0xFF) {
        // Big Endian
        encoding = StandardCharsets.UTF_16BE;
      } else if (b1 == (byte) 0xFF && b2 == (byte) 0xFE) {
        // Little endian
        encoding = StandardCharsets.UTF_16LE;
      } else {
        // no BOM, use UTF-16
        encoding = StandardCharsets.UTF_16;
      }
    } catch (IOException ex) {
      throw new WiktionaryIndexerException(
          "Could not open wiktionary dump file " + dump, ex);
    }
  }

  /**
   * Free resource taken by the index.
   */
  public void close() {
    try {
      if (null != xmlf)
        xmlf.close();
      if (null != map)
        map.clear();
    } catch (IOException ignored) {

    }
  }

  // WONTDO: check if index content is up to date ?
  private boolean isAValidIndexFile() {
    if (Files.isReadable(index)) {
      try (FileChannel fc = FileChannel.open(index, READ)) {
        ByteBuffer buf = ByteBuffer.allocate(4098);

        fc.read(buf);
        buf.flip();

        byte[] signature = INDEX_SIGNATURE.getBytes(StandardCharsets.UTF_8);
        buf.get(signature, 0, signature.length);
        String signatureString = new String(signature, StandardCharsets.UTF_8);
        if (signatureString.equals(INDEX_SIGNATURE)) {
          return true;
        }
      } catch (IOException e) {
        return false;
      }
    }
    return false;
  }

  private void dumpIndex() throws WiktionaryIndexerException {
    try (FileChannel fc = FileChannel.open(index, READ, WRITE)) {

      ByteBuffer buf = ByteBuffer.allocate(4098);

      // Write index signature out.write(...)
      buf.put(INDEX_SIGNATURE.getBytes(StandardCharsets.UTF_8));
      buf.putInt(map.size());
      for (Map.Entry<String, OffsetValue> entry : map.entrySet()) {
        byte[] bk = entry.getKey().getBytes(StandardCharsets.UTF_8);
        OffsetValue v = entry.getValue();
        // I serialize 1 int, the string, 1 long and 1 int --> bk.length + 16 bytes
        // If there is not enough room left in the buffer, first write it out, then proceed
        if (buf.remaining() < bk.length + 16) {
          buf.flip();
          fc.write(buf);
          buf.clear();
        }

        buf.putInt(bk.length);
        buf.put(bk);
        buf.putLong(v.start);
        buf.putInt(v.length);
      }
      buf.flip();
      fc.write(buf);
    } catch (IOException e) {
      throw new WiktionaryIndexerException("IOException when writing map to index file", e);
    }
  }

  /**
   * Load the index from the VALID index file (validity and readability should be checked before
   * call to this method)
   * @throws WiktionaryIndexerException if any exception arise during index load
   */
  private void loadIndex() throws WiktionaryIndexerException {
    try (FileChannel fc = FileChannel.open(index, READ)) {

      ByteBuffer buf = ByteBuffer.allocate(4098);

      fc.read(buf);
      buf.flip();
      byte[] signature = INDEX_SIGNATURE.getBytes(StandardCharsets.UTF_8);
      buf.get(signature, 0, signature.length);

      int mapSize = buf.getInt();

      map = new HashMap<>((int) (mapSize / .75));
      byte[] bk = new byte[2048]; // We assume that no entry title is longer than 2048

      for (int i = 0; i < mapSize; i++) {
        // First read the next entry size
        if (buf.remaining() < 4) { // 4 bytes = 1 int...
          readNextChunk(fc, buf);
        }
        int kSize = buf.getInt();
        // Check if the whole entry is already in the buffer, else advance buffer to fit whole entry
        if (buf.remaining() < kSize + 16) { // kSize byte + 2 ints...
          readNextChunk(fc, buf);
        }
        // read the entry
        buf.get(bk, 0, kSize);
        String key = new String(bk, 0, kSize, StandardCharsets.UTF_8);
        long vstart = buf.getLong();
        int vlength = buf.getInt();
        map.put(key, new OffsetValue(vstart, vlength));
      }

    } catch (IOException e) {
      throw new WiktionaryIndexerException("IOException when reading map from index file", e);
    }
  }

  private void readNextChunk(FileChannel fc, ByteBuffer buf) throws IOException {
    byte[] ba = new byte[buf.remaining()];
    buf.get(ba);
    buf.clear();
    buf.put(ba);
    fc.read(buf);
    buf.flip();
  }

  private void initIndex() throws WiktionaryIndexerException {
    int initialCapacity;
    try {
      initialCapacity = (int) ((Files.size(dump) / AVERAGE_PAGE_SIZE) / .75);
    } catch (IOException e) {
      throw new WiktionaryIndexerException(e);
    }
    map = new HashMap<>(initialCapacity);
    WiktionaryIndexer.createIndex(dump, map);
    long starttime = System.currentTimeMillis();
    log.info("Dumping index...");
    this.dumpIndex();
    long endtime = System.currentTimeMillis();
    log.info(" Dumping index Time = {}; ", (endtime - starttime));
  }

  public String get(String key) {
    OffsetValue ofs = map.get(key);
    if (ofs == null) {
      return null;
    }
    String res;
    try {
      xmlf.position(ofs.start * 2 + 2); // in utf-16, 2 first bytes for the BOM
      ByteBuffer b = ByteBuffer.allocate(ofs.length * 2);

      //byte[] b = new byte[ofs.length * 2];
      xmlf.read(b);
      res = encoding.decode(b).toString();
    } catch (IOException ex) {
      res = null;
    }
    return res;
  }

  private static final List<String> redirects =
      Arrays.asList("#REDIRECT", "#WEITERLEITUNG", "#REDIRECCIÓN");

  @Override
  public String getTextOfPageWithRedirects(String key) {
    String text = getTextOfPage(key);
    if (null == text) {
      text = getTextOfPage(key.replace(" ", "_"));
    }
    if (null == text) {
      text = getTextOfPage(key.replace("_", " "));
    }
    if (null != text) {
      for (String redirect : redirects) {
        if (text.startsWith(redirect)) {
          String targetLink = text.substring(redirect.length()).trim();
          Matcher linkMatcher = WikiPatterns.linkPattern.matcher(targetLink);
          if (linkMatcher.matches()) {
            return getTextOfPageWithRedirects(linkMatcher.group(1));
          }
        }
      }
    }
    return text;
  }

  @Override
  public String getTextOfPage(String key) {
    boolean notMainSpace = key.contains(":");
    Element element = cache.get(key);
    if (element != null && notMainSpace) {
      return (String) element.getObjectValue();
    }
    String res = WiktionaryIndexer.getTextElementContent(this.get(key));
    if (res != null && notMainSpace) {
      element = new Element(key, res);
      cache.put(element);
    }
    return res;
  }

  @Override
  public String getFullXmlForPage(String key) {
    return this.get(key);
  }
}
