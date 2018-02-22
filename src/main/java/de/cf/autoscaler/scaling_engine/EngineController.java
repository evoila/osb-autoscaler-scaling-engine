package de.cf.autoscaler.scaling_engine;


import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import de.cf.autoscaler.api.ApplicationNameRequest;
import de.cf.autoscaler.api.ScalingRequest;
import de.cf.autoscaler.scaling_engine.cf_services.CFService;
import de.cf.autoscaler.scaling_engine.cf_services.CFValidationService;


@Controller
public class EngineController {
	
	private Logger log = LoggerFactory.getLogger(EngineController.class);
	
	private CFService cfService;
	private CFValidationService cfValidationService;
	
	@Value("${scaler.secret}")
	private String secret;
	
	@Value("${cf.url}")
	private String apiHost;
	
	@Value("${cf.adminname}")
	private String cf_username;
	
	@Value("${cf.adminpassword}")
	private String cf_secret;
	
	@Value("#{'${engine.platforms.supported}'.split(',')}")
	private List<String> supportedPlatforms;
	
	@PostConstruct
	private void init() {
		cfService = new CFService(apiHost, cf_username, cf_secret);
		cfValidationService = new CFValidationService(supportedPlatforms);
	}
	
	@RequestMapping (value = "/resources/{resourceId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> scale(@RequestHeader(value="secret") String secret,
			@RequestBody ScalingRequest requestBody, @PathVariable("resourceId") String resourceId) {
		
		if (secret.equals(this.secret)) {
			ResponseEntity<?> response = cfValidationService.validateScalingRequest(resourceId, requestBody);
			if (response != null) 
				return response;
			
			String appName = cfService.getAppNameFromId(resourceId, requestBody.getContext());
	        if (appName == null) {
	        	log.info("Tried to scale " + resourceId + ", but could not find the resource");
	        	return new ResponseEntity<String>("{ \"error\" : \"no matching resource found\" }",HttpStatus.NOT_FOUND);
	        }
	        
	        log.info("Scaling '" + appName + "' / '" + resourceId + "' to " + requestBody.getScale() + " instances.");
			response = cfService.scaleCFApplication(resourceId, requestBody);
			if (response != null)
				return response;
			
			
			return ResponseEntity.status(HttpStatus.OK).body("{}");
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{ \"error\" : \"wrong secret\" }");
	}
	
	@RequestMapping (value = "/namefromid/{resourceId}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getAppNameFromId(@RequestHeader(value="secret") String secret, @PathVariable("resourceId") String resourceId, 
			@RequestBody ApplicationNameRequest request) {
		
		if (secret.equals(this.secret)) {
			ResponseEntity<?> response = cfValidationService.validateNameRequest(resourceId, request);
			if (response != null)
				return response;
			
			String appName = cfService.getAppNameFromId(resourceId, request.getContext());
			if (appName == null) {
				log.info("Tried to get the name of " + resourceId + ", but could not find the resource");
	        	return new ResponseEntity<String>("{ \"error\" : \"no matching resource found\" }",HttpStatus.NOT_FOUND);
			}
			
			log.info("Returning name '" + appName + "' for the id '" + resourceId + "'.");
			request.setName(appName);
			return new ResponseEntity<ApplicationNameRequest>(request,HttpStatus.OK);
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{ \"error\" : \"wrong secret\" }");
	}
}
