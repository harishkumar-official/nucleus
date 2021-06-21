function loadToolbar(client, isMetaClient, localizations, environments) {
    var clientSpan = $(".toolbar .client");
    var metabuttonsSpan = $(".toolbar .metabuttons");

    // client
    clientSpan.text(client);

    // metadata
    if (isMetaClient == false) {
        metabuttonsSpan.hide();
    } else {
        metabuttonsSpan.show();
        metabuttonsSpan.children(".metadata").click(function () {
            window.location.href = "/ui/metadata?client=" + client;
        });
        metabuttonsSpan.children(".appdata").click(function () {
            window.location.href = "/ui/appdata?client=" + client;
        });
    }

    var selectRef = $(".toolbar .select");
    // environment
    if (environments) {
        var environmentSpan = $(".toolbar .environment");
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
        var localizationSpan = $(".localization");
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
        success = true;
        if (data && data > 0) {
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