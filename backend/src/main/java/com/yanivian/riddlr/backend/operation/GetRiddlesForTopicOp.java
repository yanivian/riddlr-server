package com.yanivian.riddlr.backend.operation;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.common.collect.ImmutableList;
import com.yanivian.riddlr.backend.datastore.RiddlesForTopicDao;
import com.yanivian.riddlr.backend.datastore.RiddlesForTopicDao.RiddlesForTopicModel;
import com.yanivian.riddlr.backend.operation.proto.GetRiddlesForTopicRequest;
import com.yanivian.riddlr.backend.operation.proto.RiddlesForTopic;
import java.time.Clock;
import java.util.Optional;
import java.util.function.Function;
import javax.inject.Inject;

public final class GetRiddlesForTopicOp
    implements Function<GetRiddlesForTopicRequest, RiddlesForTopic> {
  private final RiddlesForTopicDao riddlesForTopicDao;
  private final DatastoreService datastore;
  private final Clock clock;

  @Inject
  GetRiddlesForTopicOp(
      RiddlesForTopicDao riddlesForTopicDao, DatastoreService datastore, Clock clock) {
    this.riddlesForTopicDao = riddlesForTopicDao;
    this.datastore = datastore;
    this.clock = clock;
  }

  @Override
  public RiddlesForTopic apply(GetRiddlesForTopicRequest req) {
    String topic = normalizeTopic(req.getTopic());
    ImmutableList<RiddlesForTopicModel> riddlesForTopicList =
        riddlesForTopicDao.findRiddlesByTopic(topic);
    if (!riddlesForTopicList.isEmpty()) {
      Optional<RiddlesForTopic> result = riddlesForTopicList.iterator().next().toProto();
      if (result.isPresent()) {
        // TODO: Check for freshness.
        return result.get();
      }
    }
    // TODO: Generate riddles for the given topic and persist to Datastore.
    return RiddlesForTopic.getDefaultInstance();
  }

  private static String normalizeTopic(String topic) {
    return topic.toLowerCase().replaceAll("~[a-z0-9]", "");
  }
}
