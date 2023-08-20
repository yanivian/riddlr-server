package com.yanivian.riddlr.generativelanguage;

import com.google.ai.generativelanguage.v1beta2.GenerateTextRequest;
import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public final class GenerativeLanguageModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @Singleton
  HttpTransport provideHttpTransport() {
    // Only for App Engine.
    return UrlFetchTransport.getDefaultInstance();
  }

  @Provides
  HttpRequestFactory provideHttpRequestFactory(HttpTransport httpTransport) {
    return httpTransport.createRequestFactory();
  }

  @Provides
  GenerateTextRequest provideGenerateTextRequest() {
    return GenerateTextRequest.newBuilder()
        .setTemperature(.25f)
        .setCandidateCount(1)
        .setMaxOutputTokens(4096)
        .setTopK(100)
        .setTopP(.95f)
        .build();
  }
}
