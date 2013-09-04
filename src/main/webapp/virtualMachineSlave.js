/**
 * Handles dynamic loading of the list boxes in a dependant chain.
 *
 * This is placed on a list box's `onchange` event. When called it
 * updates the list box (by name attribute) given by the `nextName`
 * argument. As the `updateListBox` function does not trigger an `onChange`
 * event in the element being updated this is called manually via the
 * `onSuccess` callback.
 *
 * @param {String} nextName Name attribute of the next list box in the chain.
 * @param params Array of GET parameters for the list box populate function.
 */
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
    //Update the listbox `nextName` - this function is defined by Jenkins in
    //the `select.js` file.
    updateListBox(document.getElementsByName(nextName)[0], url,
        {'onSuccess': function() {
                //On success call the next list box's `onChange` event
                document.getElementsByName(nextName)[0].onchange();
            }
        }
    );
}