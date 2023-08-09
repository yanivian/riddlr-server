package com.yanivian.riddlr.backend;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.time.Clock;
import javax.inject.Singleton;

public final class RiddlrBackendModule extends AbstractModule {
  @Override
  protected void configure() {}

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
