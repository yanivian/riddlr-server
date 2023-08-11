package com.yanivian.riddlr.backend.operation;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.yanivian.riddlr.backend.datastore.DatastoreUtils;
import com.yanivian.riddlr.backend.datastore.RiddlesForTopicDao;
import com.yanivian.riddlr.backend.datastore.RiddlesForTopicDao.RiddlesForTopicModel;
import com.yanivian.riddlr.backend.datastore.proto.RiddlesPayload;
import com.yanivian.riddlr.backend.operation.proto.GetRiddlesForTopicRequest;
import com.yanivian.riddlr.backend.operation.proto.RiddlesForTopic;
import com.yanivian.riddlr.generativelanguage.GenerativeLanguageClient;
import com.yanivian.riddlr.generativelanguage.proto.RiddlesContainer;
import java.time.Clock;
import java.util.Optional;
import java.util.function.Function;

public final class GetRiddlesForTopicOp
    implements Function<GetRiddlesForTopicRequest, RiddlesForTopic> {
  private final GenerativeLanguageClient generativeLanguageClient;
  private final RiddlesForTopicDao riddlesForTopicDao;
  private final DatastoreService datastore;
  private final Clock clock;

  @Inject
  public GetRiddlesForTopicOp(GenerativeLanguageClient generativeLanguageClient,
      RiddlesForTopicDao riddlesForTopicDao, DatastoreService datastore, Clock clock) {
    this.generativeLanguageClient = generativeLanguageClient;
    this.riddlesForTopicDao = riddlesForTopicDao;
    this.datastore = datastore;
    this.clock = clock;
  }

  @Override
  public RiddlesForTopic apply(GetRiddlesForTopicRequest req) {
    String topic = req.getTopic();
    ImmutableList<RiddlesForTopicModel> riddlesForTopicList =
        riddlesForTopicDao.findRiddlesForTopic(topic);
    Optional<RiddlesForTopicModel> existingModel = (riddlesForTopicList.isEmpty())
        ? Optional.empty()
        : Optional.of(riddlesForTopicList.iterator().next());
    Optional<RiddlesForTopic> existingResult = existingModel.flatMap(RiddlesForTopicModel::toProto);
    if (existingResult.isPresent()) {
      // TODO: Check for freshness.
      return existingResult.get();
    }

    Optional<RiddlesContainer> riddles =
        generativeLanguageClient.getRiddlesForTopic(topic, NUM_RIDDLES, NUM_INCORRECT_ANSWERS);
    if (!riddles.isPresent()) {
      // Riddles could not be fetched, so do not persist.
      return RiddlesForTopic.getDefaultInstance();
    }
    RiddlesPayload newResult = toRiddlesPayload(riddles.get());

    return DatastoreUtils.newTransaction(datastore, txn -> {
      if (existingModel.isPresent()) {
        return existingModel.get()
            .setRiddlesPayload(newResult)
            .save(txn, datastore, clock)
            .toProto()
            .orElseThrow(IllegalStateException::new);
      } else {
        return riddlesForTopicDao.newRiddle(topic, newResult)
            .save(txn, datastore, clock)
            .toProto()
            .orElseThrow(IllegalStateException::new);
      }
    });
  }

  private static RiddlesPayload toRiddlesPayload(RiddlesContainer riddles) {
    return RiddlesPayload.newBuilder()
        .addAllRiddles(riddles.getRiddlesList()
                           .stream()
                           .map(riddle
                               -> RiddlesPayload.Riddle.newBuilder()
                                      .setQuestion(riddle.getQuestion())
                                      .setCorrectAnswer(riddle.getCorrectAnswer())
                                      .addAllIncorrectAnswers(riddle.getIncorrectAnswersList())
                                      .build())
                           .collect(ImmutableList.toImmutableList()))
        .build();
  }

  private static final int NUM_RIDDLES = 10;
  private static final int NUM_INCORRECT_ANSWERS = 4;
}
