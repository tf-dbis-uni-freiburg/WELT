<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>Text Annotator</title>
	<!-- Latest compiled and minified CSS -->
	<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">
	
	<!-- Optional theme -->
	<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css" integrity="sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp" crossorigin="anonymous">
	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
	<!-- Latest compiled and minified JavaScript -->
	<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>
	<style>
		.notInWikipedia{
			color:#f44336;
		}
		.notInWikipedia:active{
			color:#c62828;
		}
		.notInWikipedia:hover{
			color:#c62828;
		}
	</style>
</head>
<body>
	<!-- Navigation -->
    <nav class="navbar navbar-inverse">
  <div class="container-fluid">
    <!-- Brand and toggle get grouped for better mobile display -->
    <div class="navbar-header">
      <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1" aria-expanded="false">
        <span class="sr-only">Toggle navigation</span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </button>
      <a class="navbar-brand" href="#">DJ</a>
    </div>

    <!-- Collect the nav links, forms, and other content for toggling -->
    <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
      <ul class="nav navbar-nav">
        <li class="active"><a href="#">WELT<span class="sr-only">(current)</span></a></li>
      </ul>
    </div><!-- /.navbar-collapse -->
  </div><!-- /.container-fluid -->
</nav>

	<div class="container">
		<div class="row">
			<div class="col-md-6">
				<h1>Text Annotator</h1>
			</div>
			<div class="col-md-6">
				<label for="mention_detection">Mention detection:</label>
				<select name="mention_detection" id="mention_detection" class="form-control">
					<option value="own">WELT</option>
					<option value="spotlight">DBpedia Spotlight</option>
					<option value="fox">FOX</option>
				</select>
			</div>
		</div>
		<div class="row">
			<div class="col-md-6">
				<h2>Unannotated</h2>
			</div>
			<div class="col-md-6">
				<h2>Annotated</h2>
			</div>
		</div>
		<div class="row">
			<div class="col-md-6">
				<textarea id="unannotated" name="unannotated" class="form-control" cols="40" rows="5"></textarea>
			</div>
			<div class="col-md-6">
				<div id="annotated"></textarea>
			</div>
		</div>
		<div class="row">
			<div class="col-md-10">
			</div>
			<div class="col-md-2">
				<button id="start_annotation" class="btn btn-primary btn-block" style="margin:5px;">Annotate text</button>
			</div>
		</div>
		<div class="row">
			<div class="col-md-6" id="spotter-content" style="display:none">
				<h3>Spotter surface forms:</h3>
				<table id="spotter-response" class="table table-bordered table-striped"></table>
			</div>
			<div class="col-md-6" id="disambiguation-content" style="display:none">
				<h3>Disambiguated entities:</h3>
				<table id="disambiguation-response" class="table table-bordered table-striped"></table>
			</div>
		</div>
	</div>
	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
	<!-- Latest compiled and minified JavaScript -->
	<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>

	<script type="text/javascript">
		$(document).ready(function($) {
			$("#start_annotation").click(function(event) {
				$("#spotter-response").html("");
				$("#spotter-content").hide();
				$("#disambiguation-response").html("");
				$("#disambiguation-content").hide();
				$("#annotated").html("");
				var text = $("#unannotated").val();
				annotate(text);
			});	
		});

		function annotate(text){
			sendSpotterRequest(text);
		}

		function sendSpotterRequest(text){
			var spotter = $("#mention_detection").val();
			var url = "http://paxos.informatik.uni-freiburg.de:8080/WELT/spot";
			var data = {"text" : text , "spotter" : spotter };
			$.post(url, data, function(result){
				console.log(result)
				
				//parse XML
				var xmlDoc = $.parseXML(result);
				$xml = $(xmlDoc);
				$annotation = $xml.find("annotation")
				
				//array of surface forms
				var surface_forms_array = [];
				
				//build html display and present
				var output = "<tr><th>Surface form</th><th>Offset</th></tr>";
				$annotation.find("surfaceForm").each(function(){
			        $sf = $(this);
			        var sfName = $sf.attr('name');
			        var sfOffset = $sf.attr('offset');
			        output += "<tr><td>" + sfName + "</td><td>" + sfOffset + "</td></tr>";

			        surface_forms_array.push({
		        		selectedText : sfName,
		        		context : data.text,
		        		startPosition : parseInt(sfOffset)
		        	});
			    })
				$("#spotter-response").html(output);
				$("#spotter-content").show();
				
				var json_ready_array = [];
				$.each(surface_forms_array, function( index, value ) {
					json_ready_array.push({
		        		selectedText : value.selectedText.toLowerCase(),
		        		context : value.context,
		        		startPosition : value.startPosition
		        	});
				});
				
				//build json object
				var object = {
						documentUri:"",
						surfaceFormsToDisambiguate : json_ready_array
				}
				sendDisambiguationRequest(object, text, surface_forms_array);
		    });
		}
		
		function sendDisambiguationRequest(data, text, surface_forms_array){
			var url = "http://paxos.informatik.uni-freiburg.de:8080/WELT/disambiguationWithoutCategories-collective";

			$.ajax({
			    url: url,
			    type: 'POST',
			    data: JSON.stringify(data),
			    headers: {
			    	"Accept": 'application/json',   
			    	"Content-Type": 'application/json'
		    	},
			    success: function (result) {
			        console.log(result); 

					var output = "<tr><th>Surface form</th><th>Entity</th><th>Only in Wikidata</th></tr>";
			        
					var disambiguated_array = result.tasks;
					var lastOffset = 0;
					var sfIndex = 0;
					var notInWikipediaClass = "class='notInWikipedia'"
					$.each(disambiguated_array, function( index, value ) {
				        var sfEntity = "Not found.";
						if(value != null){
							surface_forms_array[sfIndex].entity = value.disEntities["0"].entityUri;
							surface_forms_array[sfIndex].notInWikipedia = value.disEntities["0"].notInWikipedia;
							var sf = surface_forms_array[sfIndex];
							//update offset for the link building (a)
							sf.startPosition += lastOffset;
							
							//for display table generation
							sfEntity = "<a href='" + sf.entity + "'>" + sf.entity + "</a>";
							
							//build output text
							var link1 = "";
							if(surface_forms_array[sfIndex].notInWikipedia == 1){
								link1 = "<a " + notInWikipediaClass + " href='" + sf.entity + "'>";
							}else{
								link1 = "<a href='" + sf.entity + "'>";
							}
							var position = sf.startPosition;
							text = [text.slice(0, position), link1, text.slice(position)].join('');
							var link2 = "</a>";
							var position = sf.startPosition + sf.selectedText.length + link1.length;
							text = [text.slice(0, position), link2, text.slice(position)].join('');
							lastOffset += link1.length + link2.length;
						}
						//build display table
						var sfName = surface_forms_array[sfIndex].selectedText;
						var notInWikipedia = "True";
						if(surface_forms_array[sfIndex].notInWikipedia == 0){
							notInWikipedia = "False";
						}
				        output += "<tr><td>" + sfName + "</td><td>" + sfEntity + "</td><td>" + notInWikipedia + "</td></tr>";
					
						sfIndex++;
					});
					$("#disambiguation-response").html(output);
					$("#disambiguation-content").show();
					$("#annotated").html(text);
			    },
			    failure: function(errMsg) {
		            console.log(errMsg);
		            return 0;
		        }
			});
		}
	</script>
	
</body>
</html>