package com.nucleus.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JsonInclude(value = Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.LowerCaseStrategy.class)
public class Field {

  /* Helper methods */
  @JsonIgnore Set<String> fieldNameSet;
  private Integer serial;
  private Long createDate;
  private Long updateDate;
  private String fieldName;
  private String displayName;
  private String description;
  private String type;
  private String subType; // Needed when type is an Array.
  @JsonProperty("association_type") // Needed when type is an Entity.
  private String associationType;
  private Set<String> values; // Needed when type is an Enum.
  private Boolean required;
  private FieldLevel fieldLevel;
  private List<Field> fields;

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

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getSubType() {
    return subType;
  }

  public void setSubType(String subType) {
    this.subType = subType;
  }

  public String getAssociationType() {
    return associationType;
  }

  public void setAssociationType(String associationType) {
    this.associationType = associationType;
  }

  public Set<String> getValues() {
    return values;
  }

  public void setValues(Set<String> values) {
    this.values = values;
  }

  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public FieldLevel getFieldLevel() {
    return fieldLevel;
  }

  public void setFieldLevel(FieldLevel fieldLevel) {
    this.fieldLevel = fieldLevel;
  }

  public List<Field> getFields() {
    return fields;
  }

  public void setFields(List<Field> fields) {
    this.fields = fields;
  }

  /** Returns field sub-fields name set. */
  public Set<String> getFieldNameSet() {
    if (fieldNameSet == null) {
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
}
