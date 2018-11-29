function loadToolbar(client, isMetaClient, isMetadata, localizations, environments) {
    var clientSpan = $(".toolbar .client");
    var metadataSpan = $(".toolbar .metadata");
    var typeSelect = metadataSpan.children("select");
    var environmentSpan = $(".toolbar .environment");
    var localizationSpan = $(".toolbar .localization");
    var selectRef = $(".toolbar .select");

    // client
    clientSpan.text(client);

    // metadata
    if (isMetaClient == false) {
        metadataSpan.hide();
    } else {
        metadataSpan.show();
        if (isMetadata) {
            typeSelect.val("metadata");
            environmentSpan.hide();
        } else {
            typeSelect.val("appdata");
        }
        typeSelect.selectmenu();
    }
    typeSelect.selectmenu({
        change: function () {
            var value = $(this).val();
            var form = $(".toolbar .appform");
            if (value == "metadata") {
                form = $(".toolbar .metaform");
            }
            form.children("input").val(client);
            form.submit();
        }
    });

    // environment
    if (environments) {
        environmentSpan.show();
        var select = selectRef.clone().removeClass("select").show();
        var optionRef = select.children("option").remove();
        environments.forEach(function (elem) {
            select.append(optionRef.clone().text(elem).val(elem));
        });
        environmentSpan.append(select);
        select.selectmenu();
    }

    // localization
    if (localizations) {
        localizationSpan.show();
        var select = selectRef.clone().removeClass("select").show();
        var optionRef = select.children("option").remove();
        localizations.forEach(function (elem) {
            select.append(optionRef.clone().text(elem).val(elem));
        });
        localizationSpan.append(select);
        select.selectmenu();
    }

    // show toolbar
    $(".toolbar").show();
}

var successMsg = "Successful, Woo hoo!";
var errorMsg = "Error! Please try in a bit.";
function notifySuccess(msg) {
    if (!msg || msg == "") {
        msg = successMsg;
    }
    $(".notify_success").text(msg).show("slow").delay(1000).fadeOut();
}
function notifyError(error) {
    if (!error || error == "") {
        error = errorMsg;
    }
    $(".notify_error").text(error).show("slow").delay(1000).fadeOut();
}

function notifyResponseError(e) {
    if (e.responseJSON) {
        notifyError(e.responseJSON.message);
    } else {
        notifyError(e.responseText);
    }
}

function notifyIncomplete(elem) {
    notifyError("UI for this feature ain't ready yet. Please bear with us for few days.");
}

function ajaxCall(baseurl, type, dataObject) {
    var url;
    if (type == "add" || type == "add_array") {
        url = baseurl + "/element/add";
    } else if (type == "delete") {
        url = baseurl + "/element/delete";
    } else if (type == "update") {
        url = baseurl + "/update";
    } else if (type == "replace") {
        url = baseurl + "/replace";
    } else if (type == "primary") {
        url = baseurl + "/primary/update";
    }
    var success = false;
    $.ajax({
        method: "PUT",
        url: url,
        async: false,
        data: JSON.stringify(dataObject),
        contentType: 'application/json; charset=UTF-8'
    }).success(function (data) {
        if (data == 0) {
            success = false;
        }
        if (data && data > 0) {
            success = true;
            if (type == "add") {
                notifySuccess("Added, Woo hoo!");
            } else if (type == "delete") {
                notifySuccess("Deleted, Woo hoo!");
            } else if (type == "update" || type == "replace" || type == "primary") {
                notifySuccess("Updated, Woo hoo!");
            }
        }
    }).error(function (e) {
        notifyResponseError(e);
    });
    return success;
}