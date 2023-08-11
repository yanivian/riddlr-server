package com.yanivian.riddlr.generativelanguage;

import com.google.ai.generativelanguage.v1beta2.GenerateTextRequest;
import com.google.ai.generativelanguage.v1beta2.GenerateTextResponse;
import com.google.ai.generativelanguage.v1beta2.TextPrompt;
import com.google.ai.generativelanguage.v1beta2.TextServiceClient;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.yanivian.riddlr.generativelanguage.proto.Riddle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GenerativeLanguageClient {
  private final TextServiceClient textServiceClient;

  @Inject
  public GenerativeLanguageClient(TextServiceClient textServiceClient) {
    this.textServiceClient = textServiceClient;
  }

  public ImmutableList<Riddle> getRiddlesForTopic(
      String topic, int numRiddles, int numIncorrectAnswers) {
    StringBuilder promptText =
        new StringBuilder()
            .append(String.format(
                "I want to quiz my friend on the topic of \"%s\". ", normalizeTopic(topic)))
            .append(String.format(
                "List %s short questions and their word or phrase answers, together with up to %s other answers that are incorrect but reasonable? ",
                numRiddles, numIncorrectAnswers))
            .append("Output in JSON with fields: question, correctAnswer, incorrectAnswers.");
    TextPrompt prompt = TextPrompt.newBuilder().setText(promptText.toString()).build();
    GenerateTextRequest req =
        GenerateTextRequest.newBuilder().setModel(TEXT_MODEL).setPrompt(prompt).build();
    GenerateTextResponse resp = textServiceClient.generateText(req);
    if (resp.getCandidatesCount() == 0) {
      return ImmutableList.of();
    }
    String output = resp.getCandidates(0).getOutput();
    // FIXME
    LOGGER.atInfo().log(output);
    return ImmutableList.of();
  }

  /** Strips out any non-alphanumeric characters and simplifies whitespaces from the topic. */
  private static String normalizeTopic(String topic) {
    return topic.replaceAll("[^a-zA-Z0-9\\s]+", "").replaceAll("[\\s]+", " ").trim();
  }

  private static final String TEXT_MODEL = "models/chat-bison-001";
  private static final Logger LOGGER = LogManager.getLogger(GenerativeLanguageClient.class);
}
