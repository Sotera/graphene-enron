var overrideCOSE = function() {
	var CoseLayout = cytoscape.extensions.layout.cose;
	
	var lastOptions = undefined;
	var activeInterval = undefined;
	
    CoseLayout.prototype.run = function() {
		var options = lastOptions = this.options;
		var cy      = options.cy;

		// Set DEBUG - Global variable
		if (true == options.debug) {
			DEBUG = true;
		} else {
			DEBUG = false;
		}
		
		if (!options.feedbackRate) {
			options.feedbackRate = 100;
		}
		
		if (!options.onFeedback) {
			options.onFeedback = function(){};
		}
		
		if (!options.intervalRate || options.intervalRate < 1) {
			options.intervalRate = 1;
		}
		
		if (!options.edgeElasticity || options.edgeElasticity == 0) {
			console.error("divisor edgeElasticity cannot be zero");
			return;
		}
		
		// Get start time
		var startTime = new Date();

		// Initialize layout info
		var layoutInfo = createLayoutInfo(cy, options);
		
		// Show LayoutInfo contents if debugging
		if (DEBUG) {
			printLayoutInfo(layoutInfo);
		}

		// If required, randomize node positions
		if (true == options.randomize) {
			randomizePositions(layoutInfo, cy);
		}

		var i = 0, limit = options.numIter, busy = false, done = false;
		
		var intervalFn = function() {
			if (!busy) {
				busy = true;
				
				// Do one step in the physical simulation
				step(layoutInfo, cy, options, i);

				// Update temperature
				layoutInfo.temperature = layoutInfo.temperature * options.coolingFactor;
				logDebug("New temperature: " + layoutInfo.temperature);

				if (layoutInfo.temperature < options.minTemp) {
					logDebug("Temperature drop below minimum threshold. Stopping computation in step " + i);
					done = true;
					//break;
				}
				
				// If required, update positions
				if (0 < options.refresh && 0 == (i % options.refresh) && !done) {
					refreshPositions(layoutInfo, cy, options);
					if (true == options.fit) {
						cy.fit();
					}
				}
				
				// if you you've reached the max number of executions or if the temperature has 
				// cooled significantly, you've finished with the layout
				if (++i == limit || done) {
					clearInterval(activeInterval);
					
					refreshPositions(layoutInfo, cy, options);
					
					// Fit the graph if necessary
					if (true == options.fit) {
						cy.fit();
					}
					
					if (done) {
						console.log("Successfully reached specified minimum temperature.");
						console.log("Total iterations: " + i + ".");
					} else {
						console.log("Exhausted specificied maximum number of iterations.");
					}
					
					// Get end time
					var endTime = new Date();
					console.info("Layout took " + (endTime - startTime) + " ms");
					
					// Layout has finished
					cy.one("layoutstop", options.stop);
					cy.trigger("layoutstop");
				} else {
					// The idea here is eliminate idle downtime by resetting the activeInterval
					// once intervalFn finishes a single execution.
					// Note:  it does not seem to significantly affect overall layout time 
				
					//clearInterval(activeInterval);
					//activeInterval = setInterval(intervalFn, 1);
				}
				busy = false;
			}
		}
		
		activeInterval = setInterval(intervalFn, options.intervalRate);
    };
	
    CoseLayout.prototype.stop = function(){
    	var options = this.options;
    	
		if (typeof options == "undefined") 
			options = lastOptions;
			
    	var cy = options.cy;
		
		if (typeof cy == "undefined") 
			cy = lastOptions.cy;
		
		clearInterval(activeInterval);
		
		cy.one("layoutstop", options.stop);
		cy.trigger("layoutstop");
    };


    /**
     * @brief     : Creates an object which is contains all the data
     *              used in the layout process
     * @arg cy    : cytoscape.js object
     * @return    : layoutInfo object initialized
     */
    function createLayoutInfo(cy, options) {
		var layoutInfo   = {
			layoutNodes  : [], 
			idToIndex    : {},
			nodeSize     : cy.nodes().size(),
			graphSet     : [],
			indexToGraph : [], 
			layoutEdges  : [],
			edgeSize     : cy.edges().size(),
			temperature  : options.initialTemp
		}; 
		
		// Shortcut
		var nodes = cy.nodes();
		
		// Iterate over all nodes, creating layout nodes
		for (var i = 0; i < layoutInfo.nodeSize; i++) {
			var tempNode        = {};
			tempNode.id         = nodes[i].data('id');
			tempNode.parentId   = nodes[i].data('parent');	    
			tempNode.children   = [];
			tempNode.positionX  = nodes[i].position('x');
			tempNode.positionY  = nodes[i].position('y');
			tempNode.offsetX    = 0;	    
			tempNode.offsetY    = 0;
			tempNode.height     = nodes[i].height();
			tempNode.width      = nodes[i].width();
			tempNode.maxX       = tempNode.positionX + tempNode.width  / 2;
			tempNode.minX       = tempNode.positionX - tempNode.width  / 2;
			tempNode.maxY       = tempNode.positionY + tempNode.height / 2;
			tempNode.minY       = tempNode.positionY - tempNode.height / 2;
			tempNode.padLeft    = nodes[i]._private.style['padding-left'].pxValue;
			tempNode.padRight   = nodes[i]._private.style['padding-right'].pxValue;
			tempNode.padTop     = nodes[i]._private.style['padding-top'].pxValue;
			tempNode.padBottom  = nodes[i]._private.style['padding-bottom'].pxValue;
			
			// Add new node
			layoutInfo.layoutNodes.push(tempNode);
			// Add entry to id-index map
			layoutInfo.idToIndex[tempNode.id] = i;
		}

		// Inline implementation of a queue, used for traversing the graph in BFS order
		var queue = [];
		var start = 0;   // Points to the start the queue
		var end   = -1;  // Points to the end of the queue

		var tempGraph = [];

		// Second pass to add child information and 
		// initialize queue for hierarchical traversal
		for (var i = 0; i < layoutInfo.nodeSize; i++) {
			var n = layoutInfo.layoutNodes[i];
			var p_id = n.parentId;
			// Check if node n has a parent node
			if (undefined != p_id) {
				// Add node Id to parent's list of children
				layoutInfo.layoutNodes[layoutInfo.idToIndex[p_id]].children.push(n.id);
			} else {
				// If a node doesn't have a parent, then it's in the root graph
				queue[++end] = n.id;
				tempGraph.push(n.id);		
			}
		}
		
		// Add root graph to graphSet
		layoutInfo.graphSet.push(tempGraph);

		// Traverse the graph, level by level, 
		while (start <= end) {
			// Get the node to visit and remove it from queue
			var node_id  = queue[start++];
			var node_ix  = layoutInfo.idToIndex[node_id];
			var node     = layoutInfo.layoutNodes[node_ix];
			var children = node.children;
			if (children.length > 0) {
				// Add children nodes as a new graph to graph set
				layoutInfo.graphSet.push(children);
				// Add children to que queue to be visited
				for (var i = 0; i < children.length; i++) {
					queue[++end] = children[i];
				}
			}
		}

		// Create indexToGraph map
		for (var i = 0; i < layoutInfo.graphSet.length; i++) {	    
			var graph = layoutInfo.graphSet[i];
			for (var j = 0; j < graph.length; j++) {
				var index = layoutInfo.idToIndex[graph[j]];
				layoutInfo.indexToGraph[index] = i;
			}
		}

		// Shortcut
		var edges = cy.edges();
		
		// Iterate over all edges, creating Layout Edges
		for (var i = 0; i < layoutInfo.edgeSize; i++) {
			var e = edges[i];
			var tempEdge = {};	    
			tempEdge.id       = e.data('id');
			tempEdge.sourceId = e.data('source');
			tempEdge.targetId = e.data('target');

			// Compute ideal length
			var idealLength = options.idealEdgeLength;

			// Check if it's an inter graph edge
			var sourceIx    = layoutInfo.idToIndex[tempEdge.sourceId];
			var targetIx    = layoutInfo.idToIndex[tempEdge.targetId];
			var sourceGraph = layoutInfo.indexToGraph[sourceIx];
			var targetGraph = layoutInfo.indexToGraph[targetIx];

			if (sourceGraph != targetGraph) {
			// Find lowest common graph ancestor
			var lca = findLCA(tempEdge.sourceId, tempEdge.targetId, layoutInfo);

			// Compute sum of node depths, relative to lca graph
			var lcaGraph = layoutInfo.graphSet[lca];
			var depth    = 0;

			// Source depth
			var tempNode = layoutInfo.layoutNodes[sourceIx];
			while (-1 == $.inArray(tempNode.id, lcaGraph)) {
				tempNode = layoutInfo.layoutNodes[layoutInfo.idToIndex[tempNode.parentId]];
				depth++;
			}

			// Target depth
			tempNode = layoutInfo.layoutNodes[targetIx];
			while (-1 == $.inArray(tempNode.id, lcaGraph)) {
				tempNode = layoutInfo.layoutNodes[layoutInfo.idToIndex[tempNode.parentId]];
				depth++;
			}

			logDebug("LCA of nodes " + tempEdge.sourceId + " and " + tempEdge.targetId +  
				 ". Index: " + lca + " Contents: " + lcaGraph.toString() + 
				 ". Depth: " + depth);

			// Update idealLength
			idealLength *= depth * options.nestingFactor;
			}

			tempEdge.idealLength = idealLength;

			layoutInfo.layoutEdges.push(tempEdge);
		}

		// Finally, return layoutInfo object
		return layoutInfo;
    }

    
    /**
     * @brief : This function finds the index of the lowest common 
     *          graph ancestor between 2 nodes in the subtree 
     *          (from the graph hierarchy induced tree) whose
     *          root is graphIx
     *
     * @arg node1: node1's ID
     * @arg node2: node2's ID
     * @arg layoutInfo: layoutInfo object
     *
     */
    function findLCA(node1, node2, layoutInfo) {
		// Find their common ancester, starting from the root graph
		var res = findLCA_aux(node1, node2, 0, layoutInfo);
		if (2 > res.count) {
			// If aux function couldn't find the common ancester, 
			// then it is the root graph
			return 0;
		} else {
			return res.graph;
		}
    }


    /**
     * @brief          : Auxiliary function used for LCA computation
     * 
     * @arg node1      : node1's ID
     * @arg node2      : node2's ID
     * @arg graphIx    : subgraph index
     * @arg layoutInfo : layoutInfo object
     *
     * @return         : object of the form {count: X, graph: Y}, where:
     *                   X is the number of ancesters (max: 2) found in 
     *                   graphIx (and it's subgraphs),
     *                   Y is the graph index of the lowest graph containing 
     *                   all X nodes
     */
    function findLCA_aux(node1, node2, graphIx, layoutInfo) {
		var graph = layoutInfo.graphSet[graphIx];
		// If both nodes belongs to graphIx
		if (-1 < $.inArray(node1, graph) && -1 < $.inArray(node2, graph)) {
			return {count:2, graph:graphIx};
		}

		// Make recursive calls for all subgraphs
		var c = 0;
		for (var i = 0; i < graph.length; i++) {
			var nodeId   = graph[i];
			var nodeIx   = layoutInfo.idToIndex[nodeId];
			var children = layoutInfo.layoutNodes[nodeIx].children;

			// If the node has no child, skip it
			if (0 == children.length) {
			continue;
			}

			var childGraphIx = layoutInfo.indexToGraph[layoutInfo.idToIndex[children[0]]];
			var result = findLCA_aux(node1, node2, childGraphIx, layoutInfo);
			if (0 == result.count) {
			// Neither node1 nor node2 are present in this subgraph
			continue;
			} else if (1 == result.count) {
			// One of (node1, node2) is present in this subgraph
			c++;
			if (2 == c) {
				// We've already found both nodes, no need to keep searching
				break;
			}
			} else {
			// Both nodes are present in this subgraph
			return result;
			}	    
		}
		
		return {count:c, graph:graphIx};
    }


    /**
     * @brief: printsLayoutInfo into js console
     *         Only used for debbuging 
     */
    function printLayoutInfo(layoutInfo) {
		if (!DEBUG) {
			return;
		}
		console.debug("layoutNodes:");
		for (var i = 0; i < layoutInfo.nodeSize; i++) {
			var n = layoutInfo.layoutNodes[i];
			var s = 
			"\nindex: "     + i + 
			"\nId: "        + n.id + 
			"\nChildren: "  + n.children.toString() +  
			"\nparentId: "  + n.parentId  + 
			"\npositionX: " + n.positionX + 
			"\npositionY: " + n.positionY +
			"\nOffsetX: " + n.offsetX + 
			"\nOffsetY: " + n.offsetY + 
			"\npadLeft: " + n.padLeft + 
			"\npadRight: " + n.padRight + 
			"\npadTop: " + n.padTop + 
			"\npadBottom: " + n.padBottom;

			console.debug(s);		
		}	
		
		console.debug("idToIndex");
		for (var i in layoutInfo.idToIndex) {
			console.debug("Id: " + i + "\nIndex: " + layoutInfo.idToIndex[i]);
		}

		console.debug("Graph Set");
		var set = layoutInfo.graphSet;
		for (var i = 0; i < set.length; i ++) {
			console.debug("Set : " + i + ": " + set[i].toString());
		} 

		var s = "IndexToGraph";
		for (var i = 0; i < layoutInfo.indexToGraph.length; i ++) {
			s += "\nIndex : " + i + " Graph: "+ layoutInfo.indexToGraph[i];
		}
		console.debug(s);

		s = "Layout Edges";
		for (var i = 0; i < layoutInfo.layoutEdges.length; i++) {
			var e = layoutInfo.layoutEdges[i];
			s += "\nEdge Index: " + i + " ID: " + e.id + 
			" SouceID: " + e.sourceId + " TargetId: " + e.targetId + 
			" Ideal Length: " + e.idealLength;
		}
		console.debug(s);

		s =  "nodeSize: " + layoutInfo.nodeSize;
		s += "\nedgeSize: " + layoutInfo.edgeSize;
		s += "\ntemperature: " + layoutInfo.temperature;
		console.debug(s);

		return;
    }


    /**
     * @brief : Randomizes the position of all nodes
     */
    function randomizePositions(layoutInfo, cy) {
		var container = cy.container();
		var width     = container.clientWidth;
		var height    = container.clientHeight;

		for (var i = 0; i < layoutInfo.nodeSize; i++) {
			var n = layoutInfo.layoutNodes[i];
			// No need to randomize compound nodes
			if (0 == n.children.length) {
			n.positionX = Math.random() * width;
			n.positionY = Math.random() * height;
			}
		}
    }

    
    /**
     * @brief          : Updates the positions of nodes in the network
     * @arg layoutInfo : LayoutInfo object
     * @arg cy         : Cytoscape object
     * @arg options    : Layout options
     */
    function refreshPositions(layoutInfo, cy, options) {
		var container = cy.container();
		var width     = container.clientWidth;
		var height    = container.clientHeight;
		
		var s = "Refreshing positions";
		logDebug(s);

		cy.nodes().positions(function(i, ele) {
			lnode = layoutInfo.layoutNodes[layoutInfo.idToIndex[ele.data('id')]];
			s = "Node: " + lnode.id + ". Refreshed position: (" + 
			lnode.positionX + ", " + lnode.positionY + ").";
			logDebug(s);
			return {
			x: lnode.positionX,
			y: lnode.positionY
			};
		});

		// Trigger layoutReady only on first call
		if (true != refreshPositions.ready) {
			s = "Triggering layoutready";
			logDebug(s);
			refreshPositions.ready = true;
			cy.one("layoutready", options.ready);
			cy.trigger("layoutready");
		}
    }


    /**
     * @brief          : Performs one iteration of the physical simulation
     * @arg layoutInfo : LayoutInfo object already initialized
     * @arg cy         : Cytoscape object
     * @arg options    : Layout options
     */
    function step(layoutInfo, cy, options, step) {	
		var s = "\n\n###############################";
		s += "\nSTEP: " + step;
		s += "\n###############################\n";
		logDebug(s);

		// Calculate node repulsions
		// var time = new Date();
		calculateNodeForces(layoutInfo, cy, options);
		// time = new Date() - time;
		// console.log("--calculateNodeForces: " + time + " ms.");
		
		// Calculate edge forces
		// time = new Date();
		calculateEdgeForces(layoutInfo, cy, options);
		// time = new Date() - time;
		// console.log("--calculateEdgeForces: " + time + " ms.");
		
		// Calculate gravity forces
		// time = new Date();
		calculateGravityForces(layoutInfo, cy, options);
		// time = new Date() - time;
		// console.log("--calculateGravityForces: " + time + " ms.");
		
		// Propagate forces from parent to child
		// time = new Date();
		propagateForces(layoutInfo, cy, options);
		// time = new Date() - time;
		// console.log("--propagationForces: " + time + " ms.");
		
		// Update positions based on calculated forces
		// time = new Date();
		updatePositions(layoutInfo, cy, options);
		// time = new Date() - time;
		// console.log("--updatePositions: " + time + " ms.");
		
		if ( step % options.feedbackRate == 0 ) {
			options.onFeedback({
				step: step,
				iter: options.numIter,
				temp: layoutInfo.temperature
			});
		}
    }

    
    /**
     * @brief : Computes the node repulsion forces
     */
    function calculateNodeForces(layoutInfo, cy, options) {
		// Go through each of the graphs in graphSet
		// Nodes only repel each other if they belong to the same graph
    	// FIXME: This function (and nodeRepulsion()) is the most time-intensive subfunction of step().  Optimize in any way possible.
    	
    	var i, j, k, node1, node2, graph, numNodes;
    	var l = layoutInfo.graphSet.length;
    	
		for (i = 0; i < l; i += 1) {
			graph    = layoutInfo.graphSet[i];
			numNodes = graph.length;

			// Now get all the pairs of nodes 
			// Only get each pair once, (A, B) = (B, A)
			for (j = 0; j < numNodes; j += 1) {
				node1 = layoutInfo.layoutNodes[layoutInfo.idToIndex[graph[j]]];
				
				for (k = j + 1; k < numNodes; k += 1) {
					node2 = layoutInfo.layoutNodes[layoutInfo.idToIndex[graph[k]]];
					nodeRepulsion(node1, node2, layoutInfo, cy, options);
				} 
			}
		} 
    }


    /**
     * @brief : Compute the node repulsion forces between a pair of nodes
     */
    function nodeRepulsion(node1, node2, layoutInfo, cy, options) {

		// Get direction of line connecting both node centers
		var directionX = node2.positionX - node1.positionX;
		var directionY = node2.positionY - node1.positionY;

		// If both centers are the same, apply a random force
		if (0 == directionX && 0 == directionY) {
			return; // TODO
		}

		overlap = nodesOverlap(node1, node2, directionX, directionY);
		
		if (overlap > 0) {
			// If nodes overlap, repulsion force is proportional 
			// to the overlap
			var force    = options.nodeOverlap * overlap;

			// Compute the module and components of the force vector
			var distance = Math.sqrt(directionX * directionX + directionY * directionY);
			var forceX   = force * directionX / distance;
			var forceY   = force * directionY / distance;

		} else {
			// If there's no overlap, force is inversely proportional 
			// to squared distance

			// Get clipping points for both nodes
			var point1 = findClippingPoint(node1, directionX, directionY);
			var point2 = findClippingPoint(node2, -1 * directionX, -1 * directionY);

			// Use clipping points to compute distance
			var distanceX   = point2.x - point1.x;
			var distanceY   = point2.y - point1.y;
			var distanceSqr = distanceX * distanceX + distanceY * distanceY;
			var distance    = Math.sqrt(distanceSqr);

			// Compute the module and components of the force vector
			var force  = options.nodeRepulsion / distanceSqr;
			var forceX = force * distanceX / distance;
			var forceY = force * distanceY / distance;
		}

		// Apply force
		node1.offsetX -= forceX;
		node1.offsetY -= forceY;
		node2.offsetX += forceX;
		node2.offsetY += forceY;

		return;
    }


    /**
     * @brief : Finds the point in which an edge (direction dX, dY) intersects 
     *          the rectangular bounding box of it's source/target node 
     */
    function findClippingPoint(node, dX, dY) {

		// Shorcuts
		var X = node.positionX;
		var Y = node.positionY;
		var H = node.height;
		var W = node.width;
		var dirSlope     = dY / dX;
		var nodeSlope    = H / W;
		var nodeinvSlope = W / H;

		var s = "Computing clipping point of node " + node.id + 
			" . Height:  " + H + ", Width: " + W + 
			"\nDirection " + dX + ", " + dY; 
		
		// Compute intersection
		var res = {};
		do {
			// Case: Vertical direction (up)
			if (0 == dX && 0 < dY) {
			res.x = X;
			s += "\nUp direction";
			res.y = Y + H / 2;
			break;
			}

			// Case: Vertical direction (down)
			if (0 == dX && 0 > dY) {
			res.x = X;
			res.y = Y + H / 2;
			s += "\nDown direction";
			break;
			}	    

			// Case: Intersects the right border
			if (0 < dX && 
			-1 * nodeSlope <= dirSlope && 
			dirSlope <= nodeSlope) {
			res.x = X + W / 2;
			res.y = Y + (W * dY / 2 / dX);
			s += "\nRightborder";
			break;
			}

			// Case: Intersects the left border
			if (0 > dX && 
			-1 * nodeSlope <= dirSlope && 
			dirSlope <= nodeSlope) {
			res.x = X - W / 2;
			res.y = Y - (W * dY / 2 / dX);
			s += "\nLeftborder";
			break;
			}

			// Case: Intersects the top border
			if (0 < dY && 
			( dirSlope <= -1 * nodeSlope ||
			  dirSlope >= nodeSlope )) {
			res.x = X + (H * dX / 2 / dY);
			res.y = Y + H / 2;
			s += "\nTop border";
			break;
			}

			// Case: Intersects the bottom border
			if (0 > dY && 
			( dirSlope <= -1 * nodeSlope ||
			  dirSlope >= nodeSlope )) {
			res.x = X - (H * dX / 2 / dY);
			res.y = Y - H / 2;
			s += "\nBottom border";
			break;
			}

		} while (false);

		s += "\nClipping point found at " + res.x + ", " + res.y;
		logDebug(s);
		return res;
    }


    /**
     * @brief  : Determines whether two nodes overlap or not
     * @return : Amount of overlapping (0 => no overlap)
     */
    function nodesOverlap(node1, node2, dX, dY) {

		if (dX > 0) {
			var overlapX = node1.maxX - node2.minX;
		} else {
			var overlapX = node2.maxX - node1.minX;
		}

		if (dY > 0) {
			var overlapY = node1.maxY - node2.minY;
		} else {
			var overlapY = node2.maxY - node1.minY;
		}

		if (overlapX >= 0 && overlapY >= 0) {
			return Math.sqrt(overlapX * overlapX + overlapY * overlapY);
		} else {
			return 0;
		}
    }
        
    
    /**
     * @brief : Calculates all edge forces
     */
    function calculateEdgeForces(layoutInfo, cy, options) {
		// Iterate over all edges
		for (var i = 0; i < layoutInfo.edgeSize; i++) {
			// Get edge, source & target nodes
			var edge     = layoutInfo.layoutEdges[i];
			var sourceIx = layoutInfo.idToIndex[edge.sourceId];
			var source   = layoutInfo.layoutNodes[sourceIx];
			var targetIx = layoutInfo.idToIndex[edge.targetId];
			var target   = layoutInfo.layoutNodes[targetIx];

			// Get direction of line connecting both node centers
			var directionX = target.positionX - source.positionX;
			var directionY = target.positionY - source.positionY;
			
			// If both centers are the same, do nothing.
			// A random force has already been applied as node repulsion
			if (0 == directionX && 0 == directionY) {
			return;
			}

			// Get clipping points for both nodes
			var point1 = findClippingPoint(source, directionX, directionY);
			var point2 = findClippingPoint(target, -1 * directionX, -1 * directionY);


			var lx = point2.x - point1.x;
			var ly = point2.y - point1.y;
			var l  = Math.sqrt(lx * lx + ly * ly);

			var force  = Math.pow(edge.idealLength - l, 2) / options.edgeElasticity; 

			if (0 != l) {
			var forceX = force * lx / l;
			var forceY = force * ly / l;
			} else {
			var forceX = 0;
			var forceY = 0;
			}

			// Add this force to target and source nodes
			source.offsetX += forceX;
			source.offsetY += forceY;
			target.offsetX -= forceX;
			target.offsetY -= forceY;

			var s = "Edge force between nodes " + source.id + " and " + target.id;
			s += "\nDistance: " + l + " Force: (" + forceX + ", " + forceY + ")";
			logDebug(s);
		}
    }


    /**
     * @brief : Computes gravity forces for all nodes
     */
    function calculateGravityForces(layoutInfo, cy, options) {
		var s = "calculateGravityForces";
		logDebug(s);
		for (var i = 0; i < layoutInfo.graphSet.length; i ++) {
			var graph    = layoutInfo.graphSet[i];
			var numNodes = graph.length;

			s = "Set: " + graph.toString();
			logDebug(s);
					
			// Compute graph center
			if (0 == i) {
			var container = cy.container();		
			var centerX   = container.clientHeight / 2;
			var centerY   = container.clientWidth  / 2;		
			} else {
			// Get Parent node for this graph, and use its position as center
			var temp    = layoutInfo.layoutNodes[layoutInfo.idToIndex[graph[0]]];
			var parent  = layoutInfo.layoutNodes[layoutInfo.idToIndex[temp.parentId]];
			var centerX = parent.positionX;
			var centerY = parent.positionY;
			}
			s = "Center found at: " + centerX + ", " + centerY;
			logDebug(s);

			// Apply force to all nodes in graph
			for (var j = 0; j < numNodes; j++) {
			var node = layoutInfo.layoutNodes[layoutInfo.idToIndex[graph[j]]];
			s = "Node: " + node.id;
			var dx = centerX - node.positionX;
			var dy = centerY - node.positionY;
			var d  = Math.sqrt(dx * dx + dy * dy);
			if (d > 1.0) { // TODO: Use global variable for distance threshold
				var fx = options.gravity * dx / d;
				var fy = options.gravity * dy / d;
				node.offsetX += fx;
				node.offsetY += fy;
				s += ": Applied force: " + fx + ", " + fy;
			} else {
				s += ": skypped since it's too close to center";
			}
			logDebug(s);
			}
		}
    }


    /**
     * @brief          : This function propagates the existing offsets from 
     *                   parent nodes to its descendents.
     * @arg layoutInfo : layoutInfo Object
     * @arg cy         : cytoscape Object
     * @arg options    : Layout options
     */
    function propagateForces(layoutInfo, cy, options) {	
		// Inline implementation of a queue, used for traversing the graph in BFS order
		var queue = [];
		var start = 0;   // Points to the start the queue
		var end   = -1;  // Points to the end of the queue

		logDebug("propagateForces");

		// Start by visiting the nodes in the root graph
		queue.push.apply(queue, layoutInfo.graphSet[0]);
		end += layoutInfo.graphSet[0].length;

		// Traverse the graph, level by level, 
		while (start <= end) {
			// Get the node to visit and remove it from queue
			var nodeId    = queue[start++];
			var nodeIndex = layoutInfo.idToIndex[nodeId];
			var node      = layoutInfo.layoutNodes[nodeIndex];
			var children  = node.children;

			// We only need to process the node if it's compound
			if (0 < children.length) {		
			var offX = node.offsetX;
			var offY = node.offsetY;

			var s = "Propagating offset from parent node : " + node.id + 
				". OffsetX: " + offX + ". OffsetY: " + offY;
			s += "\n Children: " + children.toString();
			logDebug(s);
			
			for (var i = 0; i < children.length; i++) {
				var childNode = layoutInfo.layoutNodes[layoutInfo.idToIndex[children[i]]];
				// Propagate offset
				childNode.offsetX += offX;
				childNode.offsetY += offY;
				// Add children to queue to be visited
				queue[++end] = children[i];
			}
			
			// Reset parent offsets
			node.offsetX = 0;
			node.offsetY = 0;
			}
			
		}
    }


    /**
     * @brief : Updates the layout model positions, based on 
     *          the accumulated forces
     */
    function updatePositions(layoutInfo, cy, options) {
		var s = "Updating positions";
		logDebug(s);

		// Reset boundaries for compound nodes
		for (var i = 0; i < layoutInfo.nodeSize; i++) {
			var n = layoutInfo.layoutNodes[i];
			if (0 < n.children.length) {
			logDebug("Resetting boundaries of compound node: " + n.id);
			n.maxX = undefined;
			n.minX = undefined;
			n.maxY = undefined;
			n.minY = undefined;
			}
		}

		for (var i = 0; i < layoutInfo.nodeSize; i++) {
			var n = layoutInfo.layoutNodes[i];
			if (0 < n.children.length) {
			// No need to set compound node position
			logDebug("Skipping position update of node: " + n.id);
			continue;
			}
			s = "Node: " + n.id + " Previous position: (" + 
			n.positionX + ", " + n.positionY + ")."; 

			// Limit displacement in order to improve stability
			var tempForce = limitForce(n.offsetX, n.offsetY, layoutInfo.temperature);
			n.positionX += tempForce.x; 
			n.positionY += tempForce.y;
			n.offsetX = 0;
			n.offsetY = 0;
			n.minX    = n.positionX - n.width; 
			n.maxX    = n.positionX + n.width; 
			n.minY    = n.positionY - n.height; 
			n.maxY    = n.positionY + n.height; 
			s += " New Position: (" + n.positionX + ", " + n.positionY + ").";
			logDebug(s);

			// Update ancestry boudaries
			updateAncestryBoundaries(n, layoutInfo);
		}

		// Update size, position of compund nodes
		for (var i = 0; i < layoutInfo.nodeSize; i++) {
			var n = layoutInfo.layoutNodes[i];
			if (0 < n.children.length) {
			n.positionX = (n.maxX + n.minX) / 2;
			n.positionY = (n.maxY + n.minY) / 2;
			n.width     = n.maxX - n.minX;
			n.height    = n.maxY - n.minY;
			s = "Updating position, size of compound node " + n.id;
			s += "\nPositionX: " + n.positionX + ", PositionY: " + n.positionY;
			s += "\nWidth: " + n.width + ", Height: " + n.height;
			logDebug(s);
			}
		}	
    }


    /**
     * @brief : Limits a force (forceX, forceY) to be not 
     *          greater (in modulo) than max. 
     8          Preserves force direction. 
     */
    function limitForce(forceX, forceY, max) {
		var s = "Limiting force: (" + forceX + ", " + forceY + "). Max: " + max;
		var force = Math.sqrt(forceX * forceX + forceY * forceY);

		if (force > max) {
			var res = {
			x : max * forceX / force,
			y : max * forceY / force
			};	    

		} else {
			var res = {
			x : forceX,
			y : forceY
			};
		}

		s += ".\nResult: (" + res.x + ", " + res.y + ")";
		logDebug(s);

		return res;
    }


    /**
     * @brief : Function used for keeping track of compound node 
     *          sizes, since they should bound all their subnodes.
     */
    function updateAncestryBoundaries(node, layoutInfo) {
		var s = "Propagating new position/size of node " + node.id;
		var parentId = node.parentId;
		if (undefined == parentId) {
			// If there's no parent, we are done
			s += ". No parent node.";
			logDebug(s);
			return;
		}

		// Get Parent Node
		var p = layoutInfo.layoutNodes[layoutInfo.idToIndex[parentId]];
		var flag = false;

		// MaxX
		if (undefined == p.maxX || node.maxX + p.padRight > p.maxX) {
			p.maxX = node.maxX + p.padRight;
			flag = true;
			s += "\nNew maxX for parent node " + p.id + ": " + p.maxX;
		}

		// MinX
		if (undefined == p.minX || node.minX - p.padLeft < p.minX) {
			p.minX = node.minX - p.padLeft;
			flag = true;
			s += "\nNew minX for parent node " + p.id + ": " + p.minX;
		}

		// MaxY
		if (undefined == p.maxY || node.maxY + p.padBottom > p.maxY) {
			p.maxY = node.maxY + p.padBottom;
			flag = true;
			s += "\nNew maxY for parent node " + p.id + ": " + p.maxY;
		}

		// MinY
		if (undefined == p.minY || node.minY - p.padTop < p.minY) {
			p.minY = node.minY - p.padTop;
			flag = true;
			s += "\nNew minY for parent node " + p.id + ": " + p.minY;
		}

		// If updated boundaries, propagate changes upward
		if (flag) {
			logDebug(s);
			return updateAncestryBoundaries(p, layoutInfo);
		} 

		s += ". No changes in boundaries/position of parent node " + p.id;  
		logDebug(s);
		return;
    }


    /**
     * @brief : Logs a debug message in JS console, if DEBUG is ON
     */
    function logDebug(text) {
		if (DEBUG) {
			console.debug(text);
		}
    }


    // register the layout
    // cytoscape("layout", "cose", CoseLayout);
};

var overrideARBOR = function() {
	var ArborLayout = cytoscape.extensions.layout.arbor;
		
	ArborLayout.prototype.run = function(){
		var options = this.options;
		var cy = options.cy;
		var nodes = cy.nodes();
		var edges = cy.edges();
		var container = cy.container();
		var width = container.clientWidth;
		var height = container.clientHeight;
		var simulationBounds = options.simulationBounds;

		if( options.simulationBounds ){
			width = simulationBounds[2] -  simulationBounds[0]; // x2 - x1
			height = simulationBounds[3] - simulationBounds[1]; // y2 - y1
		} else {
			options.simulationBounds = [
				0,
				0, 
				width,
				height
			];
		}

		// make nice x & y fields
		var simBB = options.simulationBounds;
		simBB.x1 = simBB[0];
		simBB.y1 = simBB[1];
		simBB.x2 = simBB[2];
		simBB.y2 = simBB[3];

		// arbor doesn't work with just 1 node
		if( cy.nodes().size() <= 1 ){
			if( options.fit ){
				cy.reset();
			}

			cy.nodes().position({
				x: Math.round( (simBB.x1 + simBB.x2)/2 ),
				y: Math.round( (simBB.y1 + simBB.y2)/2 )
			});

			cy.one("layoutstop", options.stop);
			cy.trigger("layoutstop");

			cy.one("layoutstop", options.stop);
			cy.trigger("layoutstop");

			return;
		}

		var sys = this.system = arbor.ParticleSystem(options.repulsion, options.stiffness, options.friction, options.gravity, options.fps, options.dt, options.precision);
		ArborLayout.prototype.system = sys;
		
		if( options.liveUpdate && options.fit ){
			cy.reset();
		};
		
		var doneTime = 250;
		var doneTimeout;
		
		var ready = false;
		
		var lastDraw = +new Date;
		var sysRenderer = {
			init: function(system){
			},
			redraw: function(){
				var energy = sys.energy();

				// if we're stable (according to the client), we're done
				if( options.stableEnergy != null && energy != null && energy.n > 0 && options.stableEnergy(energy) ){
					sys.stop();
					return;
				}

				clearTimeout(doneTimeout);
				doneTimeout = setTimeout(doneHandler, doneTime);
				
				var movedNodes = [];
				
				sys.eachNode(function(n, point){ 
					var id = n.name;
					var data = n.data;
					var node = data.element;
					
					if( node == null ){
						return;
					}
					var pos = node._private.position;
					
					if( !node.locked() && !node.grabbed() ){
						pos.x = simBB.x1 + point.x;
						pos.y = simBB.y1 + point.y;
						
						movedNodes.push( node );
					}
				});
				

				var timeToDraw = (+new Date - lastDraw) >= 16;
				if( options.liveUpdate && movedNodes.length > 0 && timeToDraw ){
					//new $$.Collection(cy, movedNodes).rtrigger("position");
					new cytoscape.Collection(cy, movedNodes).rtrigger("position");
					lastDraw = +new Date;
				}

				
				if( !ready ){
					ready = true;
					cy.one("layoutready", options.ready);
					cy.trigger("layoutready");
				}
			}
			
		};
		sys.renderer = sysRenderer;
		sys.screenSize( width, height );
		sys.screenPadding( options.padding[0], options.padding[1], options.padding[2], options.padding[3] );
		sys.screenStep( options.stepSize );

		function calculateValueForElement(element, value){
			if( value == null ){
				return undefined;
			} else if( typeof value == typeof function(){} ){
				return value.apply(element, [element._private.data, {
					nodes: nodes.length,
					edges: edges.length,
					element: element
				}]); 
			} else {
				return value;
			}
		}
		
		// TODO we're using a hack; sys.toScreen should work :(
		function fromScreen(pos){
			var x = pos.x;
			var y = pos.y;
			var w = width;
			var h = height;
			
			var left = -2;
			var right = 2;
			var top = -2;
			var bottom = 2;
			
			var d = 4;
			
			return {
				x: x/w * d + left,
				y: y/h * d + right
			};
		}
		
		var grabHandler = function(e){
			grabbed = this;
			var pos = sys.fromScreen( this.position() );
			var p = arbor.Point(pos.x, pos.y);
			this.scratch().arbor.p = p;
			
			switch( e.type ){
			case "grab":
				this.scratch().arbor.fixed = true;
				break;
			case "dragstop":
				this.scratch().arbor.fixed = false;
				this.scratch().arbor.tempMass = 1000;
				break;
			}
		};
		nodes.bind("grab drag dragstop", grabHandler);
			  	
		nodes.each(function(i, node){
			var id = this._private.data.id;
			var mass = calculateValueForElement(this, options.nodeMass);
			var locked = this._private.locked;
			
			var pos = fromScreen({
				x: node.position().x,
				y: node.position().y
			});

			if( node.locked() ){
				return;
			}

			this.scratch().arbor = sys.addNode(id, {
				element: this,
				mass: mass,
				fixed: locked,
				x: locked ? pos.x : undefined,
				y: locked ? pos.y : undefined
			});
		});
		
		edges.each(function(){
			var id = this.id();
			var src = this.source().id();
			var tgt = this.target().id();
			var length = calculateValueForElement(this, options.edgeLength);
			
			this.scratch().arbor = sys.addEdge(src, tgt, {
				length: length
			});
		});
		
		function packToCenter(callback){
			// TODO implement this for IE :(
			
			if( options.fit ){
				cy.fit();
			}
			callback();
		};
		
		var grabbableNodes = nodes.filter(":grabbable");
		// disable grabbing if so set
		if( options.ungrabifyWhileSimulating ){
			grabbableNodes.ungrabify();
		}
		
		var doneHandler = function(){
			if( window.isIE ){
				packToCenter(function(){
					done();
				});
			} else {
				done();
			}
			
			function done(){
				if( !options.liveUpdate ){
					if( options.fit ){
						cy.reset();
					}

					cy.nodes().rtrigger("position");
				}

				// unbind handlers
				nodes.unbind("grab drag dragstop", grabHandler);
				
				// enable back grabbing if so set
				if( options.ungrabifyWhileSimulating ){
					grabbableNodes.grabify();
				}

				cy.one("layoutstop", options.stop);
				cy.trigger("layoutstop");
			}
		};
		
		sys.start();
		setTimeout(function(){
			sys.stop();
		}, options.maxSimulationTime);
		
	};
	
	ArborLayout.prototype.stop = function(){
		if( this.system != null ){
			this.system.stop();
		}
	};
};

var overrideRegisterInstance = function() {
	if (!cytoscape) return;
	
	var $$ = cytoscape;
	$$.registerInstance = function( instance, domElement ){
		var cy;

		if( $$.is.core(instance) ){
			cy = instance;
		} else if( $$.is.domElement(instance) ){
			domElement = instance;
		}

		// if we have an old reg that is empty (no cy), then 
		var oldReg = $$.getRegistrationForInstance(instance, domElement);
		if( oldReg ){
			// MFM OMIT if( !oldReg.cy ){
				oldReg.cy = instance;
				oldReg.domElement = domElement;
			// MFM OMIT } 
                        // MFM OMIT THIS CHECK
                        // else {
			//	$$.util.error('Tried to register on a pre-existing registration');
			//}

			return oldReg;

		// otherwise, just make a new registration
		} else {
			var time = +new Date;
			var suffix;

			// add a suffix in case instances collide on the same time
			if( !$$.lastInstanceTime || $$.lastInstanceTime === time ){
				$$.instanceCounter = 0;
			} else {
				++$$.instanceCounter;
			}
			$$.lastInstanceTime = time;
			suffix = $$.instanceCounter;

			var id = "cy-" + time + "-" + suffix;

			// create the registration object
			var registration = {
				id: id,
				cy: cy,
				domElement: domElement,
				readies: [] // list of bound ready functions before calling init
			};

			// put the registration object in the pool
			$$.instances.push( registration );
			$$.instances[ id ] = registration;

			return registration;
		}
	};
};