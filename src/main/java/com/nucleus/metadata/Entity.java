package com.nucleus.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.*;
import java.util.Map.Entry;

@JsonInclude(value = Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.LowerCaseStrategy.class)
public class Entity {

  @JsonIgnore Set<String> fieldNameSet;
  @JsonIgnore Map<List<String>, String> fieldToReferenceEntityMapForManyToMany;
  @JsonIgnore Map<List<String>, String> fieldToReferenceEntityMapForManyToOne;
  @JsonIgnore List<String> primaryFieldsName;
  private Integer serial;
  private Long createDate;
  private Long updateDate;
  private String entityName;
  private String displayName;
  private String description;
  private List<Field> fields;

  private static Map<List<String>, String> getFieldToReferenceEntityMap(
      List<Field> fields, AssociationType associationType) {
    Map<List<String>, String> map = new HashMap<List<String>, String>();
    if (fields != null) {
      for (Field field : fields) {
        String assType = field.getAssociationType();
        if (assType != null && associationType.name().equals(assType)) {
          List<String> fieldsList = new ArrayList<>();
          fieldsList.add(0, field.getFieldName());
          map.put(fieldsList, field.getType());
        } else if (field.getFields() != null) {
          Map<List<String>, String> internalMap =
              getFieldToReferenceEntityMap(field.getFields(), associationType);
          for (Entry<List<String>, String> entry : internalMap.entrySet()) {
            List<String> fieldsList = entry.getKey();
            fieldsList.add(0, field.getFieldName());
            map.put(fieldsList, entry.getValue());
          }
        }
      }
    }
    return map;
  }

  public Integer getSerial() {
    return serial;
  }

  public void setSerial(Integer serial) {
    this.serial = serial;
  }

  public Long getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Long createDate) {
    this.createDate = createDate;
  }

  public Long getUpdateDate() {
    return updateDate;
  }

  public void setUpdateDate(Long updateDate) {
    this.updateDate = updateDate;
  }

  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  public String getDisplayName() {
    return displayName;
  }

  /* Helper methods */

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<Field> getFields() {
    return fields;
  }

  public void setFields(List<Field> fields) {
    this.fields = fields;
  }

  /** Returns entity fields name set. */
  public Set<String> getFieldNameSet() {
    if (fieldNameSet == null || fieldNameSet.isEmpty()) {
      fieldNameSet = new HashSet<String>();
      if (fields != null) {
        for (Field field : fields) {
          String fieldName = field.getFieldName();
          fieldNameSet.add(fieldName);
        }
      }
    }
    return fieldNameSet;
  }

  /** Returns the list of cascading fields in reverse order. */
  public Map<List<String>, String> getFieldToReferenceEntityMapForManyToMany() {
    if (fieldToReferenceEntityMapForManyToMany == null
        || fieldToReferenceEntityMapForManyToMany.isEmpty()) {
      fieldToReferenceEntityMapForManyToMany =
          getFieldToReferenceEntityMap(fields, AssociationType.many_to_many);
    }
    return fieldToReferenceEntityMapForManyToMany;
  }

  /** Returns the list of cascading fields in reverse order. */
  public Map<List<String>, String> getFieldToReferenceEntityMapForManyToOne() {
    if (fieldToReferenceEntityMapForManyToOne == null
        || fieldToReferenceEntityMapForManyToOne.isEmpty()) {
      fieldToReferenceEntityMapForManyToOne =
          getFieldToReferenceEntityMap(fields, AssociationType.many_to_one);
    }
    return fieldToReferenceEntityMapForManyToOne;
  }

  /** Returns primary fields name list. */
  public List<String> getPrimaryFieldsName() {
    if (primaryFieldsName == null || primaryFieldsName.isEmpty()) {
      primaryFieldsName = new ArrayList<>();
      if (fields != null) {
        for (Field field : fields) {
          String fieldName = field.getFieldName();
          FieldLevel fieldLevel = field.getFieldLevel();
          if (fieldLevel != null && fieldLevel.equals(FieldLevel.primary)) {
            primaryFieldsName.add(fieldName);
          }
        }
      }
    }
    return primaryFieldsName;
  }
}
