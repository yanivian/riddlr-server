package com.yanivian.riddlr.backend;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.yanivian.riddlr.generativelanguage.GenerativeLanguageModule;
import java.time.Clock;

public final class RiddlrBackendModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new GenerativeLanguageModule());
  }

  @Provides
  @Singleton
  Clock provideClock() {
    return Clock.systemUTC();
  }

  @Provides
  @Singleton
  DatastoreService provideDatastoreService() {
    return DatastoreServiceFactory.getDatastoreService();
  }
}
