//nodeActions.js

// A panel in the graph layout that shows the buttons available whan a node is selected
// This might be replaced by context menu options on the nodes

Ext.define("DARPA.Node_Actions", {

	extend:"Ext.Panel",
	title:'ACTIONS',
	height:'auto',
	width:'auto',
	layout:{
		type:'vbox',
		align:'stretch'
	},
	
	constructor:function(config) {

		var pivot = Ext.create("Ext.Button", {
			text:'PIVOT',
			id: config.id + '-PIVOT',
			disabled:true,
			margin:4,   
			height:24,
			width:58,
			handler:function(but)
			{
				var path = but.id.split('-');
				var graphId = path[0] + '-' + path[1];
				var graph = Ext.getCmp(graphId);
				var node = graph.currentNode;
				if (node) {
					var unpivot = graph.getUnPivotButton();
					if (unpivot) {
						unpivot.enable();
					};
					if (but) {
						but.disable();
					};
					graph.pivot(node);

					AC.logUserActivity("User PIVOTED on a node", "show_graph", AC.WF_CREATE);

				}; // if node
			} // handler
		}); // pivot

		var hide = Ext.create("Ext.Button", {
			text:'HIDE',
			id: config.id + '-HIDE',
			disabled:true,
			margin:4,   
			height:24,
			width:58,
			handler:function(but) {
				var path = but.id.split('-');
				var graphId = path[0] + '-' + path[1];
				var graph = Ext.getCmp(graphId);
				var node = graph.currentNode;
				if (node) {
					if (!(graph.hideNode == undefined)) {
						graph.hideNode(node);
					
					}
					else {
						graph.GraphVis.hideNode(node);	
						graph.prevNode = node;
						graph.currentNode=null;
						var unhide = graph.getUnHideButton();
						if (unhide) {
							unhide.setDisabled(false);
						};
					}

					AC.logUserActivity("User HID a node ", "hide_graph", AC.WF_CREATE);
				};
			} // handler
		}); // hide

		var unpivot = Ext.create("Ext.Button", {
			text:'UNPIVOT',
			id: config.id + '-UNPIVOT',
			disabled:true,
			margin:4,   
			height:24,
			width:58,
			handler:function(but)
			{
				var path = but.id.split('-');
				var graphId = path[0] + '-' + path[1];
				var graph = Ext.getCmp(graphId);
				var node = graph.currentNode;
				if (node) {
					var pivot = graph.getPivotButton();
					if (pivot) {
						pivot.enable();
					}
					if (but) {
						but.disable();
					}
					graph.unpivot('customer', node);

					AC.logUserActivity("User UNPIVOTED on a node", "show_graph", AC.WF_CREATE);

				}; // if node
			} // handler
		}); // unpivot

		var unhide = Ext.create("Ext.Button", {
			text:'UNHIDE', 
			disabled:true, 
			id: config.id + '-UNHIDE',
			margin:4,   
			height:24,
			width:60,
			handler:function(but)
			{
				var path = but.id.split('-');
				var graphId = path[0] + '-' + path[1];
				var graph = Ext.getCmp(graphId);
				if (graph) {
					var prevNode = graph.prevNode;
					if (prevNode) {
						graph.GraphVis.showNode(prevNode);
						graph.currentNode = prevNode;
						but.setDisabled(true);
						
						AC.logUserActivity("User UNHID hidden nodes", "show_graph", AC.WF_CREATE);
						
					}; // if prevNode
				}; // if graph
			} // handler
		} // extend config
		); // extend

		var expand = Ext.create("Ext.Button", {
		    text:'EXPAND',
		    id: config.id+'-EXPAND',
		    disabled:false,
		    margin:4,   
		    height:24,
		    width:58,
		    handler: function(but) {
				var path = but.id.split('-');
				var graphId = path[0] + '-' + path[1];
				var graph = Ext.getCmp(graphId);
				if (graph) {
					var node = graph.currentNode;
					if (node) {
						but.disable();
						graph.expand(node); // expand out 1 hop from this node
						but.enable();
						
						AC.logUserActivity("User EXPANDED a node", "show_graph", AC.WF_CREATE);
					}
				}
		    }
		});

		var unexpand = Ext.create("Ext.Button", {
		    text:'UNEXPAND',
		    id: config.id+'-UNEXPAND',
		    disabled:false,
		    margin:4,   
		    height:24,
		    width:58,
		    handler: function(but) {
				var path = but.id.split('-');
				var graphId = path[0] + '-' + path[1];
				var graph = Ext.getCmp(graphId);
				if (graph) {
					var node = graph.currentNode;
					if (node) {
						but.disable();
						graph.unexpand(node);
						but.enable();
						
						AC.logUserActivity("User UNEXPANDED an expanded node", "hide_graph", AC.WF_CREATE)
					}
				}
		    }
		});
		
		var show = Ext.create("Ext.Button", {
			text:'SHOW',
			id:config.id + '-SHOW',
			disabled:true,
			margin:4,   
			height:24,
			width:58,
			handler: function(but) {
				var path = but.id.split('-');
				var graphId = path[0] + '-' + path[1];
				var graph = Ext.getCmp(graphId);
				var node = graph.currentNode;
				if (node != null) {
					graph.showDetail(node.data()); 
					
					AC.logUserActivity("User SHOWED another tab for the selected entity node", "show_data", AC.WF_CREATE);
				}
		    }
		});

		var stop = Ext.create("Ext.Button", {
			text: "HALT LAYOUT",
			id: config.id + '-STOP',
			disabled: true,
			margin: 4,
			height: 24,
			width: 58,
			last_layout: undefined,
			setCurrentLayout: function(layoutStr) {
				this.last_layout = layoutStr;
			},
			handler: function(btn) {
				var currentLayout = btn.last_layout;
				
				switch (currentLayout) {
					case "COSE" :
						cytoscape.extensions.layout.cose.prototype.stop();
						AC.logUserActivity("User forcefully stopped COSE layout", "stop_animation", AC.WF_CREATE);
						break;
					case "ARBOR":
						cytoscape.extensions.layout.arbor.prototype.stop();
						AC.logUserActivity("User forcefully stopped ARBOR layout", "stop_animation", AC.WF_CREATE);
						break;
					default:
						break;
				}
			}
		});
		
		var help = Ext.create("Ext.Button", {
		    icon: Config.helpIcon,
		    maxHeight: 30,
		    scale: 'medium',
		    margin: 2,
		    style: {marginTop: '2px'},
		    handler: function() {
			    Ext.Msg.alert(
			       'Help',
			       'First select a node then click one of the Action buttons.<br>' +
			       'To select or deselect a node, click on its id or drag it.<br><br>' +
			       '<b>PIVOT</b>:&nbsp;&nbsp;&nbsp;&nbsp; This will perform a search using the selected item and will re-center the graph around this item.<br><br>' +
			       '<b>UNPIVOT</b>:&nbsp;This will perform a search using the previously selected item and will re-center the graph around this item.<br><br>' +
			       '<b>HIDE</b>:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; This will hide the selected item from the graph.<br><br>' +
			       '<b>EXPAND</b>:&nbsp;&nbsp;This will expand out one hop from a selected item. You can only expand an item that has 1 connection.<br><br>' +
			       '<b>UNEXPAND</b>: This will unexpand a previously expanded item.<br><br>' +
			       '<b>Show</b>:&nbsp;&nbsp;&nbsp;&nbsp; This will display a Show for the selected item.<br><br>' +
			       //'<b>TIMELINE+</b>:&nbsp;&nbsp;&nbsp;This will show(+) or hide(-) an interaction timeline for the selected item.<br><br>' +
				'<b>EXPORT GRAPH</b>:&nbsp;&nbsp;&nbsp;&nbsp;This exports the currently displayed graph to a file of your choice.<br><br>' +
				'<b>IMPORT GRAPH</b>:&nbsp;&nbsp;&nbsp;&nbsp;This import a previously exported graph.<br><br>'
			    );
				
				AC.logUserActivity("User clicked the HELP button", "show_instructional_material", AC.WF_CREATE);
			}
		});

		
		var actionButtonsLine1 = Ext.create("Ext.form.FieldSet", { 
			border: 0,
			maxHeight: 40,
			padding: 0,
			margin: 0,
			items:[
			       {
			    	   xtype:'fieldcontainer',
			    	   height:'auto',
			    	   items:[pivot, unpivot, hide, unhide]
			       }
	       		]
		});
		var actionButtonsLine2 = Ext.create("Ext.form.FieldSet", { 
			border: 0,
			maxHeight: 40,
			padding: 0,
			margin: 0,
			items:[
			       {
			    	   xtype:'fieldcontainer',
			    	   height:'auto',
			    	   items:[expand, unexpand, show, stop, help]
			       }
	       		]
			
		});

		this.items = [
		              actionButtonsLine1,
		              actionButtonsLine2
		              ];

	this.callParent(arguments);
	} // constructor

});  

