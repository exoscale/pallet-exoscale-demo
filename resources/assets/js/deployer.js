var app = angular.module('main', ['ui.bootstrap']);

function ctrl(scope, q, http) {

    scope.show_converge_output = false;
    scope.groups = {};
    scope.nodes = [];

    http.get('/api/topology')
	.success(function(data) {
	    scope.topology = data;
	});

    var get_nodes = function() {
	http.get('/api/nodes')
	    .success(function(data) {
		valid_nodes = _.filter(data, function(node) {
		    return (node['group-name'] != null && node['running?'] == true);
		});
		scope.nodes = valid_nodes;
		scope.nodecount = valid_nodes.length;

		var newgroups = _.chain(scope.topology)
		    .keys()
		    .map(function(k) {
			var wrap = {};
			wrap[k] = [];
			return wrap;
		    })
		    .reduce(function(memo,item) {
			return _.extend(memo, item);
		    }, {})
		    .value();

		_.each(valid_nodes, function(node) {
		    newgroups[node['group-name']].push(node);
		});

		scope.groups = newgroups;
	    });
    };

    scope.converge = function() {
	scope.show_converge_output = false;
	var payload = {
	    topology: scope.topology,
	    phases: [ "install" ]
	};

	http.put('/api/topology', payload)
	    .success(function (data) {
		var output = data;
		output.phases = _.filter(output.phases,
					 function(p) {
					     return (p.phase != 'settings');
					 });
		scope.converge_output =  output;
		scope.show_converge_output = true;
	    });
    };

    get_nodes();

    setInterval(get_nodes, 5000);
}

app.controller('DeployerCtrl', ['$scope','$q', '$http', ctrl]);
