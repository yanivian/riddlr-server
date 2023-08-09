package com.yanivian.riddlr.service;

import com.google.inject.Module;
import java.util.ArrayList;
import java.util.List;

public final class ModuleCollector {
  private final List<Module> collectedModules = new ArrayList<>();

  public ModuleCollector add(Module... modules) {
    for (Module m : modules) {
      collectedModules.add(m);
    }
    return this;
  }

  List<Module> getCollectedModules() {
    return collectedModules;
  }
}
