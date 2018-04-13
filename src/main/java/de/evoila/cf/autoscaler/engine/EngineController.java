package de.evoila.cf.autoscaler.engine;

import javax.annotation.PostConstruct;

import de.evoila.cf.autoscaler.api.ApplicationNameRequest;
import de.evoila.cf.autoscaler.api.ScalingRequest;
import de.evoila.cf.autoscaler.engine.model.CfBean;
import de.evoila.cf.autoscaler.engine.model.EngineBean;
import de.evoila.cf.autoscaler.engine.model.ScalerBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@Controller
public class EngineController {
	
	private Logger log = LoggerFactory.getLogger(EngineController.class);
	
	private CfService cfService;
	private CfValidationService cfValidationService;

	@Autowired
    private ScalerBean scalerBean;
	
    @Autowired
    private CfBean cfBean;
	
	@Autowired
    private EngineBean engineBean;
	
	@PostConstruct
	private void init() {
		cfService = new CfService(cfBean.getApi(), cfBean.getUsername(), cfBean.getPassword());
		cfValidationService = new CfValidationService(engineBean.getEnginePlatform().getSupported());
	}
	
	@RequestMapping (value = "/resources/{resourceId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> scale(@RequestHeader(value="secret") String secret,
                                   @RequestBody ScalingRequest requestBody, @PathVariable("resourceId") String resourceId) {
		
		if (secret.equals(scalerBean.getSecret())) {
			ResponseEntity<?> response = cfValidationService.validateScalingRequest(resourceId, requestBody);
			if (response != null) 
				return response;
	        
			response = cfService.scaleCFApplication(resourceId, requestBody);
			if (response != null)
				return response;
			
			
			return ResponseEntity.status(HttpStatus.OK).body("{}");
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{ \"error\" : \"wrong secret\" }");
	}
	
	@RequestMapping (value = "/namefromid/{resourceId}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getAppNameFromId(@RequestHeader(value="secret") String secret, @PathVariable("resourceId") String resourceId, 
			@RequestBody ApplicationNameRequest request) {
		
		if (secret.equals(scalerBean.getSecret())) {
			ResponseEntity<?> response = cfValidationService.validateNameRequest(resourceId, request);
			if (response != null)
				return response;
			
			String appName = cfService.getAppNameFromId(resourceId, request.getContext());
			if (appName == null) {
				log.info("Tried to get the name of " + resourceId + ", but could not find the resource");
	        	return new ResponseEntity<>("{ \"error\" : \"no matching resource found\" }",HttpStatus.NOT_FOUND);
			}
			
			log.info("Returning name '" + appName + "' for the id '" + resourceId + "'.");
			request.setName(appName);
			return new ResponseEntity<>(request,HttpStatus.OK);
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{ \"error\" : \"wrong secret\" }");
	}
}
