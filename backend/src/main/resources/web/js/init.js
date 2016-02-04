
    function frontendApp() {
        return akka.viz.frontend.FrontendApp()
    }

    var graph = Viva.Graph.graph();

    var nodeSize = 40;
    var idealLength = 5 * nodeSize;
    var layout = Viva.Graph.Layout.forceDirected(graph, {
        springLength: idealLength,
        springCoeff: 0.0008,
        dragCoeff: 0.02,
        gravity: -1.2
    });

    var graphics = Viva.Graph.View.svgGraphics();
    var highlightRelatedNodes = function (nodeId, isOn) {
        graph.forEachLinkedNode(nodeId, function (node, link) {
            var linkUI = graphics.getLinkUI(link.id);
            if (linkUI) {
                linkUI
                        .attr('stroke', isOn ? 'red' : 'gray')
                        .attr('marker-end', isOn ? 'url(#TriangleRed)' : 'url(#Triangle)');
            }
        });
    };

    graphics.node(function (node) {
        var ui = Viva.Graph.svg('g');
        var svgText = Viva.Graph.svg('text').attr('y', '-4px').text(node.id);
        var imgLink = node.data.dead ? '/img/dead_actor.png' : '/img/actor.png'
        var img = Viva.Graph.svg('image')
                .attr('width', nodeSize)
                .attr('height', nodeSize)
                .link(imgLink);

        ui.append(svgText);
        if(node.data.mailboxSize > -1){ // because js
        var mailboxSizeText = Viva.Graph.svg('text')
                    .attr('y', '-20px')
                    .text('\u2709 ' + node.data.mailboxSize);

         ui.append(mailboxSizeText);
        }
        ui.append(img);

        $(ui).hover(function () { // mouse over
            highlightRelatedNodes(node.id, true);
        }, function () { // mouse out
            highlightRelatedNodes(node.id, false);
        });
        $(ui).click(function () {
            frontendApp().toggleActor(node.id)
        });

        return ui;
    }).placeNode(function (nodeUI, pos) {
        nodeUI.attr('transform',
                'translate(' +
                (pos.x - nodeSize / 2) + ',' + (pos.y - nodeSize / 2) +
                ')');
    });

    var createMarker = function (id, color) {
        return Viva.Graph.svg('marker')
                .attr('id', id)
                .attr('viewBox', "0 0 10 10")
                .attr('refX', "10")
                .attr('refY', "5")
                .attr('stroke', 'light-gray')
                .attr('fill', color)
                .attr('markerUnits', "strokeWidth")
                .attr('markerWidth', "10")
                .attr('markerHeight', "5")
                .attr('orient', "auto");
    };

    var marker = createMarker('Triangle', 'gray');
    marker.append('path').attr('d', 'M 0 0 L 10 5 L 0 10 z');
    var markerRed = createMarker('TriangleRed', 'red');
    markerRed.append('path').attr('d', 'M 0 0 L 10 5 L 0 10 z');


    // Marker should be defined only once in <defs> child element of root <svg> element:
    var defs = graphics.getSvgRoot().append('defs');
    defs.append(marker);
    defs.append(markerRed);

    var geom = Viva.Graph.geom();

    graphics.link(function (link) {
        // Notice the Triangle marker-end attribe:
        return Viva.Graph.svg('path')
                .attr('stroke', 'gray')
                .attr('stroke-width', '4')
                .attr('marker-end', 'url(#Triangle)');
    }).placeLink(function (linkUI, fromPos, toPos) {
        // Here we should take care about
        //  "Links should start/stop at node's bounding box, not at the node center."

        // For rectangular nodes Viva.Graph.geom() provides efficient way to find
        // an intersection point between segment and rectangle
        var toNodeSize = nodeSize,
                fromNodeSize = nodeSize;

        var from = geom.intersectRect(
                        // rectangle:
                        fromPos.x - fromNodeSize / 2, // left
                        fromPos.y - fromNodeSize / 2, // top
                        fromPos.x + fromNodeSize / 2, // right
                        fromPos.y + fromNodeSize / 2, // bottom
                        // segment:
                        fromPos.x, fromPos.y, toPos.x, toPos.y)
                || fromPos; // if no intersection found - return center of the node

        var to = geom.intersectRect(
                        // rectangle:
                        toPos.x - toNodeSize / 2, // left
                        toPos.y - toNodeSize / 2, // top
                        toPos.x + toNodeSize / 2, // right
                        toPos.y + toNodeSize / 2, // bottom
                        // segment:
                        toPos.x, toPos.y, fromPos.x, fromPos.y)
                || toPos; // if no intersection found - return center of the node

        var data = 'M' + from.x + ',' + from.y +
                'L' + to.x + ',' + to.y;

        linkUI.attr("d", data);
    });

    // Finally render the graph with our customized graphics object:
    var renderer = Viva.Graph.View.renderer(graph, {
        graphics: graphics,
        layout: layout,
        container: document.getElementById("graphview")
    });
    renderer.run();

