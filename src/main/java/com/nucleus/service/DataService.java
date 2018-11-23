package com.nucleus.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.nucleus.constants.Fields;
import com.nucleus.database.CollectionName;
import com.nucleus.database.DatabaseAdapter;
import com.nucleus.exception.NucleusException;
import com.nucleus.metadata.Entity;
import com.nucleus.metadata.FieldLevel;
import com.nucleus.metadata.Metadata;
import com.nucleus.proxy.amazon.AmazonS3Adapter;
import com.nucleus.transientmodel.AssociationUpdates;

@Service
@SuppressWarnings({"rawtypes", "unchecked"})
public class DataService {

  private AmazonS3Adapter amazonS3Adapter;
  private DatabaseAdapter databaseAdapter;
  private MetadataService metadataService;
  private AssociationService associationService;

  @Autowired
  public DataService(DatabaseAdapter database, MetadataService metadataService, AmazonS3Adapter amazonS3Adapter,
      AssociationService associationService) {
    this.databaseAdapter = database;
    this.metadataService = metadataService;
    this.amazonS3Adapter = amazonS3Adapter;
    this.associationService = associationService;
  }

  private void checkMandatoryField(Object field, String fieldName) {
    if (field == null || field instanceof String && StringUtils.isEmpty(((String) field).trim())
        || field instanceof Map && ((Map) field).isEmpty()) {
      throw new NucleusException("Mandatory field '" + fieldName + "' is missing.");
    }
  }

  private void checkMandatoryFieldsExistence(Metadata meta, String entity, String client) {
    if (meta == null) {
      throw new NucleusException("Client '" + client + "' doesn't exists.");
    }
    if (meta.getEntity(entity) == null) {
      throw new NucleusException("Given entity '" + entity + "' doesn't exists.");
    }
  }

  private void checkMandatoryFields(String client, String entity) {
    checkMandatoryField(client, Fields.CLIENT);
    checkMandatoryField(entity, Fields.ENTITY);
  }

  private void checkMandatoryMetaUpdateFields(List<String> ids, String client, Map<String, Object> updates) {
    checkMandatoryField(client, Fields.CLIENT);
    checkMandatoryField(ids, "ids");
    checkMandatoryField(updates, "updates");
  }

  // query-form -> field1:value1,field2:value2
  private Map<String, Object> parseQuery(String client, String entityName, String query, Metadata meta) {
    Set<String> globalFieldsSet = meta.getGlobalFieldsSet();
    Set<String> primaryAndQueryFields = meta.getEntity(entityName).getFields().stream()
        .filter(field -> field.getFieldLevel() != null
            && (field.getFieldLevel().equals(FieldLevel.primary) || field.getFieldLevel().equals(FieldLevel.query)))
        .map(field -> field.getFieldName()).collect(Collectors.toSet());

    String id = null;
    Map<String, Object> keyValueMap = new HashMap<>();
    String[] queryFields = query.split(",");
    for (String field : queryFields) {
      String[] keyValue = field.split(":");
      if (keyValue.length < 2) {
        continue;
      }
      String key = keyValue[0].trim();
      String value = keyValue[1].trim();
      if (globalFieldsSet.contains(key) || primaryAndQueryFields.contains(key)) {
        keyValueMap.put(key, value);
      } else if (Fields.ID.equals(key)) {
        id = value;
      } else {
        throw new NucleusException("Error, can't query on this field - " + key);
      }
    }
    // verify key-value map
    List<Object> response = metadataService.convertInputUpdateToDbUpdates(keyValueMap, entityName, client, meta);
    Map<String, Object> verifiedKeyValueMap = (Map<String, Object>) response.get(1);
    if (id != null) {
      verifiedKeyValueMap.put(Fields.ID, id);
    }
    return verifiedKeyValueMap;
  }

  private String getCollectionName(String client, String entity) {
    return new StringBuilder(client).append("_").append(entity).toString();
  }

  private void addSerial(Map<String, Object> doc) {
    if (doc != null) {
      for (Object value : doc.values()) {
        if (value instanceof List) {
          addSerial((List) value);
        }
      }
    }
  }

  private void addSerial(List<Object> list) {
    Integer index = 1;
    for (Object item : list) {
      if (item instanceof Map) {
        ((Map<String, Object>) item).put(Fields.SERIAL, index);
        addSerial((Map<String, Object>) item);
        index++;
      }
    }
  }

  private void removePrimaryFields(Entity entity, Map<String, Object> updates) {
    List<String> primaryFields = entity.getPrimaryFieldsName();
    primaryFields.forEach(field -> updates.remove(field));
  }

  private void checkJsonClientExists(String client, String environment, String localization) {
    Bson query = QueryService.getQuery(client, null, environment, localization);
    if (databaseAdapter.exists(query, Fields.SIMPLE_CLIENT_DEFAULT_ENTITY)) {
      throw new NucleusException("Already exists.");
    }
  }

  private void checkMetaClientExists(String client, String localization) {
    Bson query = QueryService.getQuery(client, localization);
    if (databaseAdapter.exists(query, CollectionName.metadata.name())) {
      throw new NucleusException("Client '" + client + "' with localization '" + localization + "' already exists.");
    }
  }

  /*-----Client APIs-----*/

  public boolean clientExists(String client) {
    Bson query = QueryService.getQuery(client);
    if (databaseAdapter.exists(query, CollectionName.metadata.name())) {
      return true;
    }
    if (databaseAdapter.exists(query, Fields.SIMPLE_CLIENT_DEFAULT_ENTITY)) {
      return true;
    }
    return false;
  }

  /*-----Meta-Data APIs-----*/

  public Metadata getMetadataObject(String client) {
    return metadataService.getMetadata(client);
  }

  public List<Map<String, Object>> getMetaData(String client, String localization) {
    checkMandatoryField(client, Fields.CLIENT);

    Bson query = QueryService.getQuery(client, localization);
    return databaseAdapter.get(query, CollectionName.metadata.name());
  }

  public Boolean updateMetadata(List<String> ids, String client, Map<String, Object> updates) {
    checkMandatoryMetaUpdateFields(ids, client, updates);

    Metadata meta = metadataService.getMetadata(client);
    List<Object> data = metadataService.convertMetaUpdateToDbUpdates(meta, updates, false);
    Map<String, Object> updatesToSet = (Map<String, Object>) data.get(0);
    List<Document> arrayFilters = (List<Document>) data.get(1);

    Bson query = QueryService.getQuery(client, ids);
    Long updatedCount = databaseAdapter.update(query, updatesToSet, arrayFilters, CollectionName.metadata.name());
    return checkSuccess(ids.size(), updatedCount.intValue());
  }

  private boolean checkSuccess(Integer totalCount, Integer updatedCount) {
    if (updatedCount == totalCount) {
      return true;
    } else if (updatedCount > 0 && updatedCount < totalCount) {
      throw new NucleusException("Could update only " + updatedCount + " docs, out of " + totalCount + " docs.");
    }
    return false;
  }

  public boolean addInMetaArray(List<String> ids, String client, Map<String, Object> updates) {
    checkMandatoryMetaUpdateFields(ids, client, updates);

    Metadata meta = metadataService.getMetadata(client);
    List<Object> data = metadataService.convertMetaUpdateToDbUpdates(meta, updates, true);
    Map<String, Object> updatesToSet = (Map<String, Object>) data.get(0);
    List<Document> arrayFilters = (List<Document>) data.get(1);

    Bson query = QueryService.getQuery(client, ids);
    Long updatedCount = databaseAdapter.addInArray(query, updatesToSet, arrayFilters, CollectionName.metadata.name());
    return checkSuccess(ids.size(), updatedCount.intValue());
  }

  public boolean deleteInMetaArray(List<String> ids, String client, Integer existingMaxSerial,
      Map<String, Object> deletes) {
    checkMandatoryMetaUpdateFields(ids, client, deletes);

    Metadata meta = metadataService.getMetadata(client);
    List<Object> data = metadataService.convertMetaUpdateToDbUpdates(meta, deletes, false);
    Map<String, Object> deletesToSet = (Map<String, Object>) data.get(0);
    List<Document> arrayFilters = (List<Document>) data.get(1);

    Bson query = QueryService.getQuery(client, ids);
    Long updatedCount = databaseAdapter.deleteInArray(query, existingMaxSerial, deletesToSet, arrayFilters,
        CollectionName.metadata.name());
    return checkSuccess(ids.size(), updatedCount.intValue());
  }

  public String createMetaData(String client, String localization, Map<String, Object> metadata) {
    checkMandatoryField(client, Fields.CLIENT);
    checkMandatoryField(localization, Fields.LOCALIZATION);
    checkMandatoryField(metadata, Fields.METADATA);
    checkMetaClientExists(client, localization);

    addSerial(metadata);
    metadata.put(Fields.CLIENT, client);
    metadata.put(Fields.LOCALIZATION, localization);
    metadata.remove(Fields.ID);

    String metaJson = metadataService.validateMetadata(null, metadata, false);
    Document doc = Document.parse(metaJson);
    return databaseAdapter.create(doc, CollectionName.metadata.name());
  }


  /*-----Entity APIs-----*/

  public List<Map<String, Object>> getDocList(String client, String entityName, Metadata metadata) {
    StringBuilder queryFields = new StringBuilder();
    metadata.getGlobalFields().forEach(gf -> {
      Set<String> values = gf.getValues();
      if (values.size() > 0) {
        queryFields.append(gf.getFieldName()).append(':').append(values.iterator().next()).append(',');
      }
    });
    queryFields.setLength(queryFields.length() - 1);

    Entity entity = metadata.getEntity(entityName);
    List<String> returnFields = new ArrayList<>();
    entity.getFields().forEach(field -> {
      if (field.getFieldLevel() != null && field.getFieldLevel().equals(FieldLevel.primary)) {
        returnFields.add(field.getFieldName());
      }
    });

    return getEntities(client, entityName, queryFields.toString(), returnFields, false);
  }

  public List<Map<String, Object>> getEntities(String client, String entity, String queryFields,
      List<String> returnFields, boolean withAssociationData) {
    checkMandatoryFields(client, entity);
    checkMandatoryField(queryFields, "query");
    Metadata meta = metadataService.getMetadata(client);
    checkMandatoryFieldsExistence(meta, entity, client);
    Map<String, Object> queryFieldsMap = parseQuery(client, entity, queryFields, meta);

    Bson query = QueryService.getQuery(queryFieldsMap);

    List<Map<String, Object>> documents = null;
    if (returnFields == null) {
      documents = databaseAdapter.get(query, getCollectionName(client, entity));
    } else {
      documents = databaseAdapter.get(query, returnFields, getCollectionName(client, entity));
    }
    if (withAssociationData) {
      associationService.updateResponseWithAssociationData(documents, entity, meta);
    }
    return documents;
  }

  public boolean setDefault(String client, String entity, String id, boolean defaultDoc) {
    checkMandatoryFields(client, entity);
    Metadata meta = metadataService.getMetadata(client);
    checkMandatoryFieldsExistence(meta, entity, client);
    checkMandatoryField(id, "id");

    Bson query = QueryService.getQuery(Arrays.asList(id));
    return databaseAdapter.setDefault(query, defaultDoc, entity);
  }

  public Long updatePrimaryFields(String client, String entity, String id, Map<String, Object> updates) {
    checkMandatoryFields(client, entity);
    Metadata meta = metadataService.getMetadata(client);
    checkMandatoryFieldsExistence(meta, entity, client);
    checkMandatoryField(id, "id");

    List<Object> data = metadataService.convertInputUpdateToDbUpdates(updates, entity, client, meta);
    Map<String, Object> updatesToSet = (Map<String, Object>) data.get(1);

    // check duplicate primary fields
    Bson query = QueryService.getQuery(updatesToSet);
    List<Map<String, Object>> response = databaseAdapter.get(query, getCollectionName(client, entity));
    if (!response.isEmpty() && !response.get(0).get(Fields.ID).equals(id)) {
      throw new NucleusException("These primary field values already exists.");
    }

    query = QueryService.getQuery(Arrays.asList(id));
    return databaseAdapter.update(query, updatesToSet, null, getCollectionName(client, entity), null);
  }

  public Long updateEntities(String client, String entity, List<String> ids, Map<String, Object> updates) {
    checkMandatoryFields(client, entity);
    Metadata meta = metadataService.getMetadata(client);
    checkMandatoryFieldsExistence(meta, entity, client);
    checkMandatoryField(ids, "ids");
    // remove primary fields
    removePrimaryFields(meta.getEntity(entity), updates);

    List<Object> data = metadataService.convertInputUpdateToDbUpdates(updates, entity, client, meta);
    List<AssociationUpdates> associationUpdates = (List<AssociationUpdates>) data.get(0);
    Map<String, Object> updatesToSet = (Map<String, Object>) data.get(1);
    List<Document> arrayFilters = (List<Document>) data.get(2);
    Bson query = QueryService.getQuery(ids);
    return databaseAdapter.update(query, updatesToSet, arrayFilters, getCollectionName(client, entity),
        associationUpdates);
  }

  public Long addInEntityArray(String client, String entity, List<String> ids, Map<String, Object> updates) {
    checkMandatoryFields(client, entity);
    Metadata meta = metadataService.getMetadata(client);
    checkMandatoryFieldsExistence(meta, entity, client);
    checkMandatoryField(ids, "ids");
    // remove primary fields
    removePrimaryFields(meta.getEntity(entity), updates);

    List<Object> data = metadataService.convertInputUpdateToDbUpdates(updates, entity, client, meta);
    Map<String, Object> updatesToSet = (Map<String, Object>) data.get(1);
    List<Document> arrayFilters = (List<Document>) data.get(2);
    Bson query = QueryService.getQuery(ids);
    return databaseAdapter.addInArray(query, updatesToSet, arrayFilters, getCollectionName(client, entity));
  }

  public Long deleteInEntityArray(String client, String entity, List<String> ids, Map<String, Object> deletes,
      Integer arraySize) {
    checkMandatoryFields(client, entity);
    Metadata meta = metadataService.getMetadata(client);
    checkMandatoryFieldsExistence(meta, entity, client);
    checkMandatoryField(ids, "ids");
    checkMandatoryField(arraySize, "array_size");
    // remove primary fields
    removePrimaryFields(meta.getEntity(entity), deletes);

    List<Object> data = metadataService.convertInputUpdateToDbUpdates(deletes, entity, client, meta);
    Map<String, Object> deletesToSet = (Map<String, Object>) data.get(1);
    List<Document> arrayFilters = (List<Document>) data.get(2);
    Bson query = QueryService.getQuery(ids);
    return databaseAdapter.deleteInArray(query, arraySize, deletesToSet, arrayFilters,
        getCollectionName(client, entity));
  }

  private Map<String, Object> getPrimaryFieldsValueMap(Metadata meta, String entity, Map<String, Object> doc) {
    List<String> primaryFields = meta.getEntity(entity).getPrimaryFieldsName();
    Set<String> globalFields = meta.getGlobalFieldsSet();
    Map<String, Object> primaryFieldsValue = new HashMap<>();
    globalFields.forEach(field -> {
      if (doc.containsKey(field)) {
        primaryFieldsValue.put(field, doc.get(field));
      } else {
        throw new NucleusException("Primary field(s) are missing.");
      }
    });
    primaryFields.forEach(field -> {
      if (doc.containsKey(field)) {
        primaryFieldsValue.put(field, doc.get(field));
      } else {
        throw new NucleusException("Primary field(s) are missing.");
      }
    });
    return primaryFieldsValue;
  }

  public String createEntity(String client, String entity, Map<String, Object> doc) {
    Metadata meta = metadataService.getMetadata(client);
    checkMandatoryFieldsExistence(meta, entity, client);

    addSerial(doc);
    List<AssociationUpdates> associationUpdates = metadataService.validateInput(doc, entity, client, meta);

    // check duplicate primary fields
    Map<String, Object> primaryFieldsValue = getPrimaryFieldsValueMap(meta, entity, doc);

    Bson query = QueryService.getQuery(primaryFieldsValue);
    List<Map<String, Object>> response = databaseAdapter.get(query, getCollectionName(client, entity));
    if (!response.isEmpty()) {
      throw new NucleusException("These primary field values already exists.");
    }

    Document document = new Document(doc);
    if (associationUpdates.isEmpty()) {
      return databaseAdapter.create(document, getCollectionName(client, entity));
    }
    return databaseAdapter.create(document, getCollectionName(client, entity), associationUpdates);
  }


  /*-----JSON APIs-----*/

  public String createJson(String client, Map<String, Object> doc, String environment, String localization) {
    checkMandatoryField(client, Fields.CLIENT);
    checkMandatoryField(environment, Fields.ENVIRONMENT);
    checkMandatoryField(localization, Fields.LOCALIZATION);
    checkJsonClientExists(client, environment, localization);

    Document document;
    if (doc == null) {
      document = new Document();
    } else {
      document = new Document(doc);
    }
    document.append(Fields.ENVIRONMENT, environment);
    document.append(Fields.LOCALIZATION, localization);
    document.append(Fields.CLIENT, client);
    return databaseAdapter.create(document, Fields.SIMPLE_CLIENT_DEFAULT_ENTITY);
  }

  public Long replaceJson(String client, String id, Map<String, Object> doc, String environment, String localization) {
    checkMandatoryField(client, Fields.CLIENT);
    checkMandatoryField(environment, Fields.ENVIRONMENT);
    checkMandatoryField(localization, Fields.LOCALIZATION);
    checkMandatoryField(doc, "doc");
    checkMandatoryField(id, "id");

    Document document = new Document(doc);
    document.append(Fields.ENVIRONMENT, environment);
    document.append(Fields.LOCALIZATION, localization);
    document.append(Fields.CLIENT, client);
    Bson query = QueryService.getQuery(client, Arrays.asList(id), environment, localization);
    return databaseAdapter.replaceOne(query, document, Fields.SIMPLE_CLIENT_DEFAULT_ENTITY);
  }

  public List<Map<String, Object>> getJson(String client, String environment, String localization,
      List<String> returnFields) {
    checkMandatoryField(client, Fields.CLIENT);

    Bson query = QueryService.getQuery(client, null, environment, localization);
    List<Map<String, Object>> documents = null;
    if (returnFields == null) {
      documents = databaseAdapter.get(query, Fields.SIMPLE_CLIENT_DEFAULT_ENTITY);
    } else {
      documents = databaseAdapter.get(query, returnFields, Fields.SIMPLE_CLIENT_DEFAULT_ENTITY);
    }
    return documents;
  }


  /*-----File APIs-----*/

  public String uploadFile(String client, String entity, String ids, String fieldname, MultipartFile file) {
    checkMandatoryFields(entity, client);
    checkMandatoryField(ids, "ids");

    String fileUrl = amazonS3Adapter.uploadFileTos3bucket(file);

    Map<String, Object> updates = new HashMap<>();
    updates.put(fieldname, fileUrl);

    Metadata meta = metadataService.getMetadata(client);
    List<Object> data = metadataService.convertInputUpdateToDbUpdates(updates, entity, client, meta);
    Map<String, Object> updatesToSet = (Map<String, Object>) data.get(1);
    List<Document> arrayFilters = (List<Document>) data.get(2);

    Bson query = QueryService.getQuery(client, Arrays.asList(ids.split(",")));
    Long success = databaseAdapter.update(query, updatesToSet, arrayFilters, getCollectionName(client, entity));
    if (success == 1) {
      return fileUrl;
    }
    return null;
  }

  public String uploadFileForNonMetaClient(String client, String ids, String fieldname, MultipartFile file) {
    checkMandatoryField(client, Fields.CLIENT);
    checkMandatoryField(ids, "id");

    String fileUrl = amazonS3Adapter.uploadFileTos3bucket(file);

    Map<String, Object> updates = new HashMap<>();
    updates.put(fieldname, fileUrl);

    Bson query = QueryService.getQuery(ids.split(","));
    Long success = databaseAdapter.update(query, updates, Fields.SIMPLE_CLIENT_DEFAULT_ENTITY);
    if (success == 1) {
      return fileUrl;
    }
    return null;
  }

}
