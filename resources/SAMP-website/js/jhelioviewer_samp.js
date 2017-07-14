// Set up hub registration/unregistration.
var meta = {
    "samp.name": "FlareList",
    "samp.description": "Simple Flare Display Tool"
};

var intiSendToJhv = function() {
	var cc = new samp.ClientTracker();
    var callHandler = cc.callHandler;
	var subs = cc.calculateSubscriptions();
	
	connector = new samp.Connector("FlareList", meta, cc, subs);
	
	
	$('table button.jhv-button').click(function() {
			var $tr = $(this).parent().parent();
			var $row = $tr.find('td');
			
			// we need to convert the date in the correct format
			var date = moment($row[1].innerText, "DD-MMM-YYYY");
			
			requestLayer(
				date.format("YYYY-MM-DD"),
				$row[2].innerText,
				$row[3].innerText,
				$row[4].innerText,
				$row[9].innerText,
				
				$row[10].innerText,
				$row[11].innerText);
		});
}

// Action to send messages when sliders change value.
var requestLayer = function (date, start, peak, end, xPos, yPos, radial) {
    var message = new samp.Message("jhv.layers.show",
    {
        "date": date,
        "start": start,
        "peak": peak,
        "end": end,
        "xPos": xPos,
        "yPos": yPos,
        "radial": radial
    });

    var suc = function (sender, e) {
        console.log("success");
        console.log(sender);
        console.log(e);
    }
    var err = function (sender, e) {
        console.log("error");
        console.log(sender);
        console.log(e);
    }

	connector.runWithConnection(connection => {
		connection.notifyAll([message], suc, err);
	})
    //connector.connection.notifyAll([message], suc, err);
};