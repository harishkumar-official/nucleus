package com.nucleus.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nucleus.service.DataService;
import com.nucleus.transientmodel.MetadataCreateRequest;
import com.nucleus.transientmodel.MetadataDeleteRequest;
import com.nucleus.transientmodel.MetadataUpdateRequest;

import io.swagger.annotations.Api;

@RestController
@CrossOrigin("*")
@RequestMapping("v1/metadata")
@Api(tags = "Metadata", description = "meta-data operations")
public class MetadataController {

  private DataService dataService;

  @Autowired
  public MetadataController(DataService partnerService) {
    this.dataService = partnerService;
  }

  @RequestMapping(value = "/{client}/create", method = RequestMethod.POST)
  public String create(@RequestBody MetadataCreateRequest request, @PathVariable("client") String client) {
    return dataService.createMetaData(client, request.getLocalization(), request.getMetadata());
  }

  @RequestMapping(value = "/{client}/update", method = RequestMethod.PUT)
  public Boolean update(@RequestBody MetadataUpdateRequest request, @PathVariable("client") String client) {
    return dataService.updateMetadata(request.getIds(), client, request.getUpdates());
  }

  @RequestMapping(value = "/{client}/element/add", method = RequestMethod.PUT)
  public Boolean add(@RequestBody MetadataUpdateRequest request, @PathVariable("client") String client) {
    return dataService.addInMetaArray(request.getIds(), client, request.getUpdates());
  }

  @RequestMapping(value = "/{client}/element/delete", method = RequestMethod.PUT)
  public Boolean delete(@RequestBody MetadataDeleteRequest request, @PathVariable("client") String client) {
    return dataService.deleteInMetaArray(request.getIds(), client, request.getArraySize(), request.getUpdates());
  }

  @RequestMapping(value = "/{client}/get", method = RequestMethod.GET, produces = "application/json")
  public List<Map<String, Object>> get(@PathVariable("client") String client,
      @RequestParam(required = false) String localization) {
    return dataService.getMetaData(client, localization);
  }

}
