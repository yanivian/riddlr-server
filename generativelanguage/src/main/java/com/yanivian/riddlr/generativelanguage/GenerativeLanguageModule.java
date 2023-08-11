package com.yanivian.riddlr.generativelanguage;

import com.google.ai.generativelanguage.v1beta2.TextServiceClient;
import com.google.ai.generativelanguage.v1beta2.TextServiceSettings;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.rpc.FixedHeaderProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.IOException;

public final class GenerativeLanguageModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @Singleton
  TextServiceClient provideTextServiceClient() throws IOException {
    TransportChannelProvider provider =
        InstantiatingGrpcChannelProvider.newBuilder()
            .setHeaderProvider(FixedHeaderProvider.create(ImmutableMap.of(API_KEY_HEADER, API_KEY)))
            .build();

    TextServiceSettings settings =
        TextServiceSettings.newBuilder()
            .setTransportChannelProvider(provider)
            .setCredentialsProvider(FixedCredentialsProvider.create(null))
            .build();

    return TextServiceClient.create(settings);
  }

  private static final String API_KEY_HEADER = "x-goog-api-key";
  private static final String API_KEY = "AIzaSyC6doGY1WaMe3-ORdpzs2DktDYB3YsThzA";
}
