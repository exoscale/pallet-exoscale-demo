var app = angular.module('main', ['ui.bootstrap']);

function ctrl(scope, q, http) {

    scope.show_converge_output = false;
    scope.close_modal = function () { scope.show_converge_output = false; };
    scope.selected_service = undefined;
    scope.services = [];
    scope.service_names = [];

    http.get('/api/topologies')
	.success(function(data) {
	    scope.topologies = data;
	});

    var get_nodes = function(service) {
	http.get('/api/services/' + service + '/nodes')
	    .success(function(data) {
		valid_nodes = _.filter(data, function(node) {
		    return (node['group-name'] != null);
		});
		scope.services[service].nodes = valid_nodes;
		scope.services[service].nodecount = valid_nodes.length;
	    });
    };

    var get_services = function() {
	http.get('/api/services')
	    .success(function(data) {
		scope.services = data;
		scope.service_names = _.keys(data);

		if (!scope.selected_service) {
		    scope.selected_service = _.first(scope.service_names);
		}

		_.chain(data)
		    .keys()
		    .each(function(service) {
			get_nodes(service);
		    });
	    });
	
    };

    scope.converge = function(s, size, phase) {
	var payload = {
	    topology: scope.topologies[s],
	    phases: [ "install" ]
	};

	http.put('/api/services/' + s + '/topology', payload)
	    .success(function (data) {
		// notify of run output
		scope.converge_output =  data;
		scope.show_converge_output = true;
	    });
    };

    get_services();

    setInterval(get_services, 5000);
}

app.controller('DeployerCtrl', ['$scope','$q', '$http', ctrl]);
