package com.nucleus.metadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonInclude(value = Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Metadata {

  String id;
  String client;
  String localization;
  List<Entity> entities;
  List<TypeDefinition> typeDefinitions;
  private List<Field> globalFields;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getClient() {
    return client;
  }

  public void setClient(String client) {
    this.client = client;
  }

  public String getLocalization() {
    return localization;
  }

  public void setLocalization(String localization) {
    this.localization = localization;
  }

  public List<Entity> getEntities() {
    return entities;
  }

  public void setEntities(List<Entity> entities) {
    this.entities = entities;
  }

  public List<TypeDefinition> getTypeDefinitions() {
    return typeDefinitions;
  }

  public void setTypeDefinitions(List<TypeDefinition> typeDefinitions) {
    this.typeDefinitions = typeDefinitions;
  }

  public List<Field> getGlobalFields() {
    return globalFields;
  }

  public void setGlobalFields(List<Field> globalFields) {
    this.globalFields = globalFields;
  }

  /* Helper methods */

  @JsonIgnore
  Map<String, TypeDefinition> typeDefinitionMap;
  @JsonIgnore
  Map<String, Entity> entityMap;
  @JsonIgnore
  Set<String> globalFieldsSet;

  @JsonIgnore
  public TypeDefinition getTypeDefinition(String typeName) {
    if (typeDefinitionMap == null) {
      typeDefinitionMap = new HashMap<String, TypeDefinition>();
      typeDefinitions.forEach(t -> typeDefinitionMap.put(t.getTypeName(), t));
    }
    return typeDefinitionMap.get(typeName);
  }

  @JsonIgnore
  public Entity getEntity(String entityName) {
    if (entityMap == null) {
      entityMap = new HashMap<String, Entity>();
      entities.forEach(e -> entityMap.put(e.getEntityName(), e));
    }
    return entityMap.get(entityName);
  }

  @JsonIgnore
  public Set<String> getGlobalFieldsSet() {
    if (globalFieldsSet == null) {
      globalFieldsSet = new HashSet<>();
      globalFields.forEach(f -> globalFieldsSet.add(f.getFieldName()));
    }
    return globalFieldsSet;
  }

}
