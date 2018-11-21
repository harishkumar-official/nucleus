package com.nucleus.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.util.StringUtils;

import com.mongodb.client.model.Filters;
import com.nucleus.constants.Fields;

public class QueryService {

  private QueryService() {}

  public static final String _ID = "_id";

  public static Bson getQuery(String client, String localization) {
    List<Bson> list = new ArrayList<Bson>();
    if (!StringUtils.isEmpty(client)) {
      list.add(Filters.eq(Fields.CLIENT, client));
    }
    if (!StringUtils.isEmpty(localization)) {
      list.add(Filters.eq(Fields.LOCALIZATION, localization));
    }
    return Filters.and(list);
  }

  public static Bson getQuery(String client, List<String> ids) {
    List<Bson> list = new ArrayList<Bson>();
    if (!StringUtils.isEmpty(client)) {
      list.add(Filters.eq(Fields.CLIENT, client));
    }
    if (ids != null) {
      list.add(getQuery(ids));
    }
    return Filters.and(list);
  }

  public static Bson getQuery(List<String> ids) {
    List<ObjectId> list = new ArrayList<>();
    for (String id : ids) {
      list.add(new ObjectId(id));
    }
    return Filters.in(_ID, list);
  }

  public static Bson getQuery(String[] ids) {
    List<ObjectId> list = new ArrayList<>();
    for (String id : ids) {
      list.add(new ObjectId(id));
    }
    return Filters.in(_ID, list);
  }

  public static Bson getQuery(Map<String, Object> queryFieldsMap) {
    List<Bson> list = new ArrayList<Bson>();
    queryFieldsMap.forEach((key, value) -> {
      if (key.equals(Fields.ID)) {
        list.add(Filters.eq(_ID, new ObjectId((String) value)));
      } else {
        list.add(Filters.eq(key, value));
      }
    });
    return Filters.and(list);
  }

  public static Bson getQuery(String client, List<String> ids, String environment, String localization) {
    List<Bson> list = new ArrayList<Bson>();
    if (!StringUtils.isEmpty(client)) {
      list.add(Filters.eq(Fields.CLIENT, client));
    }
    if (ids != null) {
      list.add(getQuery(ids));
    }
    if (!StringUtils.isEmpty(environment)) {
      list.add(Filters.eq(Fields.ENVIRONMENT, environment));
    }
    if (!StringUtils.isEmpty(localization)) {
      list.add(Filters.eq(Fields.LOCALIZATION, localization));
    }
    return Filters.and(list);
  }
}
