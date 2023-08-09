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
import com.google.protobuf.TextFormat.ParseException;
import com.yanivian.riddlr.backend.datastore.proto.RiddlesPayload;
import com.yanivian.riddlr.common.util.TextProtoUtils;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;

public final class TopicDao {
  private final DatastoreService datastore;

  @Inject
  TopicDao(DatastoreService datastore) {
    this.datastore = datastore;
  }

  public Optional<TopicModel> getRiddle(Transaction txn, String id) {
    try {
      Entity entity = datastore.get(txn, toKey(id));
      return Optional.of(new TopicModel(entity));
    } catch (EntityNotFoundException enfe) {
      return Optional.empty();
    }
  }

  public ImmutableMap<String, TopicModel> getRiddles(Transaction txn, ImmutableSet<String> ids) {
    Map<Key, Entity> entityMap = datastore.get(txn, Iterables.transform(ids, TopicDao::toKey));
    return entityMap.entrySet().stream().collect(ImmutableMap.toImmutableMap(
        entry -> entry.getKey().getName(), entry -> new TopicModel(entry.getValue())));
  }

  /** Find riddles associated with a given topic. */
  // Cannot be transactional.
  public ImmutableList<TopicModel> findRiddlesByTopic(String topic) {
    Query query =
        new Query(TopicModel.KIND)
            .setFilter(new FilterPredicate(TopicModel.Columns.Topic, FilterOperator.EQUAL, topic));
    return Streams.stream(datastore.prepare(query).asIterable())
        .map(TopicModel::new)
        .collect(ImmutableList.toImmutableList());
  }

  /** Returns a new unsaved riddle. */
  public TopicModel newRiddle(String id, String topic, RiddlesPayload payload) {
    Entity entity = new Entity(TopicModel.KIND, id);
    return new TopicModel(entity).setTopic(topic).setRiddlesPayload(payload);
  }

  public static final class TopicModel extends DatastoreModel<TopicModel> {
    static final String KIND = "Topic";
    static final class Columns {
      static final String Topic = "Topic";
      static final String RiddlesPayload = "RiddlesPayload";
    }

    private TopicModel(Entity entity) {
      super(entity);
    }

    public String getID() {
      return getKey().getName();
    }

    public TopicModel setTopic(String topic) {
      entity.setIndexedProperty(Columns.Topic, topic);
      return this;
    }

    public String getTopic() {
      return (String) entity.getProperty(Columns.Topic);
    }

    public TopicModel setRiddlesPayload(RiddlesPayload payload) {
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
    return KeyFactory.createKey(TopicModel.KIND, id);
  }
}
