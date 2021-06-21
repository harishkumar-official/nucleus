package com.nucleus.controller;

import com.nucleus.constants.Fields;
import com.nucleus.exception.NucleusException;
import com.nucleus.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import springfox.documentation.annotations.ApiIgnore;

import java.util.*;

@ApiIgnore
@Controller
@RequestMapping("/ui")
public class UiController {

  private DataService dataService;

  @Autowired
  public UiController(DataService partnerService) {
    this.dataService = partnerService;
  }

  /** Returns login page. */
  @RequestMapping(value = "", method = RequestMethod.GET)
  public String start(
      ModelMap model,
      @ModelAttribute("error") String error,
      @ModelAttribute("tab") String tab,
      @ModelAttribute("ismeta") String ismeta) {
    model.put("error", error);
    model.put("tab", tab);
    model.put("ismeta", ismeta);
    return "login";
  }

  private ModelAndView redirect(ModelMap model, String client, boolean created) {
    model.put("client", client);
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

  /** Returns data page after successful login. */
  @RequestMapping(value = "/login", method = RequestMethod.POST)
  public ModelAndView login(ModelMap model, @RequestParam String client) {
    if (StringUtils.isEmpty(client)) {
      model.put("error", "Sorry, fields are missing.");
    } else {
      boolean response = dataService.clientExists(client);
      if (response) {
        return redirect(model, client, false);
      }
      model.put("error", "Sorry, client '" + client + "' doesn't exists.");
    }

    model.put("tab", "login");
    return new ModelAndView("redirect:/ui", model);
  }

  /** Returns data page on successful client creation. */
  @RequestMapping(value = "/signup", method = RequestMethod.POST)
  public ModelAndView create(
      ModelMap model,
      @RequestParam String client,
      @RequestParam boolean metadata,
      @RequestParam String localization,
      @RequestParam(required = false) String environment) {
    if (StringUtils.isEmpty(client) || StringUtils.isEmpty(localization)) {
      model.put("error", "Sorry, fields are missing.");
    } else if (dataService.clientExists(client)) {
      model.put("error", "Sorry, client already exists.");
    } else {
      boolean response;
      if (metadata) {
        response = createMetaClient(client, localization);
      } else {
        response = createSimpleClient(client, localization, environment);
      }
      if (response) {
        return redirect(model, client, true);
      }
      model.put("error", "Sorry, client '" + client + "/" + localization + "' already exists.");
    }

    if (!metadata) {
      model.put("ismeta", false);
    }
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

  private boolean createSimpleClient(String client, String localization, String environment) {
    if (!StringUtils.isEmpty(environment)) {
      try {
        dataService.createJson(client, null, environment, localization);
      } catch (Exception e) {
        return false;
      }
      return true;
    }
    return false;
  }

  /** Returns meta-client meta data html. */
  @RequestMapping(value = "/metadata", method = RequestMethod.GET)
  public String metadata(ModelMap model, @RequestParam String client) {
    addDataInModel(model, client);
    model.put("showAppButton", true);
    return "metadata";
  }

  /** Returns meta-client app-data html. */
  @RequestMapping(value = "/appdata", method = RequestMethod.GET)
  public String appdata(ModelMap model, @RequestParam String client) {
    addDataInModel(model, client);
    model.put("showMetaButton", true);
    return "appdata";
  }

  private void addDataInModel(ModelMap model, String client) {
    List<Map<String, Object>> metadatas = dataService.getMetaData(client, null);
    if (metadatas == null) {
      throw new NucleusException("Invalid Client");
    }

    List<String> localizations = new ArrayList<>();
    model.put("metadata", mapMetaData(metadatas, localizations));
    model.put("localizations", localizations);
    model.put("client", client);
  }

  /** Returns simple-client app data html. */
  @RequestMapping(value = "/simpledata", method = RequestMethod.GET)
  public String simpledata(ModelMap model, @RequestParam String client) {
    List<Map<String, Object>> data = dataService.getJson(client, null, null, null);
    if (data == null) {
      throw new NucleusException("Invalid Client");
    }
    Set<String> environments = new HashSet<>();
    Set<String> localizations = new HashSet<>();

    model.put("simpledata", mapSimpleData(data, environments, localizations));
    model.put("client", client);
    model.put("environments", environments);
    model.put("localizations", localizations);
    return "simpledata";
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> mapSimpleData(
      List<Map<String, Object>> data, Set<String> environments, Set<String> localizations) {
    Map<String, Object> envLocDataMap = new HashMap<>();
    data.forEach(
        d -> {
          String environment = (String) d.get(Fields.ENVIRONMENT);
          String localization = (String) d.get(Fields.LOCALIZATION);
          environments.add(environment);
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

  private Map<String, Object> mapMetaData(
      List<Map<String, Object>> data, List<String> localizations) {
    Map<String, Object> locDataMap = new HashMap<>();
    data.forEach(
        d -> {
          String localization = (String) d.get(Fields.LOCALIZATION);
          localizations.add(localization);
          locDataMap.put(localization, d);
        });
    return locDataMap;
  }
}
