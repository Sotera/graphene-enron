Ext.define("DARPA.GraphManagerMenu", {
	extend: "Ext.menu.Menu",
	
	graphRef: undefined,
	
	constructor: function(config) {
		var scope = this;
		
		scope.items = [{
			text: "Export Graph",
			handler: function(item) {
				var gr;
				if (scope.graphRef) {
					gr = scope.graphRef;
				} else {
					var menu = item.parentMenu;
					var toolbar = menu.up().up();
					gr = toolbar.up();
				}
				
				AC.logUserActivity("User initiated export", "open_modal_tools", AC.WF_CREATE);
				gr.exportGraph();
			}
		}, {
			text: "Import Graph",
			handler: function(item) {
				var gr;
				if (scope.graphRef) {
					gr = scope.graphRef;
				} else {
					var menu = item.parentMenu;
					var toolbar = menu.up().up();
					gr = toolbar.up();
				}
				AC.logUserActivity("User initiated import", "open_modal_tools", AC.WF_CREATE);
				Ext.Msg.confirm('Confirm', 
					'Importing another graph will delete the current graph. Are you sure you want to Import?', 
					function(ans) {
						if (ans == 'yes') {
							AC.logUserActivity("User confirmed import", "import_data", AC.WF_ENRICH);
							gr.importGraph();
						} else {
							AC.logUserActivity("User canceled import", "import_data", AC.WF_ENRICH);
						}
					}
				);
			}
		}, {
			text: "Save Graph",
			handler: function(item) {
				var gr;
				if (scope.graphRef) {
					gr = scope.graphRef;
				} else {
					var menu = item.parentMenu;
					var toolbar = menu.up().up();
					gr = toolbar.up();
				}
				
				AC.logUserActivity("User saved the current graph", "export_data", AC.WF_ENRICH);
				gr.saveGraph();
			}
		}, {
			text: "Restore Graph",
			handler: function(item) {
				var gr;
				if (scope.graphRef) {
					gr = scope.graphRef;
				} else {
					var menu = item.parentMenu;
					var toolbar = menu.up().up();
					gr = toolbar.up();
				}
				
				AC.logUserActivity("User initiated a graph load", "import_data", AC.WF_ENRICH);
				Ext.Msg.confirm('Confirm', 
					'Restoring a saved graph will delete the current graph. Are you sure you want to Restore?', 
					function(ans) {
						if (ans == 'yes') {
							AC.logUserActivity("User confirmed graph load", "import_data", AC.WF_ENRICH);
							gr.restoreGraph(null);
						} else {
							AC.logUserActivity("User canceled graph load", "import_data", AC.WF_ENRICH);
						}
					}
				);
			}
		}];
		
		scope.callParent(arguments);
	}
});