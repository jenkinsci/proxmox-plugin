function onChangeListBox(nextName, params) {
    //Build the url
    var url = rootURL + '/plugin/proxmox/' + nextName + 'Values';
    if (params.length>0) url += '?';
    for (var i=0;i<params.length;++i) {
        var queryName = params[i][0];
        var queryValueNode = params[i][1];
        var queryValue = '';
        //Fetch the value of the parameter from the named node
        if (typeof queryValueNode === 'object') {
            //This is used if `this` is passed in
            queryValue = queryValueNode.value;
        } else {
            //The name of the select node is passed in, so fetch the selected value
            var el = document.getElementsByName(queryValueNode)[0];
            queryValue = el.options[el.selectedIndex].value;
        }
        //Encode the parameters onto the url
        url += queryName + '=' + encode(queryValue);
        if (i != params.length-1) url += '&';
    }
    //Update the listbox `nextName`
    updateListBox(document.getElementsByName(nextName)[0], url,
        {'onSuccess': function() {
                //On success call the next list box's `onChange` event
                document.getElementsByName(nextName)[0].onchange();
            }
        }
    );
}