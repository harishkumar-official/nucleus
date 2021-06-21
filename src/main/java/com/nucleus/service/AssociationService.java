package com.nucleus.service;

import com.mongodb.client.model.Filters;
import com.nucleus.constants.Fields;
import com.nucleus.database.CollectionName;
import com.nucleus.database.DatabaseAdapter;
import com.nucleus.exception.NucleusException;
import com.nucleus.metadata.AssociationType;
import com.nucleus.metadata.Entity;
import com.nucleus.metadata.Metadata;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Service
@SuppressWarnings("unchecked")
public class AssociationService {

  @Autowired DatabaseAdapter databaseAdapter;

  public void updateResponseWithAssociationData(
      List<Map<String, Object>> responses,
      String responseEntityName,
      Metadata meta,
      String client) {
    Entity entity = meta.getEntity(responseEntityName);

    // Many to many
    Map<List<String>, String> manyToManyRefEntities =
        entity.getFieldToReferenceEntityMapForManyToMany();
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
        updateWithManyToManyEntity(
            responseIdMap, responseEntityName, associatedField, associatedEntityName, meta, client);
      }
    }

    // Many to one
    Map<List<String>, String> manyToOneRefEntities =
        entity.getFieldToReferenceEntityMapForManyToOne();
    if (manyToOneRefEntities != null) {
      for (Entry<List<String>, String> e : manyToOneRefEntities.entrySet()) {
        List<String> associatedField = e.getKey();
        String associatedEntityName = e.getValue();
        updateWithManyToOneEntity(responses, associatedField, associatedEntityName, meta, client);
      }
    }
  }

  private void updateWithManyToManyEntity(
      Map<Object, Map<String, Object>> responseIdMap,
      String responseEntityName,
      List<String> associatedField,
      String associatedEntityName,
      Metadata meta,
      String client) {

    Bson query =
        Filters.and(
            Filters.in(Fields.ASS_MAPPING, responseIdMap.keySet()),
            Filters.eq(Fields.ASS_NAME, responseEntityName),
            Filters.eq(Fields.ASS_NAME, associatedEntityName));

    List<Map<String, Object>> associations =
        databaseAdapter.get(query, CollectionName.association.name());

    List<Map<String, Object>> responsesContainingAssociatedEntity =
        new ArrayList<Map<String, Object>>();
    List<String> associatedEntityIds = new ArrayList<String>();
    if (!associations.isEmpty()) {
      for (Map<String, Object> ass : associations) {
        List<String> mapping = (List<String>) ass.get(Fields.ASS_MAPPING);
        if (responseIdMap.containsKey(mapping.get(0))) {
          responsesContainingAssociatedEntity.add(responseIdMap.get(mapping.get(0)));
          associatedEntityIds.add(mapping.get(1));
        } else {
          responsesContainingAssociatedEntity.add(responseIdMap.get(mapping.get(1)));
          associatedEntityIds.add(mapping.get(0));
        }
      }
    }

    updateDb(
        associatedField,
        associatedEntityName,
        meta,
        associatedEntityIds,
        responsesContainingAssociatedEntity,
        client,
        AssociationType.many_to_many);
  }

  private void updateWithManyToOneEntity(
      List<Map<String, Object>> responses,
      List<String> associatedField,
      String associatedEntityName,
      Metadata meta,
      String client) {
    List<Map<String, Object>> responsesContainingAssociatedEntity =
        new ArrayList<Map<String, Object>>();
    List<String> associatedEntityIds = new ArrayList<String>();
    for (Map<String, Object> response : responses) {
      String value = getNestedField(response, associatedField);
      if (value != null) {
        responsesContainingAssociatedEntity.add(response);
        associatedEntityIds.add(value);
      }
    }

    updateDb(
        associatedField,
        associatedEntityName,
        meta,
        associatedEntityIds,
        responsesContainingAssociatedEntity,
        client,
        AssociationType.many_to_one);
  }

  private void updateDb(
      List<String> associatedField,
      String associatedEntityName,
      Metadata meta,
      List<String> associatedEntityIds,
      List<Map<String, Object>> responsesContainingAssociatedEntity,
      String client,
      AssociationType assType) {

    Bson query = QueryService.getQuery(associatedEntityIds);
    List<Map<String, Object>> associatedEntities =
        databaseAdapter.get(query, getCollectionName(client, associatedEntityName));
    if (!associatedEntities.isEmpty()) {
      for (int i = 0; i < associatedEntityIds.size(); i++) {
        String id = associatedEntityIds.get(i).toString();
        Map<String, Object> associatedEntity =
            associatedEntities.stream().filter(e -> id.equals(e.get(Fields.ID))).findFirst().get();
        removeGlobalFields(associatedEntity, meta);
        Map<String, Object> response = responsesContainingAssociatedEntity.get(i);
        setNestedField(response, associatedField, associatedEntity, assType);
      }

      // update entities with their associations
      updateResponseWithAssociationData(associatedEntities, associatedEntityName, meta, client);
    }
  }

  private void removeGlobalFields(Map<String, Object> associatedEntity, Metadata meta) {
    meta.getGlobalFieldsSet().forEach(fieldname -> associatedEntity.remove(fieldname));
  }

  private void setNestedField(
      Map<String, Object> doc,
      List<String> associatedField,
      Map<String, Object> entityToSetInDoc,
      AssociationType assType) {
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
      if (assType.equals(AssociationType.many_to_one)) {
        ((Map<String, Object>) temp).put(associatedField.get(length - 1), entityToSetInDoc);
      } else {
        addInArrayField(
            (Map<String, Object>) temp, associatedField.get(length - 1), entityToSetInDoc);
      }
    }
  }

  private void addInArrayField(Map<String, Object> object, String fieldName, Object fieldValue) {
    if (!object.containsKey(fieldName)) {
      object.put(fieldName, new ArrayList<>());
    }
    List<Object> valueArray = (List<Object>) object.get(fieldName);
    valueArray.add(fieldValue);
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

  private String getCollectionName(String client, String entity) {
    return new StringBuilder(client).append("_").append(entity).toString();
  }
}
