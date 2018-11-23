package com.nucleus.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.mongodb.client.model.Filters;
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
  public String create(Document doc, String collectionName, List<AssociationUpdates> associationUpdates) {
    // Create entity
    getCollection(collectionName).insertOne(doc);
    ObjectId objectId = (ObjectId) doc.get(_ID);
    if (objectId == null) {
      throw new NucleusException("Couldn't create the data. Please try again.");
    }
    String id = objectId.toString();

    // Update associations
    if (associationUpdates != null && !associationUpdates.isEmpty()) {
      List<WriteModel<Document>> requests = new ArrayList<WriteModel<Document>>();
      for (AssociationUpdates update : associationUpdates) {
        WriteModel<Document> req =
            getUpdateAssociationModel(collectionName, id, update.getRefEntityName(), update.getRefEntityIds());
        requests.add(req);
      }
      BulkWriteResult result = getCollection(CollectionName.association.name()).bulkWrite(requests);
      NucleusLogger.info("Updated " + result.getModifiedCount() + " association docs.", this.getClass());
      if (result.getModifiedCount() == 0 && result.getUpserts().size() == 0) {
        throw new NucleusException("Couldn't update the association data. Please try again.");
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
    return update(query, updates, collectionName, null);
  }

  public Long update(Bson query, Map<String, Object> updates, String collectionName,
      List<AssociationUpdates> associationUpdates) {
    return update(query, updates, null, collectionName, associationUpdates);
  }

  public Long update(Bson query, Map<String, Object> updates, List<Document> arrayFilters, String collectionName) {
    return update(query, updates, arrayFilters, collectionName, null);
  }

  public Long update(Bson query, Map<String, Object> updates, List<Document> arrayFilters, String collectionName,
      List<AssociationUpdates> associationUpdates) {
    List<Bson> updatesBson = new ArrayList<>();
    updates.forEach((key, value) -> updatesBson.add(Updates.set(key, value)));

    UpdateOptions option = new UpdateOptions();
    option.upsert(true);
    if (arrayFilters != null && !arrayFilters.isEmpty()) {
      option.arrayFilters(arrayFilters);
    }

    // Update entity
    UpdateResult result = getCollection(collectionName).updateMany(query, Updates.combine(updatesBson), option);

    // Update associations
    if (associationUpdates != null && !associationUpdates.isEmpty()) {
      Long modifiedCount = updateAssociations(query, collectionName, associationUpdates).longValue();
      if (modifiedCount != result.getModifiedCount()) {
        NucleusLogger.warn("Entity and association update modified different number of docs.", this.getClass());
      }
    }
    return result.getModifiedCount();
  }

  private Integer updateAssociations(Bson query, String collectionName, List<AssociationUpdates> associationUpdates) {
    FindIterable<Document> ids = getCollection(collectionName).find(query).projection(Projections.include(_ID));

    List<WriteModel<Document>> requests = new ArrayList<WriteModel<Document>>();
    for (Document document : ids) {
      String id = document.get(_ID).toString();
      for (AssociationUpdates update : associationUpdates) {
        WriteModel<Document> req =
            getUpdateAssociationModel(collectionName, id, update.getRefEntityName(), update.getRefEntityIds());
        requests.add(req);
      }
    }
    BulkWriteResult result = getCollection(CollectionName.association.name()).bulkWrite(requests);
    NucleusLogger.info("Updated " + result.getModifiedCount() + " association docs.", this.getClass());
    return result.getModifiedCount();
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
        for (Object val : ((List) value)) {
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

  public UpdateOneModel<Document> getUpdateAssociationModel(String entityName, String entityId, String refEntityName,
      List<String> refEntityIds) {
    List<Document> idMappings = new ArrayList<Document>();
    if (refEntityIds instanceof List) {
      for (String id : refEntityIds) {
        Document idMapping = new Document();
        idMapping.append(entityName, entityId);
        idMapping.append(refEntityName, id);
        idMappings.add(idMapping);
      }
    } else {
      Document idMapping = new Document();
      idMapping.append(entityName, entityId);
      idMapping.append(refEntityName, refEntityIds);
      idMappings.add(idMapping);
    }
    Bson update = Updates.addEachToSet(Fields.ASS_MAPPING, idMappings);
    Bson query = Filters.eq(Fields.ASS_NAME, Arrays.asList(entityName, refEntityName));

    UpdateOptions option = new UpdateOptions();
    option.upsert(true);

    return new UpdateOneModel<Document>(query, update, option);
  }

  private void convertId(List<Map<String, Object>> docs) {
    for (Map<String, Object> doc : docs) {
      ObjectId id = (ObjectId) doc.remove(_ID);
      doc.put(Fields.ID, id.toString());
    }
  }

}
