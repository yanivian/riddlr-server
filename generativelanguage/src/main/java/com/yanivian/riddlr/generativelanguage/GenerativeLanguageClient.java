package com.yanivian.riddlr.generativelanguage;

import com.google.ai.generativelanguage.v1beta2.GenerateTextRequest;
import com.google.ai.generativelanguage.v1beta2.GenerateTextResponse;
import com.google.ai.generativelanguage.v1beta2.TextPrompt;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.yanivian.riddlr.common.util.TextProtoUtils;
import com.yanivian.riddlr.generativelanguage.proto.RiddlesContainer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** A functional web client to the Generative Language REST API. */
public final class GenerativeLanguageClient {
  private final HttpRequestFactory httpRequestFactory;
  private final GenerateTextRequest baseGenerateTextRequest;

  @Inject
  GenerativeLanguageClient(
      HttpRequestFactory httpRequestFactory, GenerateTextRequest baseGenerateTextRequest) {
    this.httpRequestFactory = httpRequestFactory;
    this.baseGenerateTextRequest = baseGenerateTextRequest;
  }

  public Optional<RiddlesContainer> getRiddlesForTopic(
      String topic, int numRiddles, int numIncorrectAnswers) {
    String prompt =
        new StringBuilder()
            .append(String.format(
                "I want to quiz my friend on the topic of \"%s\". ", normalizeTopic(topic)))
            .append(String.format(
                "List %s short questions and their word or phrase answers, together with up to %s other answers that are incorrect but reasonable? ",
                numRiddles, numIncorrectAnswers))
            .append("Output in JSON with fields: question, correctAnswer, incorrectAnswers.")
            .toString();
    LOGGER.atInfo().log("Prompt: {}", prompt);

    try {
      HttpResponse httpResponse = createGenerateTextHttpRequest(prompt).execute();
      if (httpResponse.getStatusCode() != 200) {
        LOGGER.atError().log("Generation Failed: Status {} {}", httpResponse.getStatusCode(),
            httpResponse.getStatusMessage());
        return Optional.empty();
      }
      GenerateTextResponse response =
          TextProtoUtils.parseJson(httpResponse.parseAsString(), GenerateTextResponse.class);
      if (response.getCandidatesCount() == 0) {
        // No candidates.
        return Optional.of(RiddlesContainer.getDefaultInstance());
      }
      String generation = response.getCandidates(0).getOutput();
      LOGGER.atInfo().log("Generation: {}", generation);
      return parseRiddles(generation);
    } catch (IOException ioe) {
      LOGGER.atError().withThrowable(ioe).log("Web request failed.");
      return Optional.empty();
    }
  }

  private HttpRequest createGenerateTextHttpRequest(String prompt) throws IOException {
    URI uri;
    try {
      uri = new URIBuilder()
                .setScheme("https")
                .setHost("generativelanguage.googleapis.com")
                .setPath("/v1beta2/models/text-bison-001:generateText")
                .addParameter("key", API_KEY)
                .build();
    } catch (URISyntaxException urise) {
      throw new IllegalStateException(urise);
    }
    GenericUrl url = new GenericUrl(uri);
    String requestBody =
        TextProtoUtils.encodeToJsonString(baseGenerateTextRequest.toBuilder()
                                              .setPrompt(TextPrompt.newBuilder().setText(prompt))
                                              .build());
    HttpRequest request =
        httpRequestFactory.buildPostRequest(url, ByteArrayContent.fromString(null, requestBody));
    request.getHeaders().setContentType("application/json");
    return request;
  }

  private static Optional<RiddlesContainer> parseRiddles(String str) {
    if (str.startsWith("```json")) {
      // Strip out code start.
      str = str.substring(7).trim();
      LOGGER.atInfo().log("Stripped JSON code start.");
      if (str.endsWith("```")) {
        // Strip out code end.
        str = str.substring(0, str.length() - 3).trim();
        LOGGER.atInfo().log("Stripped JSON code end.");
      }
    }
    if (str.indexOf("\"\"") > 0) {
      str = str.replaceAll("[\"]+", "\"");
      LOGGER.atInfo().log("De-duped double quotes.");
    }
    if (str.indexOf("''") > 0) {
      str = str.replaceAll("[']+", "'");
      LOGGER.atInfo().log("De-duped single quotes.");
    }
    if (!str.endsWith("}]")) {
      int lastAnswerIdx = str.lastIndexOf("},");
      String ignored = str.substring(lastAnswerIdx);
      str = str.substring(0, lastAnswerIdx) + "}]";
      LOGGER.atInfo().log("Truncated answers, ignored: {}", ignored);
    }
    String riddlesContainerStr = String.format("{ \"riddles\": %s }", str);
    try {
      return Optional.of(TextProtoUtils.parseJson(riddlesContainerStr, RiddlesContainer.class));
    } catch (InvalidProtocolBufferException ipbe) {
      LOGGER.atError().withThrowable(ipbe).log("Failed to parse: {}", riddlesContainerStr);
      return Optional.empty();
    }
  }

  /** Strips out any non-alphanumeric characters and simplifies whitespaces from the topic. */
  private static String normalizeTopic(String topic) {
    return topic.replaceAll("[^a-zA-Z0-9\\s]+", "").replaceAll("[\\s]+", " ").trim();
  }

  private static final String API_KEY = "AIzaSyC6doGY1WaMe3-ORdpzs2DktDYB3YsThzA";
  private static final Logger LOGGER = LogManager.getLogger(GenerativeLanguageClient.class);
}
