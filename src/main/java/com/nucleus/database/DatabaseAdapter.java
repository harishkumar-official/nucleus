package com.nucleus.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.DeleteManyModel;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.UpdateResult;
import com.nucleus.constants.Fields;
import com.nucleus.exception.NucleusException;
import com.nucleus.logger.NucleusLogger;
import com.nucleus.transientmodel.AssociationUpdates;

@Component
@SuppressWarnings({"rawtypes", "unchecked"})
public class DatabaseAdapter {

  private static final String DATABASE = "nucleus";
  private static final String _ID = "_id";

  private MongoClient mongoClient;
  private MongoDatabase db;
  private Map<String, MongoCollection<Document>> collectionsMap;

  @Autowired
  public DatabaseAdapter(@Value("${mongohost}") String connectionString) {
    mongoClient = MongoClients.create("mongodb://" + connectionString.trim());
    setup();
  }

  public DatabaseAdapter(String user, char[] password, String database, String host, Integer port) {
    MongoCredential credential = MongoCredential.createCredential(user, database, password);

    MongoClientSettings settings =
        MongoClientSettings.builder().credential(credential).applyToSslSettings(builder -> builder.enabled(true))
            .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(host, port)))).build();

    mongoClient = MongoClients.create(settings);
    setup();
  }

  private void setup() {
    db = mongoClient.getDatabase(DATABASE);
    collectionsMap = new HashMap<String, MongoCollection<Document>>();
    for (CollectionName coll : CollectionName.values()) {
      collectionsMap.put(coll.name(), db.getCollection(coll.name()));
    }
  }

  private MongoCollection<Document> getCollection(String collectionName) {
    if (!collectionsMap.containsKey(collectionName)) {
      collectionsMap.put(collectionName, db.getCollection(collectionName));
    }
    return collectionsMap.get(collectionName);
  }


  /*-----Indexing-----*/

  /**
   * Index the fields.
   */
  public boolean index(Set<String> fields, String collectionName) {
    List<IndexModel> indexes = new ArrayList<>();
    fields.forEach(field -> {
      IndexModel index = new IndexModel(Indexes.ascending(field));
      indexes.add(index);
    });
    List<String> indexesList = getCollection(collectionName).createIndexes(indexes);
    return indexesList.size() == fields.size() ? true : false;
  }


  /*-----CREATE-----*/

  /**
   * Insert the doc.
   */
  public String create(Document doc, String collectionName) {
    getCollection(collectionName).insertOne(doc);
    ObjectId objectId = (ObjectId) doc.get(_ID);
    if (objectId == null) {
      throw new NucleusException("Couldn't create the data. Please try again.");
    }
    return objectId.toString();
  }

  /**
   * Insert the doc with association data.
   */
  public String create(Document doc, String collectionName, List<AssociationUpdates> associationUpdates,
      String client) {
    // Create entity
    getCollection(collectionName).insertOne(doc);
    ObjectId objectId = (ObjectId) doc.get(_ID);
    if (objectId == null) {
      throw new NucleusException("Couldn't create the data. Please try again.");
    }
    String id = objectId.toString();

    // Update associations
    Long modifiedCount = 0L;
    if (associationUpdates != null && !associationUpdates.isEmpty()) {
      Bson query = Filters.eq(new ObjectId(id));
      modifiedCount = updateAssociations(query, collectionName, associationUpdates, client).longValue();
      if (modifiedCount == 0) {
        NucleusLogger.warn("Association update failed.", this.getClass());
      }
    }

    return id;
  }


  /*-----UPDATE-----*/
  /* If a field does not exist, the operator adds the field to the document. */

  public boolean setDefault(Bson query, boolean defaultDoc, String collectionName) {
    UpdateResult result = getCollection(collectionName).updateOne(query, Updates.set(Fields.DEFAULT, defaultDoc));
    return result.getModifiedCount() == 1;
  }

  public Long updateOne(String id, Map<String, Object> updates, String collectionName) {
    return update(Filters.eq(new ObjectId(id)), updates, collectionName);
  }

  public Long update(Bson query, Map<String, Object> updates, String collectionName) {
    return update(query, updates, null, collectionName);
  }

  public Long update(Bson query, Map<String, Object> updates, List<Document> arrayFilters, String collectionName) {
    return update(query, updates, arrayFilters, collectionName, null, null);
  }

  public Long update(Bson query, Map<String, Object> updates, List<Document> arrayFilters, String collectionName,
      List<AssociationUpdates> associationUpdates, String client) {
    List<Bson> updatesBson = new ArrayList<>();
    updates.forEach((key, value) -> updatesBson.add(Updates.set(key, value)));

    UpdateOptions option = new UpdateOptions();
    option.upsert(true);
    if (arrayFilters != null && !arrayFilters.isEmpty()) {
      option.arrayFilters(arrayFilters);
    }

    // Update entity
    UpdateResult result = null;
    if (!updatesBson.isEmpty()) {
      result = getCollection(collectionName).updateMany(query, Updates.combine(updatesBson), option);;
    }

    // Update associations
    Long modifiedCount = 0L;
    if (associationUpdates != null && !associationUpdates.isEmpty()) {
      modifiedCount = updateAssociations(query, collectionName, associationUpdates, client).longValue();
      if (modifiedCount == 0) {
        NucleusLogger.warn("Association update failed.", this.getClass());
      }
    }
    return result == null ? modifiedCount : result.getModifiedCount();
  }

  private Integer updateAssociations(Bson query, String collectionName, List<AssociationUpdates> associationUpdates,
      String client) {
    FindIterable<Document> ids = getCollection(collectionName).find(query).projection(Projections.include(_ID));

    List<WriteModel<Document>> requests = new ArrayList<WriteModel<Document>>();
    for (Document document : ids) {
      String id = document.get(_ID).toString();
      for (AssociationUpdates update : associationUpdates) {
        updateWriteRequests(requests, update, id, client);
      }
    }
    BulkWriteResult result = getCollection(CollectionName.association.name()).bulkWrite(requests);
    NucleusLogger.info("Updated " + result.getModifiedCount() + " association docs.", this.getClass());
    return result.getModifiedCount() > 0 ? result.getModifiedCount() : result.getInsertedCount();
  }

  private void updateWriteRequests(List<WriteModel<Document>> requests, AssociationUpdates associationUpdate,
      String parentEntityDocId, String client) {
    String parentEntityName = associationUpdate.getParentEntityName();
    String refEntityName = associationUpdate.getRefEntityName();

    for (Map<String, Object> refEntityIdsMap : associationUpdate.getRefEntityIdsMapList()) {
      String newRefEntityDocId = (String) refEntityIdsMap.get(Fields.VALUE);

      if (refEntityIdsMap.containsKey(Fields.PREVIOUS_VALUE)) {
        String prevRefEntityDocId = (String) refEntityIdsMap.get(Fields.PREVIOUS_VALUE);
        Bson update = Updates.set(Fields.ASS_MAPPING, Arrays.asList(newRefEntityDocId, parentEntityDocId));
        Bson query =
            Filters.and(Filters.eq(Fields.ASS_NAME, parentEntityName), Filters.eq(Fields.ASS_NAME, refEntityName),
                Filters.eq(Fields.ASS_MAPPING, parentEntityDocId), Filters.eq(Fields.ASS_MAPPING, prevRefEntityDocId));
        requests.add(new UpdateOneModel<Document>(query, update));
      } else {
        Document document = new Document();
        document.append(Fields.CLIENT, client);
        document.append(Fields.ASS_NAME, Arrays.asList(parentEntityName, refEntityName));
        document.append(Fields.ASS_MAPPING, Arrays.asList(newRefEntityDocId, parentEntityDocId));
        requests.add(new InsertOneModel<>(document));
      }
    }
  }

  public Integer deleteAssociations(List<String> parentIds, List<AssociationUpdates> associationUpdates) {
    List<WriteModel<Document>> requests = new ArrayList<WriteModel<Document>>();
    for (AssociationUpdates associationUpdate : associationUpdates) {
      String parentEntityName = associationUpdate.getParentEntityName();
      String refEntityName = associationUpdate.getRefEntityName();

      for (Map<String, Object> refEntityIdsMap : associationUpdate.getRefEntityIdsMapList()) {
        String refEntityDocId = (String) refEntityIdsMap.get(Fields.VALUE);
        Bson query =
            Filters.and(Filters.eq(Fields.ASS_NAME, refEntityName), Filters.eq(Fields.ASS_NAME, parentEntityName),
                Filters.eq(Fields.ASS_MAPPING, refEntityDocId), Filters.in(Fields.ASS_MAPPING, parentIds));
        requests.add(new DeleteManyModel<>(query));
      }
    }
    BulkWriteResult result = getCollection(CollectionName.association.name()).bulkWrite(requests);
    return result.getDeletedCount();
  }


  /*-----Array Updates-----*/

  public Long addInArray(Bson query, Map<String, Object> updates, List<Document> arrayFilters, String collectionName) {
    List<Bson> updatesBson = new ArrayList<>();
    for (String field : updates.keySet()) {
      Object value = updates.get(field);
      if (value instanceof List) {
        updatesBson.add(Updates.addEachToSet(field, (List<Document>) value));
      } else {
        updatesBson.add(Updates.addToSet(field, value));
      }
    }
    return updateArray(query, updatesBson, arrayFilters, collectionName);
  }

  public Long deleteInArray(Bson query, Integer existingMaxSerial, Map<String, Object> deletes,
      List<Document> arrayFilters, String collectionName) {
    Map<String, List<Integer>> deletedSerialMap = new HashMap<>();
    List<Bson> deletesBson = new ArrayList<>();
    for (String field : deletes.keySet()) {
      Object value = deletes.get(field);
      if (value instanceof List) {
        for (Object val : (List) value) {
          deletesBson.add(Updates.pull(field, val));
          addDeletedSerials(field, val, deletedSerialMap);
        }
      } else {
        deletesBson.add(Updates.pull(field, value));
        addDeletedSerials(field, value, deletedSerialMap);
      }
    }
    Long modifiedCount = updateArray(query, deletesBson, arrayFilters, collectionName);

    // shift serial of other array entries
    if (!deletedSerialMap.isEmpty() && existingMaxSerial > 1) {
      Map<String, Object> fieldNewSerialsMap = getSerialMapToUpdate(deletedSerialMap, existingMaxSerial, arrayFilters);
      if (!fieldNewSerialsMap.isEmpty()) {
        update(query, fieldNewSerialsMap, arrayFilters, collectionName);
      }
    }

    return modifiedCount;
  }

  private void addDeletedSerials(String field, Object val, Map<String, List<Integer>> deletedSerialMap) {
    if (val instanceof Map && ((Map) val).containsKey(Fields.SERIAL)) {
      List<Integer> deletedSerials;
      if (deletedSerialMap.containsKey(field)) {
        deletedSerials = deletedSerialMap.get(field);
      } else {
        deletedSerials = new ArrayList<>();
        deletedSerialMap.put(field, deletedSerials);
      }

      deletedSerials.add((int) ((Map) val).get(Fields.SERIAL));
    }
  }

  private Map<String, Object> getSerialMapToUpdate(Map<String, List<Integer>> deletedSerialMap,
      Integer existingMaxSerial, List<Document> arrayFilters) {
    Document lastFilter = arrayFilters.isEmpty() ? null : arrayFilters.get(arrayFilters.size() - 1);// eg: e1.serial=5
    String filterKey = lastFilter == null ? "e1.serial" : lastFilter.keySet().stream().findFirst().orElse("e1.serial");
    int jIndex = Integer.parseInt(filterKey.substring(1, filterKey.length() - 7)) + 1;

    Map<String, Object> fieldNewSerialsMap = new HashMap<>();
    for (String field : deletedSerialMap.keySet()) {
      List<Integer> deletedSerials = deletedSerialMap.get(field);
      deletedSerials.sort(new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
          return o1.compareTo(o2);
        }
      });

      int lastSerial = deletedSerials.get(0);
      for (int i = 1; i <= deletedSerials.size(); i++) {
        int currSerial;
        if (i == deletedSerials.size()) {
          currSerial = existingMaxSerial + 1;
        } else {
          currSerial = deletedSerials.get(i);
        }

        lastSerial++;
        while (lastSerial < currSerial) {
          fieldNewSerialsMap.put(field + ".$[e" + jIndex + "].serial", lastSerial - i);
          Document doc = new Document();
          doc.append("e" + jIndex + "." + Fields.SERIAL, lastSerial); // eg: e1.serial = 5
          arrayFilters.add(doc);
          jIndex++;
          lastSerial++;
        }

        lastSerial = currSerial;
      }
    }
    return fieldNewSerialsMap;
  }

  public Long updateArray(Bson query, List<Bson> updatesBson, List<Document> arrayFilters, String collectionName) {
    UpdateOptions option = new UpdateOptions();
    if (arrayFilters != null && !arrayFilters.isEmpty()) {
      option.arrayFilters(arrayFilters);
    }
    UpdateResult result = getCollection(collectionName).updateMany(query, Updates.combine(updatesBson), option);
    return result.getModifiedCount();
  }

  public Long replaceOne(Bson query, Document doc, String collectionName) {
    UpdateResult result = getCollection(collectionName).replaceOne(query, doc);
    return result.getModifiedCount();
  }


  /*-----GET-----*/

  public List<Map<String, Object>> get(Bson query, String collectionName) {
    List<Map<String, Object>> docs =
        getCollection(collectionName).find(query).into(new ArrayList<Map<String, Object>>());
    convertId(docs);
    return docs;
  }

  public List<Map<String, Object>> get(Bson query, List<String> returnFields, String collectionName) {
    List<Map<String, Object>> docs = getCollection(collectionName).find(query)
        .projection(Projections.include(returnFields)).into(new ArrayList<Map<String, Object>>());
    convertId(docs);
    return docs;
  }

  public boolean exists(Bson query, String collectionName) {
    long count = getCollection(collectionName).countDocuments(query);
    return count > 0 ? true : false;
  }


  /*-----Helpers-----*/

  private void convertId(List<Map<String, Object>> docs) {
    for (Map<String, Object> doc : docs) {
      ObjectId id = (ObjectId) doc.remove(_ID);
      doc.put(Fields.ID, id.toString());
    }
  }

}
