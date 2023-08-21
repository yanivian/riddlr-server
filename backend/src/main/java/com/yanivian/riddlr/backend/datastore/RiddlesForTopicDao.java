package com.yanivian.riddlr.backend.datastore;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Transaction;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.inject.Inject;
import com.google.protobuf.TextFormat.ParseException;
import com.yanivian.riddlr.backend.datastore.proto.RiddlesPayload;
import com.yanivian.riddlr.backend.operation.proto.RiddlesForTopic;
import com.yanivian.riddlr.common.util.TextProtoUtils;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class RiddlesForTopicDao {
  private final DatastoreService datastore;

  @Inject
  public RiddlesForTopicDao(DatastoreService datastore) {
    this.datastore = datastore;
  }

  public Optional<RiddlesForTopicModel> getRiddle(Transaction txn, String id) {
    try {
      Entity entity = datastore.get(txn, toKey(id));
      return Optional.of(new RiddlesForTopicModel(entity));
    } catch (EntityNotFoundException enfe) {
      return Optional.empty();
    }
  }

  public ImmutableMap<String, RiddlesForTopicModel> getRiddles(
      Transaction txn, ImmutableSet<String> ids) {
    Map<Key, Entity> entityMap =
        datastore.get(txn, Iterables.transform(ids, RiddlesForTopicDao::toKey));
    return entityMap.entrySet().stream().collect(ImmutableMap.toImmutableMap(
        entry -> entry.getKey().getName(), entry -> new RiddlesForTopicModel(entry.getValue())));
  }

  /** Find riddles associated with a given topic. */
  // Cannot be transactional.
  public ImmutableList<RiddlesForTopicModel> findRiddlesForTopic(String topic) {
    Query query = new Query(RiddlesForTopicModel.KIND)
                      .setFilter(new FilterPredicate(RiddlesForTopicModel.Columns.Topic,
                          FilterOperator.EQUAL, normalizeTopic(topic)));
    return Streams.stream(datastore.prepare(query).asIterable())
        .map(RiddlesForTopicModel::new)
        .collect(ImmutableList.toImmutableList());
  }

  /** Returns a new unsaved riddle. */
  public RiddlesForTopicModel newRiddle(String topic, RiddlesPayload payload) {
    Entity entity = new Entity(RiddlesForTopicModel.KIND, UUID.randomUUID().toString());
    return new RiddlesForTopicModel(entity)
        .setTopic(normalizeTopic(topic))
        .setRiddlesPayload(payload);
  }

  public static final class RiddlesForTopicModel extends DatastoreModel<RiddlesForTopicModel> {
    static final String KIND = "Topic";
    static final class Columns {
      static final String Topic = "Topic";
      static final String RiddlesPayload = "RiddlesPayload";
    }

    private RiddlesForTopicModel(Entity entity) {
      super(entity);
    }

    public String getID() {
      return getKey().getName();
    }

    public Optional<RiddlesForTopic> toProto() {
      try {
        ImmutableList<RiddlesForTopic.Riddle> riddles =
            getRiddlesPayload()
                .getRiddlesList()
                .stream()
                .map(RiddlesForTopicModel::transformRiddle)
                .collect(ImmutableList.toImmutableList());
        return Optional.of(
            RiddlesForTopic.newBuilder().setID(getID()).addAllRiddles(riddles).build());
      } catch (ParseException pe) {
        LOGGER.atError().withThrowable(pe).log("Failed to decode payload for ID: {}", getID());
        return Optional.empty();
      }
    }

    private static RiddlesForTopic.Riddle transformRiddle(RiddlesPayload.Riddle riddle) {
      return RiddlesForTopic.Riddle.newBuilder()
          .setQuestion(riddle.getQuestion())
          .setCorrectAnswer(riddle.getCorrectAnswer())
          .addAllIncorrectAnswers(riddle.getIncorrectAnswersList())
          .setExplanation(riddle.getExplanation())
          .setCitationURL(riddle.getCitationURL())
          .build();
    }

    public RiddlesForTopicModel setTopic(String topic) {
      entity.setIndexedProperty(Columns.Topic, topic);
      return this;
    }

    public String getTopic() {
      return (String) entity.getProperty(Columns.Topic);
    }

    public RiddlesForTopicModel setRiddlesPayload(RiddlesPayload payload) {
      entity.setUnindexedProperty(Columns.RiddlesPayload, new Blob(TextProtoUtils.encode(payload)));
      return this;
    }

    /** @throws ParseException when the payload has unexpectedly become incompatible. */
    public RiddlesPayload getRiddlesPayload() throws ParseException {
      Blob blob = (Blob) entity.getProperty(Columns.RiddlesPayload);
      return TextProtoUtils.decode(blob.getBytes(), RiddlesPayload.class);
    }
  }

  static Key toKey(String id) {
    return KeyFactory.createKey(RiddlesForTopicModel.KIND, id);
  }

  /** Lower-cases and strips out any non-alphanumeric characters from the topic. */
  private static String normalizeTopic(String topic) {
    return topic.toLowerCase().replaceAll("[^a-z0-9]+", "");
  }

  private static final Logger LOGGER = LogManager.getLogger(RiddlesForTopicDao.class);
}
