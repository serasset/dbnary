package org.getalp.dbnary.cli;

import info.bliki.api.Connector;
import info.bliki.api.User;
import info.bliki.api.query.Parse;

public class TestInterwikiLinks {

  public TestInterwikiLinks() {
    super();
  }

  public static void testQueryLangLinks() {
    User user = new User("", "", "http://en.wiktionary.org/w/api.php");
    user.login();
    Connector connector = new Connector();
    Parse parse = Parse.create();
    parse.prop("iwlinks").page("flight").format("json");
    String rawXmlResponse = null; // connector.query(user, parse);
    if (rawXmlResponse == null) {
      System.out.println("Got no XML result for the query");
    }
    System.out.println(rawXmlResponse);

  }

  public static void main(String[] args) {
    testQueryLangLinks();
  }
}
