package com.nucleus.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.nucleus.constants.Environment;
import com.nucleus.constants.Fields;
import com.nucleus.exception.NucleusException;
import com.nucleus.metadata.Metadata;
import com.nucleus.service.DataService;

import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@Controller
@RequestMapping("/ui")
public class UiController {

  private DataService dataService;

  @Autowired
  public UiController(DataService partnerService) {
    this.dataService = partnerService;
  }

  /**
   * Returns login page.
   */
  @RequestMapping(value = "", method = RequestMethod.GET)
  public String start(ModelMap model, @ModelAttribute("error") String error, @ModelAttribute("tab") String tab) {
    model.put("error", error);
    model.put("tab", tab);
    return "login";
  }

  private ModelAndView redirect(ModelMap model, String client, boolean created) {
    List<Map<String, Object>> metadatas = dataService.getMetaData(client, null);
    if (metadatas.isEmpty()) {
      return new ModelAndView("redirect:/ui/simpledata", model);
    }
    if (created) {
      return new ModelAndView("redirect:/ui/metadata", model);
    } else {
      return new ModelAndView("redirect:/ui/appdata", model);
    }
  }

  /**
   * Returns data page after successful login.
   */
  @RequestMapping(value = "/login", method = RequestMethod.POST)
  public ModelAndView login(ModelMap model, @RequestParam String client) {
    model.put("client", client);

    boolean response = dataService.clientExists(client, null);
    if (response) {
      return redirect(model, client, false);
    }

    model.put("error", "Sorry, client '" + client + "' doesn't exists.");
    model.put("tab", "login");
    return new ModelAndView("redirect:/ui", model);
  }

  /**
   * Returns data page on successful client creation.
   */
  @RequestMapping(value = "/signup", method = RequestMethod.POST)
  public ModelAndView create(ModelMap model, @RequestParam String client, @RequestParam String localization,
      @RequestParam boolean metadata) {
    boolean response;
    if (metadata) {
      response = createMetaClient(client, localization);
    } else {
      response = createSimpleClient(client, localization);
    }

    model.put("client", client);
    if (response) {
      return redirect(model, client, true);
    }
    model.put("error", "Sorry, client '" + client + "/" + localization + "' already exists.");
    model.put("tab", "signup");
    return new ModelAndView("redirect:/ui", model);
  }

  private boolean createMetaClient(String client, String localization) {
    try {
      Map<String, Object> metadata = new HashMap<>();
      metadata.put(Fields.ENTITIES, new ArrayList<>());
      metadata.put(Fields.TYPE_DEFINITIONS, new ArrayList<>());
      dataService.createMetaData(client, localization, metadata);
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  private boolean createSimpleClient(String client, String localization) {
    try {
      dataService.createJson(client, null, Environment.development.name(), localization);
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  /**
   * Returns meta-client meta data html.
   */
  @RequestMapping(value = "/metadata", method = RequestMethod.GET)
  public String metadata(ModelMap model, @RequestParam String client) {
    List<Map<String, Object>> data = dataService.getMetaData(client, null);
    List<String> localizations = new ArrayList<>();

    model.put("metadata", mapMetaData(data, localizations));
    model.put("client", client);
    model.put("localizations", localizations);
    return "metadata";
  }

  /**
   * Returns meta-client meta-data.
   */
  @ResponseBody
  @RequestMapping(value = "/metadata/get", method = RequestMethod.GET)
  public Map<String, Object> metadata(@RequestParam String client) {
    List<Map<String, Object>> data = dataService.getMetaData(client, null);
    List<String> localizations = new ArrayList<>();

    return mapMetaData(data, localizations);
  }

  /**
   * Returns meta-client app data html.
   */
  @RequestMapping(value = "/appdata", method = RequestMethod.GET)
  public String appdata(ModelMap model, @RequestParam String client) {
    Metadata metadata = dataService.getMetaDataObject(client, null);
    if (metadata == null) {
      throw new NucleusException("Invalid Client");
    }
    String entity = metadata.getEntities().get(0).getEntityName();

    List<Map<String, Object>> data = dataService.getDocList(client, entity, metadata);

    model.put("metadata", metadata);
    model.put("appdata", data);
    model.put("entity", entity);
    model.put("client", client);
    return "appdata";
  }

  /**
   * Returns meta-client app data.
   */
  @ResponseBody
  @RequestMapping(value = "/appdata/get", method = RequestMethod.GET)
  public List<Map<String, Object>> appdata(@RequestParam String client, @RequestParam String entity,
      @RequestParam String queryFields) {
    return dataService.getEntities(client, entity, queryFields, null, false);
  }

  /**
   * Returns simple-client app data html.
   */
  @RequestMapping(value = "/simpledata", method = RequestMethod.GET)
  public String simpledata(ModelMap model, @RequestParam String client) {
    List<Map<String, Object>> data = null;
    data = dataService.getJson(client, null, null, null);
    Set<String> environments = new HashSet<>();
    Map<String, Set<String>> envLocMap = new HashMap<>();

    model.put("simpledata", mapData(data, environments, envLocMap));
    model.put("client", client);
    model.put("environments", environments);
    model.put("localization_map", envLocMap);
    return "simpledata";
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> mapData(List<Map<String, Object>> data, Set<String> environments,
      Map<String, Set<String>> envLocMap) {
    Map<String, Object> envLocDataMap = new HashMap<>();
    data.forEach(d -> {
      String environment = (String) d.get(Fields.ENVIRONMENT);
      String localization = (String) d.get(Fields.LOCALIZATION);
      environments.add(environment);

      Set<String> localizations;
      if (envLocMap.containsKey(environment)) {
        localizations = envLocMap.get(environment);
      } else {
        localizations = new HashSet<>();
        envLocMap.put(environment, localizations);
      }
      localizations.add(localization);

      Map<String, Object> localizationMap = null;
      if (envLocDataMap.containsKey(environment)) {
        localizationMap = (Map<String, Object>) envLocDataMap.get(environment);
      } else {
        localizationMap = new HashMap<>();
      }

      localizationMap.put(localization, d);
      envLocDataMap.put(environment, localizationMap);
    });
    return envLocDataMap;
  }

  private Map<String, Object> mapMetaData(List<Map<String, Object>> data, List<String> localizations) {
    Map<String, Object> locDataMap = new HashMap<>();
    data.forEach(d -> {
      String localization = (String) d.get(Fields.LOCALIZATION);
      localizations.add(localization);
      locDataMap.put(localization, d);
    });
    return locDataMap;
  }
}

