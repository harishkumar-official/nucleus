function createMetadata(doc) {
	var success = false;
	var localization = $(".toolbar .localization select").val();
	var metaString = JSON.stringify(metadata[localization]);
	var metaJson = JSON.parse(metaString);
	delete metaJson.id;
	delete metaJson.localization;
	doc["metadata"] = metaJson;
	$.ajax({
		method: "POST",
		url: metaBaseURL + "/create",
		async: false,
		data: JSON.stringify(doc),
		contentType: 'application/json; charset=UTF-8'
	}).success(function (data) {
		if (data) {
			var localization = doc["localization"];
			select = $(".toolbar .localization select")
			var optionRef = select.children("option").first().clone();
			optionRef.text(localization).val(localization)
			// select.selectmenu("destroy");
			select.append(optionRef);
			select.selectmenu();
			success = true;
			notifySuccess("Created, Woo hoo!");
		}
	}).error(function (e) {
		notifyResponseError(e);
	});
	return success;
}

function addInMetadata(fieldname, fieldvalue) {
	var updatesMap = new Object();
	updatesMap[fieldname.replace(/-/g, ".")] = [fieldvalue];
	return updateMetadata("add", updatesMap);
}

function deleteInMetadata(fieldname, fieldvalue, maxSerial) {
	var updatesMap = new Object();
	updatesMap[fieldname.replace(/-/g, ".")] = [fieldvalue];
	return updateMetadata("delete", updatesMap, maxSerial);
}

function updateInMetadata(fieldname, fieldvalue) {
	var updatesMap = new Object();
	updatesMap[fieldname.replace(/-/g, ".")] = fieldvalue;
	return updateMetadata("update", updatesMap);
}

function multipleUpdateInMetadata(parentFieldId, serial, updatesMap) {
	var newUpdatesMap = new Object();
	$.each(updatesMap, function (key, value) {
		newUpdatesMap[parentFieldId.replace(/-/g, ".") + "." + serial + "." + key] = value;
	});
	return updateMetadata("update", newUpdatesMap);
}

function updateMetadata(type, updatesMap, maxSerial) {
	var dataObject = new Object();
	if (type == "add") {
		dataObject["ids"] = ids;
	} else if (type == "update") {
		dataObject["ids"] = [currentMetadata.id];
	}
	dataObject["updates"] = updatesMap;
	if (maxSerial) {
		dataObject["array_size"] = maxSerial;
	}
	return ajaxCall(metaBaseURL, type, dataObject);
}

function getMetadata() {
	var success = false;
	$.ajax({
		method: "GET",
		url: "/ui/metadata/get?client=" + client,
		async: false,
		contentType: 'application/json; charset=UTF-8'
	}).success(function (data) {
		if (data) {
			success = true;
			metadata = data;
		}
	}).error(function (e) {
		notifyResponseError(e);
	});
	return success;
}

function hideAll() {
	$('.FieldTable').hide();
	$('.EntityTable').hide();
	$('.entityRow').hide();
	$('.TypeTable').hide();
	$('.typeRow').hide();

	$('.AddEntityButtons').hide().children().hide();
	$('.AddTypeButtons').hide().children().hide();
	$('.AddFieldButtons').hide().children().hide();

	$('.NewEntity').remove();
	$('.NewType').remove();
	$('.NewField').remove();
}

function assignEdit(fieldRow) {
	var infoButton = $(".edit .info").clone().show();
	var editButton = $(".edit .edit").clone();
	var saveButton = $(".edit .save").clone();
	var fieldChild = fieldRow.children("td.serial");
	fieldChild.append(infoButton).append(editButton).append(saveButton);

	infoButton.click(function () {
		// TODO: creater and modifier details + create/update date
		notifyIncomplete($(this));
	});

	fieldRow.hover(function () {
		if (saveButton.is(":hidden")) {
			editButton.show();
		}
	}, function () {
		editButton.hide();
	});
	editButton.click(function () {
		saveButton.show();
		editButton.hide();

		var subFields = $(this).parent().siblings(":visible");
		var type = $(this).parent().siblings(".type").text().trim();
		$.each(subFields, function (index, elem) {
			elem = $(elem);
			var val = elem.text();
			var fieldname = elem.attr("class");
			if (fieldname != "displayname" && fieldname != "description" && !(fieldname == "values" && type == "enum")) {
				return true;
			}
			elem.text("");
			setElementInput(elem, fieldname);
			elem.children("input, select").val(val);
		});
	});
	saveButton.click(function () {
		saveButton.hide();

		var tableTbody = $(this).parents("tbody");
		var parentDiv = tableTbody.parents("table").parent();
		var tableName = parentDiv.attr("class");
		var id = parentDiv.attr("id");
		if (tableName == "EntityTable") {
			id = "entities";
		} else if (tableName == "TypeTable") {
			id = "type_definitions";
		} else if (id == "global_fields") {
			id = "global_fields";
		} else {
			id = id + "-fields";
		}

		var serial = parseInt($(this).siblings(".serialspan").text().trim());
		var subFields = $(this).parent().siblings();
		var errorFlag = false;
		var updateObject = new Object();
		$.each(subFields, function (index, elem) {
			elem = $(elem);
			if (elem.children('input, select').size() == 0) {
				return true;
			}
			var response = updateField(elem, elem, updateObject, tableTbody);
			if (!response) {
				errorFlag = true;
				return false;
			}
		});
		if (errorFlag) {
			return false;
		}

		// call API with updates
		multipleUpdateInMetadata(id, serial, updateObject)

		// update left explorer panel
		var nameAttr = id + "-" + serial;
		var explorerElem = $('.explorer li[name=' + nameAttr + ']');
		var displayName = updateObject["displayname"];
		if (explorerElem.children().size() == 0) {
			explorerElem.text(displayName);
		} else {
			explorerElem.children("a").text(displayName);
		}
	});
}

function assignBackground(elem, index) {
	if (index % 2 == 0) {
		elem.css("background-color", "rgb(246, 246, 246)");
	} else {
		elem.css("background-color", "rgb(236, 236, 236)");
	}
}

function assignExplorerLinkClick(linkElem, innerFieldstableDiv) {
	linkElem.click(function () {
		highlightElem(this);
		hideAll();
		innerFieldstableDiv.show();
		$('.AddFieldButtons, #AddFieldButton').show();
	});
}

function applyMultipleEntityRules(elem) {
	if (!allowMultipleEntity) {
		elem.find("th.association_type").hide();
		elem.children("td.association_type").hide();
	}
}

function hideDates(elem) {
	elem.find("th.createdate").hide();
	elem.find("th.updatedate").hide();
	elem.children("td.createdate").hide();
	elem.children("td.updatedate").hide();
}

function populateFields(fields, explorerDiv, parentId) {
	var fieldsTableDiv;
	if (parentId == "global_fields") {
		fieldsTableDiv = $('.GlobalFieldTableRef').clone();
		fieldsTableDiv.removeClass("GlobalFieldTableRef").addClass("FieldTable");
	} else {
		fieldsTableDiv = $('.FieldTableRef').clone();
		fieldsTableDiv.removeClass("FieldTableRef").addClass("FieldTable");
	}
	fieldsTableDiv.attr("id", parentId)
	contentDiv.append(fieldsTableDiv);
	var fieldTable = $(".FieldTable");
	fieldTable.find("th").show();
	applyMultipleEntityRules(fieldTable);
	hideDates(fieldTable);

	var fieldTbody = fieldsTableDiv.find('table tbody');
	var rowRef = fieldTbody.children('tr');

	var ulElem = $('.UlRef').clone();
	ulElem.removeClass("UlRef");
	var liRef = ulElem.children('li');
	explorerDiv.append(ulElem);

	if (fields) {
		var index = 1;
		fields.forEach(field => {
			var fieldRow = rowRef.clone();
			fieldRow.children("td").show();
			applyMultipleEntityRules(fieldRow);
			hideDates(fieldRow);
			$.each(field, function (key, value) {
				var fieldChild = fieldRow.children("td." + key);
				if (key == "fields") {
					// skip
				} else if (key == "serial") {
					var serialSpan = $(".input span").clone().append(value).addClass("serialspan");
					fieldChild.append(serialSpan);
				} else if (key == "updatedate" || key == "createdate") {
					var date = new Date(value).toString();
					date.substr(0, 15)
					fieldChild.append(date.substr(0, 15)).attr("title", date);
				} else if (value.constructor === Array) {
					fieldChild.append(enumString(value));
				} else {
					fieldChild.append(value);
				}
			});

			// Add edit buttons
			assignEdit(fieldRow);

			assignBackground(fieldRow, index);
			index = index + 1;
			fieldTbody.append(fieldRow);

			var fieldLi = liRef.clone();
			ulElem.append(fieldLi);
			var fieldId;
			if (parentId == "global_fields") {
				fieldId = parentId + "-" + field.serial;
			} else {
				fieldId = parentId + "-fields-" + field.serial;
			}
			fieldLi.attr("name", fieldId);
			if (field.type == "object" || field.subtype == "object") {
				fieldLi.append("<a href='#'>" + field.displayname + "</a>");
				var innerFieldstableDiv = populateFields(field.fields, fieldLi, fieldId);
				assignExplorerLinkClick(fieldLi.children('a'), innerFieldstableDiv);
			} else {
				fieldLi.append(field.displayname);
			}
		});
	}
	liRef.remove();
	rowRef.remove();
	return fieldsTableDiv;
}

function populateElements(elements, elemRowRef, elementsTbody, elementsUlElem) {
	var index = 1;
	elements.forEach(elem => {
		var isEntity = false;
		var elementId;
		if (elem.entityname) {
			isEntity = true;
			entitiesName[elem.entityname] = elem.displayname;
			elementId = "entities-" + elem.serial;
		} else {
			typesName[elem.typename] = elem.displayname;
			elementId = "type_definitions-" + elem.serial;
		}
		var elemRow = elemRowRef.clone();
		elemRow.children("td").show();
		hideDates(elemRow);
		$.each(elem, function (key, value) {
			if (key == "fields") {
				// skip
			} else if (key == "serial") {
				var serialSpan = $(".input span").clone().append(value).addClass("serialspan");
				elemRow.children("td." + key).append(serialSpan);
			} else if (key == "updatedate" || key == "createdate") {
				var date = new Date(value).toString();
				date.substr(0, 15)
				elemRow.children("td." + key).append(date.substr(0, 15)).attr("title", date);
			} else {
				elemRow.children("td." + key).append(value);
			}
		});
		assignBackground(elemRow, index);
		index = index + 1;
		elementsTbody.append(elemRow);

		// Add edit buttons
		if (!isEntity || allowMultipleEntity == true) {
			assignEdit(elemRow);
		}

		var elemLi = liRef.clone();
		elemLi.attr("name", elementId)
		elemLi.append("<a href='#'>" + elem.displayname + "</a>");
		elementsUlElem.append(elemLi);

		var fieldsTableDiv = populateFields(elem.fields, elemLi, elementId);

		assignClick(elemLi, elemRow, isEntity, fieldsTableDiv);
	});
}

function assignClick(elemLi, elemRow, isEntity, fieldsTableDiv) {
	elemLi.children('a').click(function () {
		highlightElem(this);
		hideAll();
		/* if (isEntity) {
			$('.EntityTable').show();
		} else {
			$('.TypeTable').show();
		}
		elemRow.show(); */
		fieldsTableDiv.attr("style", "margin-top:-10px");
		fieldsTableDiv.show();
		$('.AddFieldButtons, #AddFieldButton').show();
		$('#SaveFieldButton').hide();
	});
}

function hideOthers(table) {
	table.find('.subtype').hide().val("");
	table.find('.association_type').hide().val("");
	table.find('.values').hide().find('input').val("");
}

function setTypeClick(typeElem) {
	$(typeElem).change(function () {
		var selectedValue = $(this).children('select').val();
		var table = $(this).parents('table');
		hideOthers(table);
		if (selectedValue == "enum") {
			table.find('.values').show();
		} else if (selectedValue == "array") {
			table.find('.subtype').show();
		} else if (entitiesName.hasOwnProperty(selectedValue)) {
			table.find('.association_type').show();
		}
	});
}

function setElementInput(elem, fieldname) {
	if (fieldname == "fieldname" || fieldname == "entityname" || fieldname == "typename") {
		var fieldInput = $('.input input').clone();
		fieldInput.css("text-transform", "lowercase");
		elem.append(fieldInput[0]);
	} else if (fieldname == "type") {
		elem.append($('.input .type').clone()[0]);
		setTypeClick(elem);
	} else if (fieldname == "subtype") {
		elem.append($('.input .subtype').clone()[0]);
	} else if (fieldname == "association_type") {
		elem.append($('.input .association_type').clone()[0]);
	} else if (fieldname == "required") {
		elem.append($('.input .boolean').clone()[0]);
	} else if (fieldname == "fieldlevel") {
		elem.append($('.input .fieldlevel').clone()[0]);
		$(elem).children(".fieldlevel").change(function () {
			var value = $(this).val();
			if (value == "primary") {
				$(elem).siblings(".required").children("select").val("true")
			}
		});
	} else {
		elem.append($('.input input').clone()[0]);
	}
}

var charList = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '0'];
function checkFieldExists(elem, tableTbody) {
	var response = true;
	var fieldName = elem.attr("class");
	if (fieldName == "fieldname" || fieldName == "entityname" || fieldName == "typename") {
		var fieldnamevalue = $(elem).children('input').val();
		fieldnamevalue = fieldnamevalue.toLowerCase();
		$(elem).children('input').val(fieldnamevalue);

		// checking allowed characters 
		if (charList.includes(fieldnamevalue.charAt(0))) {
			$(elem).append("<span style='color:red'><br>Can't start with number</span>");
			$(elem).children().focus();
			return false;
		}
		if (fieldnamevalue.includes(" ")) {
			$(elem).append("<span style='color:red'><br>Spaces ain't allowed</span>");
			$(elem).children().focus();
			return false;
		}

		// checking uniqueness
		var elems = tableTbody.find("td." + fieldName);
		$.each(elems, function (index, elem) {
			var currentname = $(elem).text();
			if (fieldnamevalue == currentname) {
				$(elem).append("<span style='color:red'><br>Duplicate Field Name</span>");
				$(elem).children().focus();
				response = false;
				return false;
			}
		});
	}
	return response;
}

var mandatoryFields = ["entityname", "typename", "fieldname", "displayname", "values", "subtype", "association_type"];
function checkMandatoryFields(elem) {
	var fieldName = elem.attr("class");
	var val = elem.children('input, select').val();
	val = !val ? val : val.trim();
	if (mandatoryFields.indexOf(fieldName) >= 0 && !val) {
		$(elem).append("<span style='color:red'><br>Mandatory Field</span>");
		$(elem).children().focus();
		return false;
	}
	return true;
}

function checkField(elem, tableTbody) {
	if (!checkFieldExists(elem, tableTbody)) {
		return false;
	}
	if (!checkMandatoryFields(elem)) {
		return false;
	}
	return true;
}

function updateField(elem, newElem, updateObject, tableTbody) {
	if (!checkField(elem, tableTbody)) {
		return false;
	}

	var fieldName = elem.attr("class");
	var val = elem.children('input, select').val();
	val = !val ? val : val.trim();
	if (val) {
		if (fieldName == "values") {
			val = convertToEnum(val);
			newElem.text(enumString(val));
			updateObject[fieldName] = val;
		} else {
			newElem.text(val);
			updateObject[fieldName] = val;
		}
	} else {
		newElem.text(val);
	}
	return true;
}

function checkPrimaryKey(row, tbody) {
	var elem = row.children(".fieldlevel");
	var fieldlevel = elem.children("select").val();
	if (fieldlevel == "primary") {
		var id = $('.FieldTable:visible').attr("id");
		if (id.includes("-fields-")) {
			$(elem).append("<span style='color:red'><br>Invalid Inside Object</span>");
			$(elem).children().focus();
			return false;
		}
		var fieldtype = row.children(".type").children("select").val();
		if (fieldtype == "object" || typesName.hasOwnProperty(fieldtype) || entitiesName.hasOwnProperty(fieldtype)) {
			$(elem).append("<span style='color:red'><br>Invalid for Object types</span>");
			$(elem).children().focus();
			return false;
		}
	}
	return true;
}

function populateAddButton(name, tableRef) {
	$('#Add' + name + 'Button').unbind("click");
	$('#Add' + name + 'Button').click(function () {
		$(this).hide();
		$('.Add' + name + 'Buttons, #Save' + name + 'Button').show();
		var addTable;
		var id = $('.FieldTable:visible').attr("id");
		if (id == "global_fields") {
			addTable = $('.GlobalFieldTableRef table').clone();
		} else {
			addTable = tableRef.clone();
		}
		addTable.addClass('New' + name);
		$(this).after(addTable);
		var addRow = addTable.find("tbody tr");
		$.each(addRow.children(), function (index, elem) {
			var fieldname = $(elem).attr("class");
			setElementInput(elem, fieldname);
		});
		if (id == "global_fields") {
			addTable.find("td.type").children("select").attr("disabled", "disabled").val("enum");
		}
		$(".reference").animate({ scrollTop: $(document).height() }, "slow");
	});

	$('#Save' + name + 'Button').unbind("click");
	$('#Save' + name + 'Button').click(function () {
		var table;
		var id;
		if (name == "Field") {
			table = $('.FieldTable:visible table tbody');
			id = $('.FieldTable:visible').attr("id");
			if (id != "global_fields") {
				id = id + "-fields";
			}
		} else if (name == "Entity") {
			table = $('.EntityTable table tbody');
			id = "entities";
		} else if (name == "Type") {
			table = $('.TypeTable table tbody');
			id = "type_definitions";
		}

		if (id == "global_fields") {
			tableRef = $('.GlobalFieldTableRef table');
		}

		var newRow = tableRef.find('tbody tr').clone();
		var errorFlag = false;

		var updateObject = new Object();
		var dataRow = $('.New' + name + ' tbody tr');
		var columns = dataRow.children(':visible');
		dataRow.find('span').remove();
		$.each(columns, function (index, elem) {
			var fieldName = $(elem).attr("class");
			var newElem = newRow.children("." + fieldName);
			var response = updateField($(elem), newElem, updateObject, table);
			if (!response) {
				errorFlag = true;
				return false;
			}
		});
		if (!checkPrimaryKey(dataRow, table)) {
			errorFlag = true;
		}

		if (errorFlag) {
			return false;
		}
		// add serial
		var serial = table.children(":last-child").find('td.serial .serialspan').text();
		var newSerial = parseInt(serial) + 1;
		if (!newSerial) {
			newSerial = 1;
		}
		var serialSpan = $(".input span").clone().append(newSerial).addClass("serialspan");
		newRow.children('.serial').append(serialSpan);
		updateObject["serial"] = newSerial;

		// Add edit buttons
		assignEdit(newRow);

		// add background color
		assignBackground(newRow, newSerial);

		// add dates
		var date = new Date();
		val = date.toString();
		newRow.children('.createdate').append(val.substr(0, 15)).attr("title", val);
		newRow.children('.updatedate').append(val.substr(0, 15)).attr("title", val);
		updateObject["createdate"] = date.getTime();
		updateObject["updatedate"] = date.getTime();

		// call API with updates
		var success = addInMetadata(id, updateObject);
		if (!success) {
			return false;
		}

		// update UI
		updateUI(newRow, name);

		// update table
		newRow.children().show();
		applyMultipleEntityRules(newRow);
		hideDates(newRow);
		table.append(newRow);
		$('.New' + name).remove();
		$(this).hide();
		$('.Add' + name + 'Buttons, #Add' + name + 'Button').show();
	});
}

function enumString(array) {
	val = "";
	for (var i = 0; i < array.length; i++) {
		if (i == array.length - 1) {
			val = val + array[i];
		} else {
			val = val + array[i] + ", ";
		}
	}
	return val;
}

function convertToEnum(value) {
	var array = value.split(",");
	val = [];
	array.forEach(function (elem) {
		elem = elem.trim();
		if (!val.includes(elem) && elem != "") {
			val.push(elem);
		}
	});
	return val;
}

function updateUI(newRow, name) {
	var displayName = newRow.children('.displayname').text();
	var ul = $('.UlRef').clone();
	ul.removeClass("UlRef");
	var li = ul.children('li').remove();
	if (name == "Field") {
		var type = newRow.children('.type').text();
		var subtype = newRow.children('.subtype').text();
		var tableDiv = $('.FieldTable:visible');
		var idAttr = tableDiv.attr('id');
		var fieldSerial = newRow.find('.serial .serialspan').text();
		if(idAttr == "global_fields"){
			$('.explorer .global_fields ul:first').append(li);
			li.attr("name", "global_fields-" + fieldSerial);
			li.append(displayName);
		} else {
			$('.explorer li[name=' + idAttr + ']').children('ul').append(li);
			var newId = idAttr + "-fields-" + fieldSerial;
			if (type == "object" || subtype == "object") {
				li.attr("name", newId);
				li.append("<a href='#'>" + displayName + "</a>");
				var innerFieldstableDiv = populateFields([], li, newId);
				assignExplorerLinkClick(li.children('a'), innerFieldstableDiv);
			} else {
				li.attr("name", newId);
				li.append(displayName);
			}
		}
	} else if (name == "Entity") {
		var entityname = newRow.children('.entityname').text();
		var serial = newRow.find('.serial .serialspan').text();
		var id = "entities-" + serial;
		li.attr("name", id);
		li.append("<a href='#'>" + displayName + "</a>");
		$('.explorer .entities ul:first').append(li);
		var fieldsTableDiv = populateFields([], li, id);
		assignClick(li, newRow, true, fieldsTableDiv)
		updateTypeSelect(entityname, displayName);
		entitiesName[entityname] = displayName;
	} else if (name == "Type") {
		var typename = newRow.children('.typename').text();
		var serial = newRow.find('.serial .serialspan').text();
		var id = "type_definitions-" + serial;
		li.attr("name", id);
		li.append("<a href='#'>" + displayName + "</a>");
		$('.explorer .type_definitions ul:first').append(li);
		var fieldsTableDiv = populateFields([], li, id);
		assignClick(li, newRow, false, fieldsTableDiv)
		updateTypeSelect(typename, displayName);
		typesName[typename] = displayName;
	}
}

function updateTypeSelect(name, displayName) {
	var selectOption = $(".input .type option").clone()[0];
	selectOption = $(selectOption).text(displayName).val(name);
	$('.input .type').append(selectOption);
	$('.input .subtype').append(selectOption.clone());
}

function populateButtons() {
	// Add Global Field Button
	populateAddButton("Field", $('.GlobalFieldTableRef table'));

	// Add Entity Button
	populateAddButton("Entity", $('.AddEntity table'));

	// Add Type Button
	populateAddButton("Type", $('.AddType table'));

	// Add Field Button
	populateAddButton("Field", $('.FieldTableRef table'));
}

function highlightElem(elem){
	explorerDivRoot.find("a, .global_fields_label, .entities_label, .type_definitions_label").css("font-weight", "");
	$(elem).css("font-weight", "bold");
}

var ulRef = $('.UlRef').clone();
ulRef.removeClass("UlRef");
var liRef = ulRef.children('li').remove();
var explorerDivRoot;
var contentDiv;
var entitiesName;
var typesName;
var jsonObj;

function loadMetadata(json) {
	hideAll();
	jsonObj = json;
	var globalFields = jsonObj.global_fields;
	var entities = jsonObj.entities;
	var typeDefinitions = jsonObj.type_definitions;

	entitiesName = new Object();
	typesName = new Object();

	var mainDiv = $('.main');
	explorerDivRoot = mainDiv.children('.explorer');
	referenceDiv = mainDiv.children(".reference");
	contentDiv = referenceDiv.children('.content');
	contentDiv.children(".FieldTable").remove();
	referenceDiv.children(".AddFieldButtons").children(".NewField").remove();

	// Populate Global Fields
	var globalFieldsUlElem = explorerDivRoot.children('.global_fields').text("");
	var gloablFieldsTableDiv = populateFields(globalFields, globalFieldsUlElem, "global_fields");
	var globalFieldsLabel = explorerDivRoot.children('.global_fields_label');
	globalFieldsLabel.click(function () {
		highlightElem(this);
		hideAll();
		gloablFieldsTableDiv.show();
		$('.AddFieldButtons, #AddFieldButton').show();
	});

	// Populate Entities
	var addEntityTableRef = $('.AddEntity').children('table');
	var entitiesUlElem = ulRef.clone();
	explorerDivRoot.children('.entities').text("").append(entitiesUlElem);
	var entityTableDiv = $('.EntityTable');
	var entitiesTbody = entityTableDiv.text("").append(addEntityTableRef.clone()).find('table tbody');
	$('.EntityTable table thead tr th').show();
	hideDates(entityTableDiv);
	var entityRowRef = entitiesTbody.children('.entityRow').remove();
	populateElements(entities, entityRowRef, entitiesTbody, entitiesUlElem);
	var entitiesLabel = explorerDivRoot.children('.entities_label');
	if (allowMultipleEntity) {
		entitiesLabel.click(function () {
			highlightElem(this);
			hideAll();
			$('.EntityTable').show();
			$('.entityRow').show();
			$('.AddEntityButtons, #AddEntityButton').show();
		});
	} else {
		entitiesLabel.css("cursor", "");
	}

	// Populate Type Definitions
	var addTypeTableRef = $('.AddType').children('table');
	var typesUlElem = ulRef.clone();
	explorerDivRoot.children('.type_definitions').text("").append(typesUlElem);
	var typeTableDiv = $('.TypeTable');
	var typesTbody = typeTableDiv.text("").append(addTypeTableRef.clone()).find('table tbody');
	$('.TypeTable table thead tr th').show();
	hideDates(typeTableDiv);
	var typeRowRef = typesTbody.children('.typeRow').remove();
	populateElements(typeDefinitions, typeRowRef, typesTbody, typesUlElem);
	explorerDivRoot.children('.type_definitions_label').click(function () {
		highlightElem(this);
		hideAll();
		$('.TypeTable').show();
		$('.typeRow').show();
		$('.AddTypeButtons, #AddTypeButton').show();
	});

	// Update types
	var typeSelect = $('.input .type');
	var subTypeSelect = $('.input .subtype');
	var optionRef = typeSelect.children('option')[0];

	// Update types with custom types name
	$.each(typesName, function (key, value) {
		var option = $(optionRef).clone();
		option.text(value).val(key);
		typeSelect.append(option);
		option = $(optionRef).clone();
		option.text(value).val(key);
		subTypeSelect.append(option);
	});

	// Update types with entites name
	if (allowMultipleEntity) {
		$.each(entitiesName, function (key, value) {
			var option = $(optionRef).clone();
			option.text(value).val(key);
			typeSelect.append(option);
		});
	}

	// Populate Add Buttons
	populateButtons();

	if (!allowMultipleEntity) {
		$(".explorer li[name=entities-1]").children("a").click(); // showing first entity data on page load
	}
}

function addDocument() {
	var valid = true;
	dialogFields.removeClass("ui-state-error");

	var errorFlag = false;
	var doc = new Object();
	dialogFields = dialog.find("fieldset input[type=text], select");
	$.each(dialogFields, function (index, input) {
		input = $(input);
		var fieldname = input.attr("name");
		var value = input.val().trim();
		if (!value || value == NaN) {
			errorFlag = true;
			input.addClass("ui-state-error");
			return false;
		}
		doc[fieldname] = value;
	});
	if (errorFlag == true) {
		notifyError("Wrong data");
		valid = false;
	}
	if (valid) {
		var response = createMetadata(doc);
		if (response == true) {
			dialog.dialog("close");
		}
	}
	return valid;
}

function populateCreateButton() {
	// populate create dialog fields
	var fieldset = dialog.find("fieldset");
	fieldset.children("input[type=text], select").remove();
	var label = fieldset.children("label").remove()[0];
	label = $(label).text("Localization");
	var input = $(".input input").clone().removeAttr("style");
	input.attr("name", "localization");
	fieldset.append(label).append(input);

	var button = $(".menubar .create");
	button.click(function () {
		dialog.dialog("open");
	});
}

function setupDialogBox(dialogFields) {
	var dialog = $("#dialog-form").dialog({
		autoOpen: false,
		height: 400,
		width: 350,
		modal: true,
		buttons: {
			"Create": addDocument,
			Cancel: function () {
				dialog.dialog("close");
			}
		},
		close: function () {
			form[0].reset();
			dialogFields.removeClass("ui-state-error");
		}
	});
	var form = dialog.find("form").on("submit", function (event) {
		// for, if somebody presses key enter
		event.preventDefault();
		addDocument();
	});
	return dialog;
}