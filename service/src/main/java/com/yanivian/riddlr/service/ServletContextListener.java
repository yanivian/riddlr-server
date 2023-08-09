package com.yanivian.riddlr.service;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.servlet.GuiceServletContextListener;
import jakarta.servlet.ServletContextEvent;

/** Base class for the servlet context listener. */
public abstract class ServletContextListener extends GuiceServletContextListener {
  private final Supplier<Injector> injector = Suppliers.memoize(this::createInjector);

  @Override
  protected Injector getInjector() {
    return injector.get();
  }

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    servletContextEvent.getServletContext().setAttribute(
        Endpoint.GUICE_INJECTOR_ATTRIBUTE_NAME, injector.get());
    super.contextInitialized(servletContextEvent);
  }

  private Injector createInjector() {
    ModuleCollector moduleCollector = new ModuleCollector();
    collectModules(moduleCollector);
    return Guice.createInjector(Stage.PRODUCTION, moduleCollector.getCollectedModules());
  }

  /** Collects guice modules for the app. */
  protected abstract void collectModules(ModuleCollector collector);
}
