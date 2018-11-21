package com.nucleus.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nucleus.service.DataService;
import com.nucleus.transientmodel.DeleteRequest;
import com.nucleus.transientmodel.PrimaryFieldsRequest;
import com.nucleus.transientmodel.SetDefaultRequest;
import com.nucleus.transientmodel.UpdateRequest;

import io.swagger.annotations.Api;

@RestController
@CrossOrigin("*")
@RequestMapping("v1/client")
@Api(tags = "Meta Client", description = "client operations with meta-data capability")
public class ClientController {

  private DataService dataService;

  @Autowired
  public ClientController(DataService dataService) {
    this.dataService = dataService;
  }

  /* Comma separated ids */
  @RequestMapping(value = "/{client}/entity/{entity}/upload", method = RequestMethod.POST)
  public String upload(@PathVariable String client, @PathVariable String entity, @RequestPart String ids,
      @RequestPart String fieldname, @RequestPart MultipartFile file) throws IOException {
    return dataService.uploadFile(client, entity, ids, fieldname, file);
  }

  @RequestMapping(value = "/{client}/entity/{entity}/create", method = RequestMethod.POST)
  public String create(@PathVariable String client, @PathVariable String entity, @RequestBody Document doc) {
    return dataService.createEntity(client, entity, doc);
  }

  @RequestMapping(value = "/{client}/entity/{entity}/update", method = RequestMethod.PUT)
  public Long update(@PathVariable String client, @PathVariable String entity, @RequestBody UpdateRequest request) {
    return dataService.updateEntities(client, entity, request.getIds(), request.getUpdates());
  }

  @RequestMapping(value = "/{client}/entity/{entity}/primary/update", method = RequestMethod.PUT)
  public Long primaryFieldsUpdate(@PathVariable String client, @PathVariable String entity,
      @RequestBody PrimaryFieldsRequest request) {
    return dataService.updatePrimaryFields(client, entity, request.getId(), request.getUpdates());
  }

  @RequestMapping(value = "/{client}/entity/{entity}/element/add", method = RequestMethod.PUT)
  public Long add(@PathVariable String client, @PathVariable String entity, @RequestBody UpdateRequest request) {
    return dataService.addInEntityArray(client, entity, request.getIds(), request.getUpdates());
  }

  @RequestMapping(value = "/{client}/entity/{entity}/element/delete", method = RequestMethod.PUT)
  public Long delete(@PathVariable String client, @PathVariable String entity, @RequestBody DeleteRequest request) {
    return dataService.deleteInEntityArray(client, entity, request.getIds(), request.getUpdates(),
        request.getArraySize());
  }

  @RequestMapping(value = "/{client}/entity/{entity}/set/default", method = RequestMethod.PUT)
  public boolean setDefault(@PathVariable String client, @PathVariable String entity,
      @RequestBody SetDefaultRequest request) {
    return dataService.setDefault(client, entity, request.getId(), request.isDefaultDoc());
  }

  /**
   * Returns list of client data documents.
   */
  @RequestMapping(value = "/{client}/entity/{entity}/get", method = RequestMethod.GET, produces = "application/json")
  public List<Map<String, Object>> get(@PathVariable String client, @PathVariable String entity,
      @RequestParam String query, @RequestParam(required = false, value = "return_fields") List<String> returnFields) {

    List<Map<String, Object>> result = dataService.getEntities(client, entity, query, returnFields, true);

    if (!StringUtils.isEmpty(returnFields)) {
      for (Map<String, Object> partner : result) {
        List<String> removedFields =
            partner.keySet().stream().filter(key -> !returnFields.contains(key)).collect(Collectors.toList());
        removedFields.forEach(key -> partner.remove(key));
      }
    }
    return result;
  }

}
