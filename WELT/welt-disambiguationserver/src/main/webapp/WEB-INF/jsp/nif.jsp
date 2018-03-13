<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>Mentions to NIF Datasets</title>
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
			<div class="col-md-3">
				<label for="title">Title</label>
				<input id="title" class="form-control" type="text" />
			</div>
			<div class="col-md-3">
				<label for="output">Output:</label>
				<select name="output" id="output" class="form-control">
					<option value="own">NIF</option>
				</select>
			</div>
		</div>
		<div class="row">
			<div class="col-md-6">
				<h2>Mentions</h2>
			</div>
			<div class="col-md-6">
				<h2>Annotated</h2>
			</div>
		</div>
		<div class="row">
			<div class="col-md-6">
				<textarea id="mentions" name="mentions" class="form-control" cols="40" rows="5"></textarea>
			</div>
			<div class="col-md-6">
				<div id="annotated"></textarea>
			</div>
		</div>
		<div class="row">
			<div class="col-md-6">
			</div>
			<div class="col-md-2">
				<button id="copy_nif" class="btn btn-primary btn-block" style="margin:5px; display:none;">Copy NIF</button>
			</div>
			<div class="col-md-2">
				<button id="modify_nif" class="btn btn-primary btn-block" style="margin:5px; display:none;">Modify NIF</button>
			</div>
			<div class="col-md-2">
				<button id="json_to_nif" class="btn btn-primary btn-block" style="margin:5px;">JSON to NIF</button>
			</div>
		</div>
		<div class="row">
			<div class="col-md-6" id="response-content" style="display:none">
				<h3>Surface forms:</h3>
				<table id="response" class="table table-bordered table-striped"></table>
			</div>
			<div class="col-md-6" id="nif-content" style="display:none">
				<h3>NIF:</h3>
				<xmp id="nif"></xmp>
			</div>
		</div>
	</div>
	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
	<!-- Latest compiled and minified JavaScript -->
	<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>

	<script type="text/javascript">
		var surfaceFormsArray;
		var surfaceFormsArrayForNif;
		var context;
	
		$(document).ready(function($) {
			$("#json_to_nif").click(function(event) {
				$("#response").html("");
				$("#response-content").hide();
				$("#nif").html("");
				$("#nif-content").hide();
				$("#modify_nif").hide();
				$("#copy_nif").hide();
				$("#annotated").html("");
				var text = $("#mentions").val();
				if(text != null && text != ""){
					var data = $.parseJSON(text);
					annotate(data);
				}
			});
			$("#modify_nif").click(function(){
				
				$.each(surfaceFormsArrayForNif, function( index, value ) {
					var entityValue = $("#"+index).val();
					console.log(entityValue);
					surfaceFormsArrayForNif[index].entity = entityValue;
				});
				
				$.each(surfaceFormsArray, function( index, value ) {
					var entityValue = $("#"+index).val();
					console.log(entityValue);
					surfaceFormsArray[index].entity = entityValue;
				});
				
				var json = generateJSONObject();
				sendNIFRequest(json);
			});
			$("#copy_nif").click(function(){
				copyToClipboard('#nif')
			});
		});

		function annotate(data){
			surfaceFormsArray = data.surfaceFormsToDisambiguate;
			surfaceFormsArrayForNif = $.extend(true, [], surfaceFormsArray);
			
			var json_ready_array = [];
			$.each(surfaceFormsArray, function( index, value ) {
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
			sendDisambiguationRequest(object, data.surfaceFormsToDisambiguate[0].context);
		}
		
		function sendDisambiguationRequest(data, text){
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
					var notInWikipediaClass = "class='notInWikipedia'";
					//take the context before it is modified with links
					context = text;
					
					$.each(disambiguated_array, function( index, value ) {
				        var sfEntity = "Not found.";
						var sfUriValue = "http://paxos.informatik.uni-freiburg.de:8080/WELT/null";
						
						if(value != null){
							surfaceFormsArray[sfIndex].entity = value.disEntities["0"].entityUri;
							surfaceFormsArray[sfIndex].notInWikipedia = value.disEntities["0"].notInWikipedia;
							surfaceFormsArrayForNif[sfIndex].entity = value.disEntities["0"].entityUri;
							surfaceFormsArrayForNif[sfIndex].notInWikipedia = value.disEntities["0"].notInWikipedia;
							var sf = surfaceFormsArray[sfIndex];
							if(sf.entity){
								sfUriValue = sf.entity;
							}
							//update offset for the link building (a)
							sf.startPosition += lastOffset;
							
							//for display table generation
							sfEntity = "<a href='" + sf.entity + "' target='_blank'>link</a>";
							
							//build output text
							var link1 = "";
							if(surfaceFormsArray[sfIndex].notInWikipedia == 1){
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
						var sfName = surfaceFormsArray[sfIndex].selectedText;
						var notInWikipedia = "True";
						if(surfaceFormsArray[sfIndex].notInWikipedia == 0){
							notInWikipedia = "False";
						}
						
						output += "<tr><td>" + sfName + "</td><td>" + sfEntity + " <input id='"+
				        	sfIndex+"' type='text' value='" + sfUriValue + "' /></td><td>" + 
				        	notInWikipedia + "</td></tr>";
					
						sfIndex++;
					});
					$("#response").html(output);
					$("#response-content").show();
					$("#annotated").html(text);
					
					var json = generateJSONObject();
					sendNIFRequest(json);
			    },
			    failure: function(errMsg) {
		            console.log(errMsg);
		            return 0;
		        }
			});
		}
			
		function sendNIFRequest(data){
			var url = "http://paxos.informatik.uni-freiburg.de:8080/WELT/nif";
			console.log(JSON.stringify(data));
		
			$.ajax({
			    url: url,
			    type: 'POST',
			    data: JSON.stringify(data),
			    headers: {
			    	"Accept": 'text/plain',   
			    	"Content-Type": 'application/json'
		    	},
			    success: function (result) {
			    	console.log(result);
			    	
					$("#nif").html(result);
					$("#nif-content").show();
					$("#modify_nif").show();
					$("#copy_nif").show();
		    	}
			});
		}
		
		function generateJSONObject(){
			var title = $("#title").val();
			var jsonData = new Object();
			jsonData.documentUri = "http://paxos.informatik.uni-freiburg.de:8080/WELT/"+title;
			jsonData.context = context;
			jsonData.entities = [];
			$.each(surfaceFormsArrayForNif, function( index, value ) {
				var object = new Object();
				object.entityName = value.selectedText;
				if(value.entity){
					object.entityURI = value.entity;
				}else{
					object.entityURI = "http://paxos.informatik.uni-freiburg.de:8080/WELT/null"
				}
				object.start = value.startPosition;
				object.end = value.startPosition + value.selectedText.length;
				jsonData.entities.push(object);
			});
			return jsonData;
		}
		
		function copyToClipboard(element) {
			  var $temp = $("<input>");
			  $("body").append($temp);
			  $temp.val($(element).text()).select();
			  document.execCommand("copy");
			  $temp.remove();
			}
	</script>
	
</body>
</html>