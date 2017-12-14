package org.getalp.dbnary.bliki;

import info.bliki.extensions.scribunto.engine.lua.CompiledScriptCache;
import info.bliki.extensions.scribunto.engine.lua.ScribuntoLuaEngine;
import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.IWikiModel;
import info.bliki.wiki.namespaces.INamespace;

public class DbnaryScribuntoLuaEngine extends ScribuntoLuaEngine {

  public DbnaryScribuntoLuaEngine(IWikiModel model, CompiledScriptCache cache) {
    super(model, cache);
  }

  public DbnaryScribuntoLuaEngine(IWikiModel model, CompiledScriptCache cache, boolean debug) {
    super(model, cache, debug);
  }

  // WORKAROUND bliki issue #49: bug in ScribuntoEngineBase when a module is called using the non primary
  // (localized) Module name
  // TODO: remove this patch when issue will be corrected in bliki.
  @Override
  protected ParsedPageName pageNameForModule(String moduleName,
      INamespace.INamespaceValue fallback) {
    if (startsWithModuleNamespace(moduleName)) {
      return ParsedPageName.parsePageName(model, moduleName, moduleNamespace, false, false);
    } else {
      return new ParsedPageName(fallback, moduleName, true);
    }
  }

  private boolean startsWithModuleNamespace(String moduleName) {
    for (String n : moduleNamespace.getTexts()) {
      if (moduleName.toLowerCase().startsWith(n.toLowerCase() + ":")) {
        return true;
      }
    }
    return false;
  }

}
