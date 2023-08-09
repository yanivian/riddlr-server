package com.yanivian.riddlr.backend.datastore;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Transaction;
import java.util.function.Function;

public final class DatastoreUtils {
  // Not instantiable.
  private DatastoreUtils() {}

  public static <T> T newTransaction(DatastoreService datastore, Function<Transaction, T> logic) {
    Transaction txn = datastore.beginTransaction();
    try {
      T result = logic.apply(txn);
      txn.commit();
      return result;
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
  }
}
