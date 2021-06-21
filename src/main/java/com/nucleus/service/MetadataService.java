package com.nucleus.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nucleus.constants.Fields;
import com.nucleus.database.CollectionName;
import com.nucleus.database.DatabaseAdapter;
import com.nucleus.exception.NucleusException;
import com.nucleus.metadata.AssociationType;
import com.nucleus.metadata.Entity;
import com.nucleus.metadata.Field;
import com.nucleus.metadata.FieldLevel;
import com.nucleus.metadata.Metadata;
import com.nucleus.metadata.PrimaryType;
import com.nucleus.metadata.TypeDefinition;
import com.nucleus.transientmodel.AssociationUpdates;
import org.springframework.util.StringUtils;

@Service
@SuppressWarnings({"rawtypes", "unchecked"})
public class MetadataService {

  private static final String DOT = "\\.";
  private static final String COMMA = ",";

  private static ObjectMapper mapper = new ObjectMapper();

  private DatabaseAdapter database;

  @Autowired
  public MetadataService(DatabaseAdapter database) {
    this.database = database;
  }

  public Metadata getMetadata(String client) {
    return getMetadata(client, null);
  }

  public Metadata getMetadata(String client, String localization) {
    Metadata meta = null;
    List<Map<String, Object>> metadatas =
        database.get(QueryService.getQuery(client, localization), CollectionName.metadata.name());
    if (!metadatas.isEmpty()) {
      try {
        Map<String, Object> metaDoc = metadatas.get(0);
        String metaJson = mapper.writeValueAsString(metaDoc);
        meta = mapper.readValue(metaJson, Metadata.class);
      } catch (Exception e) {
        throw new NucleusException(e);
      }
    }
    return meta;
  }

  private void validateAllowedName(String name) {
    if (CHAR_LIST.contains(name.charAt(0))) {
      throw new NucleusException("Name shouldn't start with a number.");
    } else if (name.contains(" ")) {
      throw new NucleusException("Name shouldn't contain a space.");
    } else if (name.contains("-")) {
      throw new NucleusException("Name shouldn't contain a hyphen. Rather use _underscore");
    }
  }

  private void validateEntity(Metadata meta, Map<String, Object> newMeta) {
    if (meta != null) {
      int entitySize = meta.getEntities().size();
      List<Map<String, Object>> entities = (List<Map<String, Object>>) newMeta.get(Fields.ENTITIES);
      if (entities != null) {
        for (Map<String, Object> newEntity: entities) {
          String entityName = (String) newEntity.get(Fields.ENTITY_NAME);
          validatePrimaryFields((List<Map<String, Object>>) newEntity.get(Fields.FIELDS));
          if (entityName == null) {
            Integer serial = (Integer) newEntity.get(Fields.SERIAL);
            Entity existingEntity =
                meta.getEntities().stream().filter(en -> en.getSerial().equals(serial)).findFirst().orElse(null);
            if (existingEntity == null) {
              throw new NucleusException("Entity with serial'" + serial + "' doesn't exists.");
            } else {
              validateMetaFields(existingEntity.getEntityName(), existingEntity.getFields(),
                  (List<Map<String, Object>>) newEntity.get(Fields.FIELDS));
            }
          } else {
            validateUniqueName(meta, entityName);
            validateAllowedName(entityName);
            newEntity.put(Fields.SERIAL, ++entitySize);
          }
        }
      }
    }
  }

  private void validateTypeDefinition(Metadata meta, Map<String, Object> newMeta) {
    if (meta != null) {
      int typeSize = meta.getTypeDefinitions().size();
      List<Map<String, Object>> typeDefinitions = (List<Map<String, Object>>) newMeta.get(Fields.TYPE_DEFINITIONS);
      if (typeDefinitions != null) {
        for (Map<String, Object> newType: typeDefinitions) {
          String typeName = (String) newType.get(Fields.TYPE_NAME);
          validateTypeDefinitionFields(meta, (List<Map<String, Object>>) newType.get(Fields.FIELDS));
          if (typeName == null) {
            Integer serial = (Integer) newType.get(Fields.SERIAL);
            TypeDefinition existingType =
                meta.getTypeDefinitions().stream().filter(en -> en.getSerial().equals(serial)).findFirst().orElse(null);
            if (existingType == null) {
              throw new NucleusException("Type with serial'" + serial + "' doesn't exists.");
            } else {
              validateMetaFields(existingType.getTypeName(), existingType.getFields(),
                  (List<Map<String, Object>>) newType.get(Fields.FIELDS));
            }
          } else {
            validateUniqueName(meta, typeName);
            validateAllowedName(typeName);
            newType.put(Fields.SERIAL, ++typeSize);
          }
        }
      }
    }
  }

  private boolean validateUniqueName(Metadata meta, String name) {
    boolean isNotInTypeDefinitions = meta.getTypeDefinition(name) == null;
    boolean isNotInEntities = meta.getEntity(name) == null;
    if (isNotInTypeDefinitions && isNotInEntities) {
      return true;
    } else if (isNotInTypeDefinitions) {
      throw new NucleusException("Entity with name '" + name + "' already exists.");
    }
    throw new NucleusException("Type with name '" + name + "' already exists.");
  }

  private void validatePrimaryFields(List<Map<String, Object>> newFields) {
    if (newFields != null) {
      for (Map<String, Object> field : newFields) {
        String fieldLevel = (String) field.get(Fields.FIELD_LEVEL);
        String type = (String) field.get(Fields.TYPE);
        String subType = (String) field.get(Fields.SUB_TYPE);
        if (FieldLevel.primary.name().equals(fieldLevel)
            && (!isFieldTypeForPrimaryLevel(type) || StringUtils.hasLength(subType))) {
          throw new NucleusException("Allowed Primary fields are [\"string\", \"number\"]");
        }
      }
    }
  }

  private void validateTypeDefinitionFields(Metadata meta, List<Map<String, Object>> newFields) {
    if (newFields != null) {
      for (Map<String, Object> field : newFields) {
        String type = (String) field.get(Fields.TYPE);
        String subType = (String) field.get(Fields.SUB_TYPE);
        if (meta.getEntity(type) != null) {
          throw new NucleusException("Type definitions can't have Entity as a field");
        }
      }
    }
  }

  private void validateMetaFields(String name, List<Field> existingSubFields, List<Map<String, Object>> newSubFields) {
    if (newSubFields != null && existingSubFields != null) {
      int fieldsSize = existingSubFields.size();
      for (Map<String, Object> newSubField: newSubFields) {
        Field existingSubField = null;
        String fieldName = (String) newSubField.get(Fields.FIELD_NAME);
        if (fieldName == null) {
          Integer serial = (Integer) newSubField.get(Fields.SERIAL);
          existingSubField =
              existingSubFields.stream().filter(e -> e.getSerial().equals(serial)).findFirst().orElse(null);
        } else {
          existingSubField =
              existingSubFields.stream().filter(e -> e.getFieldName().equals(fieldName)).findFirst().orElse(null);
          if (existingSubField == null) {
            validateAllowedName(fieldName);
            int serial = ++fieldsSize;
            newSubField.put(Fields.SERIAL, serial);
          }
        }

        if (newSubField.containsKey(Fields.FIELDS) && existingSubField != null
            && existingSubField.getFields() != null) {
          validateMetaFields(existingSubField.getFieldName(), existingSubField.getFields(),
              (List<Map<String, Object>>) newSubField.get(Fields.FIELDS));
        } else if (!newSubField.containsKey(Fields.FIELDS) && existingSubField != null) {
          throw new NucleusException("Field '" + fieldName + "' already exists inside '" + name + "'.");
        }
      }
    }
  }

  private void validateMetaFields(Metadata meta, Map<String, Object> newMeta) {
    validateEntity(meta, newMeta);
    validateTypeDefinition(meta, newMeta);
  }

  /**
   * Meta-data validation.
   */
  public String validateMetadata(Metadata metadata, Map<String, Object> newMetadataParameters, boolean validateFields) {
    if (validateFields) {
      validateMetaFields(metadata, newMetadataParameters);
    }
    Metadata meta = null;
    String beforeJson = null;
    String afterJson = null;
    String afterJsonFinal = null;
    try {
      beforeJson = mapper.writeValueAsString(newMetadataParameters);
      meta = mapper.readValue(beforeJson, Metadata.class);
      afterJson = mapper.writeValueAsString(meta);
      if (meta.getEntities() == null) {
        meta.setEntities(new ArrayList<>());
      }
      if (meta.getTypeDefinitions() == null) {
        meta.setTypeDefinitions(new ArrayList<>());
      }
      afterJsonFinal = mapper.writeValueAsString(meta);
    } catch (Exception e) {
      throw new NucleusException("Wrong json format.", e);
    }
    if (beforeJson.replace("\"", "").length() == afterJson.replace("\"", "").length()) {
      return afterJsonFinal;
    } else {
      throw new NucleusException("Wrong format of metadata or it contains invalid fields.");
    }
  }

  private List<Field> includeGlobalFields(Entity entity, Metadata meta) {
    List<Field> fields = new ArrayList<>();
    if (entity.getFields() != null) {
      fields.addAll(entity.getFields());
    }
    if (meta.getGlobalFields() != null) {
      fields.addAll(meta.getGlobalFields());
    }
    return fields;
  }

  private Set<String> includeGlobalFieldsName(Entity entity, Metadata meta) {
    Set<String> fields = new HashSet<>();
    if (entity.getFieldNameSet() != null) {
      fields.addAll(entity.getFieldNameSet());
    }
    if (meta.getGlobalFieldsSet() != null) {
      fields.addAll(meta.getGlobalFieldsSet());
    }
    return fields;
  }

  /**
   * Data validation.
   */
  public List<AssociationUpdates> validateInput(Map<String, Object> input, String entityName, String client,
      Metadata meta) {
    try {
      Entity entity = meta.getEntity(entityName);
      List<AssociationUpdates> associationUpdates = new ArrayList<AssociationUpdates>();
      List<Field> fields = includeGlobalFields(entity, meta);
      Set<String> fieldNameSet = includeGlobalFieldsName(entity, meta);
      validateInput(input, fields, fieldNameSet, meta, associationUpdates, entityName);
      return associationUpdates;
    } catch (RuntimeException e) {
      throw new NucleusException(e);
    }
  }

  private void validateInput(Map<String, Object> input, List<Field> fields, Set<String> fieldNameSet, Metadata meta,
      List<AssociationUpdates> associationUpdates, String parentEntityName) {
    validateFieldsName(input, fieldNameSet);
    if (fields != null) {
      for (Field field : fields) {
        String fieldName = field.getFieldName();
        Object val = input.get(fieldName);

        if (val != null) {
          String fieldType = field.getType();

          if (fieldType.equals(PrimaryType.OBJECT)) {
            validateInput((Map<String, Object>) val, field.getFields(), field.getFieldNameSet(), meta,
                associationUpdates, parentEntityName);
          } else if (PrimaryType.SET.contains(fieldType)) {
            input.put(fieldName, cast(val, fieldType, field, meta, associationUpdates, parentEntityName));
          } else {
            TypeDefinition typeDefinition = meta.getTypeDefinition(fieldType);
            if (typeDefinition == null) {
              Entity entity = meta.getEntity(fieldType);
              if (entity == null) {
                throw new NucleusException("Not a valid type: " + fieldType);
              }
              String assType = field.getAssociationType();
              if (assType != null && AssociationType.many_to_many.name().equals(assType)) {
                field.setSubType(PrimaryType.STRING);
                List<Map<String, Object>> refEntityIdsMap = (List<Map<String, Object>>) cast(val, PrimaryType.ARRAY,
                    field, meta, associationUpdates, parentEntityName);
                updateAssociation(parentEntityName, fieldType, refEntityIdsMap, associationUpdates);
                input.remove(fieldName);
              } else {
                input.put(fieldName, cast(val, PrimaryType.STRING, field, meta, associationUpdates, parentEntityName));
              }
            } else {
              validateInput((Map<String, Object>) val, typeDefinition.getFields(), typeDefinition.getFieldNameSet(),
                  meta, associationUpdates, parentEntityName);
            }
          }
        }
      }
    }
  }

  private void updateAssociation(String parentEntityName, String refEntityName,
      List<Map<String, Object>> refEntityIdsMap, List<AssociationUpdates> associationUpdates) {
    refEntityIdsMap.forEach(entry -> {
      String value = (String) entry.get(Fields.VALUE);
      if (value.contains(COMMA)) {
        String[] values = value.split(COMMA);
        entry.put(Fields.VALUE, values[0]);
        entry.put(Fields.PREVIOUS_VALUE, values[1]);
      }
    });
    AssociationUpdates update = new AssociationUpdates();
    update.setParentEntityName(parentEntityName);
    update.setRefEntityName(refEntityName);
    update.setRefEntityIdsMapList(refEntityIdsMap);
    associationUpdates.add(update);
  }

  private boolean validateFieldsName(Map<String, Object> input, Set<String> fieldNameList) {
    fieldNameList.add(Fields.SERIAL);
    if (fieldNameList == null || !fieldNameList.containsAll(input.keySet())) {
      throw new NucleusException("Please check your fields " + input.keySet() + ", some of them don't exist.");
    }
    return true;
  }

  private Object parseString(Object value, String fieldType) {
    if (value instanceof String) {
      String val = ((String) value).trim();
      switch (fieldType) {
        case PrimaryType.FLOAT:
          return Float.parseFloat(val);
        case PrimaryType.DOUBLE:
          return Double.parseDouble(val);
        case PrimaryType.INTEGER:
          return Integer.parseInt(val);
        case PrimaryType.LONG:
        case PrimaryType.DATE:
          return Long.parseLong(val);
        case PrimaryType.BOOLEAN:
          return Boolean.parseBoolean(val);
        default:
          return value;
      }
    }
    return value;
  }

  private boolean isFieldTypeForPrimaryLevel(String fieldType) {
    switch (fieldType) {
      case PrimaryType.STRING:
      case PrimaryType.FLOAT:
      case PrimaryType.DOUBLE:
      case PrimaryType.INTEGER:
      case PrimaryType.LONG:
        return true;
      default:
        return false;
    }
  }

  private Object cast(Object value, String fieldType, Field field, Metadata meta,
      List<AssociationUpdates> associationUpdates, String parentEntityName) {
    value = parseString(value, fieldType);
    try {
      switch (fieldType) {
        case PrimaryType.FILE:
        case PrimaryType.STRING:
          return (String) value;
        case PrimaryType.FLOAT:
          return ((Number) value).floatValue();
        case PrimaryType.DOUBLE:
          return ((Number) value).doubleValue();
        case PrimaryType.INTEGER:
          return ((Number) value).intValue();
        case PrimaryType.LONG:
        case PrimaryType.DATE:
          return ((Number) value).longValue();
        case PrimaryType.BOOLEAN:
          return (Boolean) value;
        case PrimaryType.ENUM: {
          String enumVal = (String) value;
          Set<String> enumValues = field.getValues();
          if (!enumValues.contains(enumVal)) {
            throw new NucleusException("The enum value [" + enumVal + "] is not defined.");
          }
          return enumVal;
        }
        case PrimaryType.ARRAY: {
          String subType = field.getSubType();
          if (subType.equals(PrimaryType.OBJECT)) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) value;
            list.forEach(e -> validateInput(e, field.getFields(), field.getFieldNameSet(), meta, associationUpdates,
                parentEntityName));
            return value;
          } else if (PrimaryType.SET.contains(subType)) {
            List<Map<String, Object>> list = (List) value;
            list.forEach(elem -> {
              Object serial = elem.get(Fields.SERIAL);
              if (serial != null) {
                elem.put(Fields.SERIAL, cast(serial, PrimaryType.INTEGER, null, null, null, parentEntityName));
              }
              if (elem.containsKey(Fields.VALUE)) {
                Object val = elem.get(Fields.VALUE);
                elem.put(Fields.VALUE, cast(val, subType, null, null, null, parentEntityName));
              }
              if (elem.containsKey(Fields.PREVIOUS_VALUE)) {
                Object prev_val = elem.get(Fields.PREVIOUS_VALUE);
                elem.put(Fields.PREVIOUS_VALUE, cast(prev_val, subType, null, null, null, parentEntityName));
              }
            });
            return list;
          } else {
            TypeDefinition typeDefinition = meta.getTypeDefinition(subType);
            List<Map<String, Object>> list = (List<Map<String, Object>>) value;
            list.forEach(e -> validateInput(e, typeDefinition.getFields(), typeDefinition.getFieldNameSet(), meta,
                associationUpdates, parentEntityName));
            return value;
          }
        }
        default:
          return (Map<String, Object>) value;
      }
    } catch (ClassCastException e) {
      throw new NucleusException("Field: " + field.getFieldName() + " value: " + value + " is not of right type.", e);
    }
  }

  private static final Set<Character> CHAR_LIST = new HashSet<Character>();

  static {
    CHAR_LIST.add('0');
    CHAR_LIST.add('1');
    CHAR_LIST.add('2');
    CHAR_LIST.add('3');
    CHAR_LIST.add('4');
    CHAR_LIST.add('5');
    CHAR_LIST.add('6');
    CHAR_LIST.add('7');
    CHAR_LIST.add('8');
    CHAR_LIST.add('9');
  }

  private static final String PART1 = "$[e";
  private static final String PART2 = "]";
  private static final String PART3 = "e";
  private static final String PART4 = "." + Fields.SERIAL;

  public List<Object> convertInputUpdateToDbUpdates(Map<String, Object> updates, String entityName, String client,
      Metadata meta) {
    Entity entity = meta.getEntity(entityName);
    List<AssociationUpdates> associationUpdates = new ArrayList<AssociationUpdates>();
    Map<String, Object> updatesToSet = new HashMap<String, Object>();
    List<Document> arrayFilters = new ArrayList<Document>();

    List<Field> fields = includeGlobalFields(entity, meta);
    Set<String> fieldNameSet = includeGlobalFieldsName(entity, meta);

    WrapInt index = new WrapInt();
    index.value = 1;
    for (String fullFieldName : updates.keySet()) {
      String fullFieldToSetString = convertFieldToDbField(fullFieldName, arrayFilters, index);

      Map<String, Object> doc = new HashMap<>();
      convertToDoc(fullFieldName, updates.get(fullFieldName), doc);

      validateInput(doc, fields, fieldNameSet, meta, associationUpdates, entityName);
      if (!doc.isEmpty()) {
        Object verifiedValue = getVerifiedValue(fullFieldName, doc);
        updatesToSet.put(fullFieldToSetString, verifiedValue);
      }
    }
    return Arrays.asList(associationUpdates, updatesToSet, arrayFilters);
  }

  class WrapInt {
    int value;
  }

  public List<Object> convertMetaUpdateToDbUpdates(Metadata meta, Map<String, Object> updates, boolean validateFields) {
    Map<String, Object> updatesToSet = new HashMap<String, Object>();
    List<Document> arrayFilters = new ArrayList<Document>();

    WrapInt index = new WrapInt();
    index.value = 1;
    for (String fullFieldName : updates.keySet()) {
      String fullFieldToSetString = convertFieldToDbField(fullFieldName, arrayFilters, index);

      Map<String, Object> doc = new HashMap<>();
      convertToDoc(fullFieldName, updates.get(fullFieldName), doc);

      validateMetadata(meta, doc, validateFields);
      Object verifiedValue = getVerifiedValue(fullFieldName, doc);

      updatesToSet.put(fullFieldToSetString, verifiedValue);
    }
    return Arrays.asList(updatesToSet, arrayFilters);
  }

  private String convertFieldToDbField(String fullFieldName, List<Document> arrayFilters, WrapInt index) {
    String[] fields = fullFieldName.split(DOT);
    StringBuilder fullFieldToSet = new StringBuilder();
    for (int j = 0; j < fields.length; j++) {
      String curr = fields[j];
      if (CHAR_LIST.contains(curr.charAt(0))) {
        fullFieldToSet.append(PART1).append(index.value).append(PART2).append('.');
        Document doc = new Document();
        doc.append(PART3 + index.value + PART4, Integer.parseInt(curr)); // eg: e1.serial = 5
        arrayFilters.add(doc);
        index.value++;
      } else {
        fullFieldToSet.append(curr).append('.');
      }
    }
    fullFieldToSet.setLength(fullFieldToSet.length() - 1);
    return fullFieldToSet.toString();
  }

  private void convertToDoc(String key, Object value, Object doc) {
    int index = key.indexOf('.');
    if (index == -1) {
      ((Map<String, Object>) doc).put(key, value);
    } else {
      String outerKey = key.substring(0, index);
      String innerKey = key.substring(index + 1);
      if (doc instanceof Map) {
        Map<String, Object> tempDoc = (Map<String, Object>) doc;
        if (CHAR_LIST.contains(innerKey.charAt(0))) {
          if (tempDoc.containsKey(outerKey)) {
            List<Map<String, Object>> innerList = (List<Map<String, Object>>) tempDoc.get(outerKey);
            convertToDoc(innerKey, value, innerList);
          } else {
            List<Map<String, Object>> innerList = new ArrayList<>();
            tempDoc.put(outerKey, innerList);
            convertToDoc(innerKey, value, innerList);
          }
        } else {
          if (tempDoc.containsKey(outerKey)) {
            Map<String, Object> innerdoc = (Map<String, Object>) tempDoc.get(outerKey);
            convertToDoc(innerKey, value, innerdoc);
          } else {
            Map<String, Object> innerdoc = new HashMap<>();
            tempDoc.put(outerKey, innerdoc);
            convertToDoc(innerKey, value, innerdoc);
          }
        }
      } else {
        Integer serial = Integer.parseInt(outerKey);
        List tempList = (List) doc;
        Map<String, Object> data = getElement(serial, tempList);
        convertToDoc(innerKey, value, data);
      }
    }
  }

  private Map<String, Object> getElement(Integer serial, List<Map<String, Object>> list) {
    for (Map<String, Object> data : list) {
      if (serial == (Integer) data.get(Fields.SERIAL)) {
        return data;
      }
    }
    Map<String, Object> data = new HashMap<>();
    data.put(Fields.SERIAL, serial);
    list.add(data);
    return data;
  }

  private Object getVerifiedValue(String field, Object doc) {
    Object verifiedValue = doc;
    for (String key : field.split(DOT)) {
      if (CHAR_LIST.contains(key.charAt(0))) {
        verifiedValue = getElement(Integer.parseInt(key), (List) verifiedValue);
      } else {
        verifiedValue = ((Map<String, Object>) verifiedValue).get(key);
      }
    }
    return verifiedValue;
  }

}
