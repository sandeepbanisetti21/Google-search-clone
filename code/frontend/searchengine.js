var app = angular.module('solrApp', []);
app.controller('solrCtrl', ['$scope', '$http', '$sce', '$timeout', function($scope, $http, $sce, $timeout) {
	$scope.send = function()
	{
		console.log($scope.selectedAlgo);
		var url = '';
		if ($scope.selectedAlgo == 'lucene')
		{
			url = "http://localhost:8080/search-engine/api/search?q=" + $scope.query + '&sort=lucene' + '&spell=on';
		}
		else if ($scope.selectedAlgo == 'pageRank')
		{
			url = "http://localhost:8080/search-engine/api/search?q=" + $scope.query + '&sort=pageRank' + '&spell=on';
		}
		$scope.getRequest(url);
	}
	
	$scope.isEmpty = false;
	$scope.getRequest = function(url)
	{
		$http({
			method: "get",
			url: url,
		}).then(function successCallback(data) {
			$scope.solrData = data.data.data;
			if ($scope.solrData.length == 0)
			{
				$scope.isEmpty = true;
			}
			else
			{
				$scope.isEmpty = false;
			}
			$scope.resCount = data.data.resultsCount;
			$scope.count = 10;
			if ($scope.resCount < 10)
			{
				$scope.count = $scope.resCount;
			}
			$scope.isCorrected = data.data.isCorrected;
			$scope.correctedWord = data.data.correctedWord;
			$scope.isFocussed = false;
			for (var i = 0; i < $scope.solrData.length; i++)
			{
				var desc = "";
				for (var j = 0; j < $scope.query.split(" ").length; j++)
				{
					var temp = $scope.query.split(" ")[j];
					var boldData = $sce.trustAsHtml('<b>'+temp+'</b>');
					
					$scope.solrData[i].description = $scope.solrData[i].description.replace(new RegExp(temp,"gi"), '<b>'+temp+'</b>');
				}
				$scope.solrData[i].description = $sce.trustAsHtml($scope.solrData[i].description);
			}
		}, function errorCallback(response) {
			alert("Message Failure: " + response);
		});
	}
	
	$scope.fetchResults = function()
	{
		var url = '';
		if ($scope.selectedAlgo == 'lucene')
		{
			url = "http://localhost:8080/search-engine/api/search?q=" + $scope.correctedWord + '&sort=lucene' + '&spell=off';
		}
		else if ($scope.selectedAlgo == 'pageRank')
		{
			url = "http://localhost:8080/search-engine/api/search?q=" + $scope.correctedWord + '&sort=pageRank' + '&spell=off';
		}
		$scope.getRequest(url);
	}
	$scope.getAutoComplete = function()
	{
		$scope.isFocussed = true;
		if ($scope.query == undefined)
		{
			$scope.suggestions=[];
		}
		else
		{
			var queryTerms = $scope.query.toLowerCase().split(" ");
			var url = "http://localhost:8080/search-engine/api/suggest?q=" + queryTerms[queryTerms.length-1];
			$scope.suggestedPrefix = queryTerms.slice(0, queryTerms.length-1).join(" ");
			$scope.suggestedPrefix += ' ';
			$http({
				method: "get",
				url: url,
			}).then(function successCallback(data) {
				$scope.suggestions = data.data.data;
				for (var i = 0; i < $scope.suggestions.length ; i++)
				{
					$scope.suggestions[i].term = $scope.suggestedPrefix + $scope.suggestions[i].term;
				}
				
			}, function errorCallback(response) { 
				alert("Message Failure: " + response);
			});
		}
	}
	
	$scope.prevent = function($event){
		console.log("prevent called")
		$scope.isFocussed = false;
		$event.stopPropagation();
	}
	
	
	$scope.assignValue = function(obj){
		$scope.isFocussed = true;
		$scope.query = obj.term.trim();
		$scope.send();
		$scope.isFocussed = false;
		
	}
}]);
