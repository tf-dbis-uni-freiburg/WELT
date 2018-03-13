package welt.server.actions.disambiguation;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import welt.entitydisambiguation.backend.AbstractDisambiguationTask;
import welt.entitydisambiguation.backend.DisambiguationMainService;
import welt.entitydisambiguation.backend.DisambiguationTaskCollective;
import welt.entitydisambiguation.backend.DisambiguationTaskSingle;
import welt.entitydisambiguation.dpo.DisambiguationRequest;
import welt.entitydisambiguation.dpo.DisambiguationResponse;
import welt.entitydisambiguation.dpo.EntityDisambiguationDPO;
import welt.entitydisambiguation.dpo.Response;
import welt.entitydisambiguation.knowledgebases.KnowledgeBaseIdentifiers;
import welt.entitydisambiguation.properties.Properties;
import welt.mentiondetection.DBpediaSpotterMentionDetection;
import welt.mentiondetection.FoxMentionDetection;
import welt.mentiondetection.MentionDetection;
import welt.mentiondetection.WeltMentionDetection;
import welt.models.EntityNIF;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.nlp2rdf.NIF;
import org.nlp2rdf.bean.NIFBean;
import org.nlp2rdf.bean.NIFType;
import org.nlp2rdf.nif20.impl.NIF20;


@Controller
@RequestMapping("/")
public class DisambiguationService {

	public DisambiguationService() {
		super();
	}
	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public ModelAndView index() {
		ModelAndView model = new ModelAndView("index");
		return model;
	}
	
	@RequestMapping(value = "/nif", method = RequestMethod.GET)
	public ModelAndView nif() {
		ModelAndView model = new ModelAndView("nif");
		return model;
	}
	
	@RequestMapping(value = "/nif", method = RequestMethod.POST)
	public @ResponseBody String nifPost(@RequestBody final String request) {
		
		String contextString = null;
		String documentURI = null;
		ArrayList<EntityNIF> entities = new ArrayList<>();
		
		try {
			JSONObject jsonObject = new JSONObject(request);
			documentURI = jsonObject.getString("documentUri");
			contextString = jsonObject.getString("context");
			JSONArray entitiesArray = jsonObject.getJSONArray("entities");
			for(int i = 0; i < entitiesArray.length(); i++) {
				JSONObject entityNifJSON = entitiesArray.getJSONObject(i);
				int start = entityNifJSON.getInt("start");
				int end = entityNifJSON.getInt("end");
				String uri = entityNifJSON.getString("entityURI");
				String text = entityNifJSON.getString("entityName");
				entities.add(new EntityNIF(start, end, uri, text));
			}
		} catch (JSONException e) {
			return "Error while parsing the JSON file.";
		}
		
		if(contextString != null) {
			if(documentURI == null || documentURI.trim().length() == 0) {
				documentURI = "http://paxos.informatik.uni-freiburg.de:8080/WELT";
			}
			
			NIFBean.NIFBeanBuilder contextBuilder = new NIFBean.NIFBeanBuilder();
	
			
			contextBuilder.context(documentURI, 0, contextString.length()).mention(contextString).nifType(NIFType.CONTEXT);
	
			NIFBean beanContext = new NIFBean(contextBuilder);
			
			NIFBean.NIFBeanBuilder entityBuilder = new NIFBean.NIFBeanBuilder();
		    
		    List<NIFBean> beans = new ArrayList<>();
		    beans.add(beanContext);
		    
		    NIFBean entityBean;
	
		    for(int i = 0; i < entities.size(); i++) {
		    	EntityNIF entity = entities.get(i);
		        entityBuilder.context(documentURI, entity.getStart(), entity.getEnd()).mention(entity.getText()).beginIndex(entity.getStart()).endIndex(entity.getEnd())
		        		.taIdentRef(entity.getUri());
		        entityBean = new NIFBean(entityBuilder);
		        System.out.println(entity.getStart() + " " + entity.getEnd() + " " + entity.getText() + " " +entity.getUri());
		        beans.add(entityBean);
		    }
		    
		    NIF nif = new NIF20(beans);   // For NIF 2.0
		    
		    String syntax = "TTL"; // "TURTLE"
		    StringWriter out = new StringWriter();
		    nif.getModel().write(out, syntax);
		    
		    return out.toString(); //Turtle 
		} else {
			return "No context string given or an error occured, please check the logs.";
		}
	}
	
	@RequestMapping(value = "/spot", method = RequestMethod.POST)
	public @ResponseBody String externalCall(@RequestBody final String request) {
		String [] valuesAndKeys = request.split("&");
		String [] annotationText = valuesAndKeys[0].split("=");
		String [] spotter = valuesAndKeys[1].split("=");
		String resStr = "";
		if(annotationText[0].equals("text")) {
			String text = annotationText[1];
			MentionDetection mentionDetection;
			if(spotter[1].equals("spotlight")) {
				mentionDetection = new DBpediaSpotterMentionDetection(text);
			} else if(spotter[1].equals("fox")) {
				mentionDetection = new FoxMentionDetection(text);
			} else {
				final DisambiguationMainService mainService = DisambiguationMainService.getInstance();
				
				mentionDetection = new WeltMentionDetection(text,
						mainService.knowledgebases.get(KnowledgeBaseIdentifiers.Standard));
			}
			resStr = mentionDetection.detectMentions();
		}else {
			resStr = "Bad request.";
		}
		return resStr;
	}
	
	@RequestMapping(value = "/disambiguateWithoutCategories-single", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody DisambiguationResponse annotateSingle(@RequestBody final DisambiguationRequest request) {
		DisambiguationResponse annotationResponse = disambiguateSingle(request);
		return annotationResponse;
	}

	@RequestMapping(value = "/disambiguationWithoutCategories-collective", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody DisambiguationResponse annotateCollectiveWithoutCategories(
			@RequestBody final DisambiguationRequest request) {
		final DisambiguationResponse response = new DisambiguationResponse();
		final DisambiguationMainService mainService = DisambiguationMainService.getInstance();
		final List<EntityDisambiguationDPO> listToDis = request.getSurfaceFormsToDisambiguate();

		if (mainService != null) {
			final List<AbstractDisambiguationTask> tasks = new LinkedList<AbstractDisambiguationTask>();
			DisambiguationTaskCollective collectiveTask = new DisambiguationTaskCollective(listToDis,
					request.getMainTopic());
			collectiveTask.setKbIdentifier("default", "EntityCentric");
			collectiveTask.setReturnNr(1000);
			tasks.add(collectiveTask);
			mainService.disambiguate(tasks);

			List<Response> responses = collectiveTask.getResponse();
			response.setTasks(responses);
			response.setDocumentUri(request.getDocumentUri());
		}
		return response;
	}


	private DisambiguationResponse disambiguateSingle(DisambiguationRequest request) {
		final DisambiguationResponse response = new DisambiguationResponse();
		final List<EntityDisambiguationDPO> listToDis = request.getSurfaceFormsToDisambiguate();
		List<Response> responseList = new LinkedList<Response>();
		response.setDocumentUri(request.getDocumentUri());
		final List<AbstractDisambiguationTask> tasks = new LinkedList<AbstractDisambiguationTask>();
		final DisambiguationMainService mainService = DisambiguationMainService.getInstance();
		if (mainService != null) {
			int docsToReturn = 0;
			if (request.getDocsToReturn() == null) {
				docsToReturn = Properties.getInstance().getDisambiguationResultSize();
			} else {
				docsToReturn = request.getDocsToReturn();
			}
			for (int i = 0; i < listToDis.size(); i++) {
				EntityDisambiguationDPO dpo = listToDis.get(i);
				DisambiguationTaskSingle task = new DisambiguationTaskSingle(dpo);
				task.setReturnNr(docsToReturn);
				task.setKbIdentifier(listToDis.get(i).getKbversion(), listToDis.get(i).getSetting());
				// Bugfix! Selected text may not be null. Should be ""
				// String instead;
				if (dpo.getSelectedText() != null) {
					tasks.add(task);
				}
			}
			mainService.disambiguate(tasks);
		}

		for (AbstractDisambiguationTask task : tasks) {
			responseList.add(task.getResponse().get(0));
		}
		response.setTasks(responseList);
		return response;
	}
}