package com.yanivian.riddlr.frontend;

import com.yanivian.riddlr.backend.RiddlrBackendModule;
import com.yanivian.riddlr.service.ModuleCollector;
import com.yanivian.riddlr.service.ServletContextListener;

public final class RiddlrFrontend extends ServletContextListener {
  @Override
  protected void collectModules(ModuleCollector collector) {
    collector.add(new RiddlrBackendModule());
  }
}
