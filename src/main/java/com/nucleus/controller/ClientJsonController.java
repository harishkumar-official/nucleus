package com.nucleus.controller;

import com.nucleus.service.DataService;
import com.nucleus.transientmodel.JsonRequest;
import com.nucleus.transientmodel.JsonUpdateRequest;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("v1/client")
@Api(tags = "Client", description = "client operations with free-form json capability")
public class ClientJsonController {

  private DataService dataService;

  @Autowired
  public ClientJsonController(DataService dataService) {
    this.dataService = dataService;
  }

  /* Comma separated ids */
  @RequestMapping(value = "/{client}/json/upload", method = RequestMethod.POST)
  public String upload(
      @PathVariable String client,
      @RequestPart String ids,
      @RequestPart String fieldname,
      @RequestPart MultipartFile file)
      throws IOException {
    return dataService.uploadFileForNonMetaClient(client, ids, fieldname, file);
  }

  @RequestMapping(value = "/{client}/json/create", method = RequestMethod.POST)
  public String createJson(@PathVariable String client, @RequestBody JsonRequest request) {
    return dataService.createJson(
        client, request.getDoc(), request.getEnvironment(), request.getLocalization());
  }

  @RequestMapping(value = "/{client}/json/replace", method = RequestMethod.PUT)
  public Long replaceJson(@PathVariable String client, @RequestBody JsonUpdateRequest request) {
    return dataService.replaceJson(
        client,
        request.getId(),
        request.getDoc(),
        request.getEnvironment(),
        request.getLocalization());
  }

  /** Returns list of client data documents. */
  @RequestMapping(
      value = "/{client}/json/get",
      method = RequestMethod.GET,
      produces = "application/json")
  public List<Map<String, Object>> getJson(
      @PathVariable String client,
      @RequestParam String environment,
      @RequestParam(required = false) String localization,
      @RequestParam(required = false, value = "return_fields") List<String> returnFields) {

    return dataService.getJson(client, environment, localization, returnFields);
  }
}
