package com.yanivian.riddlr.generativelanguage;

import com.google.ai.generativelanguage.v1beta2.GenerateTextRequest;
import com.google.ai.generativelanguage.v1beta2.GenerateTextResponse;
import com.google.ai.generativelanguage.v1beta2.TextPrompt;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.yanivian.riddlr.common.util.TextProtoUtils;
import com.yanivian.riddlr.generativelanguage.proto.Riddle;
import com.yanivian.riddlr.generativelanguage.proto.RiddlesContainer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.utils.URIBuilder;

/** A functional web client to the Generative Language REST API. */
public final class GenerativeLanguageClient {
  private final HttpRequestFactory httpRequestFactory;
  private final GenerateTextRequest baseGenerateTextRequest;

  @Inject
  GenerativeLanguageClient(
      HttpRequestFactory httpRequestFactory, GenerateTextRequest baseGenerateTextRequest) {
    this.httpRequestFactory = httpRequestFactory;
    this.baseGenerateTextRequest = baseGenerateTextRequest.toBuilder()
                                       .setTemperature(.1f)
                                       .setCandidateCount(4)
                                       .setTopK(40)
                                       .setTopP(.95f)
                                       .build();
  }

  public Optional<RiddlesContainer> getRiddlesForTopic(
      String topic, int numRiddles, int numIncorrectAnswers) {
    LOGGER.log(Level.INFO, "Topic: {0}", topic);
    String prompt =
        new StringBuilder()
            .append(String.format(
                "I want to quiz my friend on the topic of \"%s\". ", normalizeTopic(topic)))
            .append(String.format(
                "Can you list %s short questions and their word or phrase answers, together with up to %s other answers that are incorrect but reasonable? ",
                numRiddles, numIncorrectAnswers))
            .append(
                "Output in JSON with fields: question, correctAnswer, incorrectAnswers, explanation and citationURL.")
            .toString();
    LOGGER.log(Level.FINE, "Prompt: {0}", prompt);

    try {
      HttpResponse httpResponse = createGenerateTextHttpRequest(prompt).execute();
      if (httpResponse.getStatusCode() != 200) {
        LOGGER.log(Level.SEVERE, "Generation Failed: Status {0} {1}",
            new Object[] {httpResponse.getStatusCode(), httpResponse.getStatusMessage()});
        return Optional.empty();
      }
      GenerateTextResponse response =
          TextProtoUtils.parseJson(httpResponse.parseAsString(), GenerateTextResponse.class);
      if (response.getCandidatesCount() == 0) {
        // No candidates.
        return Optional.of(RiddlesContainer.getDefaultInstance());
      }
      ImmutableList<Riddle> riddles =
          response.getCandidatesList()
              .stream()
              .map(candidate -> parseRiddles(candidate.getOutput()))
              .filter(Optional::isPresent)
              .flatMap(optional -> optional.get().getRiddlesList().stream())
              .collect(ImmutableList.toImmutableList());
      Set<String> uniqueQuestions = new HashSet<>();
      Set<String> correctAnswers = new HashSet<>();
      ImmutableList<Riddle> dedupedRiddles =
          riddles
              .stream()
              // Deduplicate riddles on the question.
              .filter(riddle -> uniqueQuestions.add(riddle.getQuestion()))
              // Deduplicate riddles on the correct answers.
              .filter(riddle -> correctAnswers.add(riddle.getCorrectAnswer()))
              .collect(ImmutableList.toImmutableList());
      LOGGER.log(Level.INFO, "Found {0} riddles, {1} after de-duplication.",
          new Object[] {riddles.size(), dedupedRiddles.size()});
      return Optional.of(RiddlesContainer.newBuilder().addAllRiddles(dedupedRiddles).build());
    } catch (IOException ioe) {
      LOGGER.log(Level.SEVERE, "Generation Failed.", ioe);
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
      LOGGER.log(Level.FINE, "Stripped JSON code start.");
      if (str.endsWith("```")) {
        // Strip out code end.
        str = str.substring(0, str.length() - 3).trim();
        LOGGER.log(Level.FINE, "Stripped JSON code end.");
      }
    }
    if (str.indexOf("\\$") > 0) {
      str = str.replaceAll("\\$", "$");
      LOGGER.log(Level.FINE, "Un-escaped $ characters.");
    }
    if (str.indexOf("\"\"") > 0) {
      str = str.replaceAll("[\"]+", "\"");
      LOGGER.log(Level.FINE, "De-duped double quotes.");
    }
    if (str.indexOf("''") > 0) {
      str = str.replaceAll("[']+", "'");
      LOGGER.log(Level.FINE, "De-duped single quotes.");
    }
    if (!str.endsWith("}]")) {
      int lastAnswerIdx = str.lastIndexOf("},");
      String ignored = str.substring(lastAnswerIdx);
      str = str.substring(0, lastAnswerIdx) + "}]";
      LOGGER.log(Level.FINE, "Truncated answers, ignored: {0}", ignored);
    }
    String riddlesContainerStr = String.format("{ \"riddles\": %s }", str);
    try {
      return Optional.of(TextProtoUtils.parseJson(riddlesContainerStr, RiddlesContainer.class));
    } catch (InvalidProtocolBufferException ipbe) {
      LOGGER.log(Level.SEVERE, String.format("Failed to parse: %s", riddlesContainerStr), ipbe);
      return Optional.empty();
    }
  }

  /** Strips out any non-alphanumeric characters and simplifies whitespaces from the topic. */
  private static String normalizeTopic(String topic) {
    return topic.replaceAll("[^a-zA-Z0-9\\s]+", "").replaceAll("[\\s]+", " ").trim();
  }

  private static final String API_KEY = "AIzaSyC6doGY1WaMe3-ORdpzs2DktDYB3YsThzA";
  private static final Logger LOGGER = Logger.getLogger(GenerativeLanguageClient.class.getName());
}
