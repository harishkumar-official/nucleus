package com.nucleus.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mongodb.client.model.Filters;
import com.nucleus.constants.Fields;
import com.nucleus.database.CollectionName;
import com.nucleus.database.DatabaseAdapter;
import com.nucleus.exception.NucleusException;
import com.nucleus.metadata.Entity;
import com.nucleus.metadata.Metadata;

@Service
@SuppressWarnings("unchecked")
public class AssociationService {

  @Autowired
  DatabaseAdapter databaseAdapter;

  public void updateResponseWithAssociationData(List<Map<String, Object>> responses, String responseEntityName,
      Metadata meta) {
    Entity entity = meta.getEntity(responseEntityName);

    // Many to many
    Map<List<String>, String> manyToManyRefEntities = entity.getFieldToReferenceEntityMapForManyToMany();
    if (manyToManyRefEntities != null) {
      Map<Object, Map<String, Object>> responseIdMap = null;
      if (!manyToManyRefEntities.isEmpty()) {
        responseIdMap = new HashMap<Object, Map<String, Object>>();
        for (Map<String, Object> response : responses) {
          responseIdMap.put(response.get(Fields.ID), response);
        }
      }

      for (Entry<List<String>, String> e : manyToManyRefEntities.entrySet()) {
        List<String> associatedField = e.getKey();
        String associatedEntityName = e.getValue();
        updateWithManyToManyEntity(responseIdMap, responseEntityName, associatedField, associatedEntityName, meta);
      }
    }

    // Many to one
    Map<List<String>, String> manyToOneRefEntities = entity.getFieldToReferenceEntityMapForManyToOne();
    if (manyToOneRefEntities != null) {
      for (Entry<List<String>, String> e : manyToOneRefEntities.entrySet()) {
        List<String> associatedField = e.getKey();
        String associatedEntityName = e.getValue();
        updateWithManyToOneEntity(responses, associatedField, associatedEntityName, meta);
      }
    }
  }

  private void updateWithManyToManyEntity(Map<Object, Map<String, Object>> responseIdMap, String responseEntityName,
      List<String> associatedField, String associatedEntityName, Metadata meta) {

    Bson query = Filters.and(Filters.eq(Fields.ASS_NAME, Arrays.asList(responseEntityName, associatedEntityName)),
        Filters.in(Fields.ASS_MAPPING + "." + responseEntityName, responseIdMap.keySet()));
    Map<String, Object> association = databaseAdapter.get(query, CollectionName.association.name()).get(0);

    List<Map<String, Object>> responsesContainingAssociatedEntity = new ArrayList<Map<String, Object>>();
    List<String> associatedEntityIds = new ArrayList<String>();
    if (association != null) {
      for (Map<String, String> ass : (List<Map<String, String>>) association.get(Fields.ASS_MAPPING)) {
        associatedEntityIds.add(ass.get(associatedEntityName));
        responsesContainingAssociatedEntity.add(responseIdMap.get(ass.get(responseEntityName)));
      }
    }

    updateDb(associatedField, associatedEntityName, meta, associatedEntityIds, responsesContainingAssociatedEntity);
  }

  private void updateWithManyToOneEntity(List<Map<String, Object>> responses, List<String> associatedField,
      String associatedEntityName, Metadata meta) {
    List<Map<String, Object>> responsesContainingAssociatedEntity = new ArrayList<Map<String, Object>>();
    List<String> associatedEntityIds = new ArrayList<String>();
    for (Map<String, Object> response : responses) {
      String value = getNestedField(response, associatedField);
      if (value != null) {
        responsesContainingAssociatedEntity.add(response);
        associatedEntityIds.add(value);
      }
    }

    updateDb(associatedField, associatedEntityName, meta, associatedEntityIds, responsesContainingAssociatedEntity);
  }

  private void updateDb(List<String> associatedField, String associatedEntityName, Metadata meta,
      List<String> associatedEntityIds, List<Map<String, Object>> responsesContainingAssociatedEntity) {

    Bson query = QueryService.getQuery(associatedEntityIds);
    List<Map<String, Object>> associatedEntities = databaseAdapter.get(query, associatedEntityName);
    if (!associatedEntities.isEmpty()) {
      for (int i = 0; i < associatedEntityIds.size(); i++) {
        String id = associatedEntityIds.get(i).toString();
        Map<String, Object> associatedEntity =
            associatedEntities.stream().filter(e -> id.equals(e.get(Fields.ID))).findFirst().get();
        removeInternalParameters(associatedEntity);
        Map<String, Object> response = responsesContainingAssociatedEntity.get(i);
        setNestedField(response, associatedField, associatedEntity);
      }

      // update entities with their associations
      updateResponseWithAssociationData(associatedEntities, associatedEntityName, meta);
    }
  }

  private void removeInternalParameters(Map<String, Object> associatedEntity) {
    associatedEntity.remove(Fields.CLIENT);
    associatedEntity.remove(Fields.ENVIRONMENT);
    associatedEntity.remove(Fields.LOCALIZATION);
  }

  private void setNestedField(Map<String, Object> doc, List<String> associatedField,
      Map<String, Object> entityToSetInDoc) {
    Object temp = doc;
    int length = associatedField.size();

    // leaving out last field
    for (int i = 0; i < length - 1; i++) {
      String field = associatedField.get(i);
      temp = ((Map<String, Object>) temp).get(field);
      if (temp == null) {
        break;
      }
    }
    if (temp instanceof Map) {
      ((Map<String, Object>) temp).put(associatedField.get(length - 1), entityToSetInDoc);
    }
  }

  private String getNestedField(Map<String, Object> doc, List<String> associatedField) {
    Object value = doc;
    for (String field : associatedField) {
      value = ((Map<String, Object>) value).get(field);
      if (value == null) {
        return null;
      }
    }
    if (value instanceof String) {
      return (String) value;
    } else {
      throw new NucleusException("Invalid data.");
    }
  }

}
