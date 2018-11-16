package de.evoila.cf.autoscaler.engine;

import de.evoila.cf.autoscaler.api.ApplicationNameRequest;
import de.evoila.cf.autoscaler.api.ScalingRequest;
import de.evoila.cf.autoscaler.engine.model.CloudFoundryConfigurationBean;
import de.evoila.cf.autoscaler.engine.model.EngineBean;
import de.evoila.cf.autoscaler.engine.model.ScalerBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import javax.annotation.PostConstruct;

/**
 * @author Marius Berger
 */
@Controller
public class EngineController {
	
	private Logger log = LoggerFactory.getLogger(EngineController.class);
	
	private CloudFoundryService cloudFoundryService;

	private CloudFoundryValidationService cloudFoundryValidationService;

    private ScalerBean scalerBean;
	
    private CloudFoundryConfigurationBean cloudFoundryConfigurtionBean;
	
    private EngineBean engineBean;

	public EngineController(ScalerBean scalerBean, CloudFoundryConfigurationBean cloudFoundryConfigurationBean,
                            EngineBean engineBean) {
	    this.scalerBean = scalerBean;
	    this.cloudFoundryConfigurtionBean = cloudFoundryConfigurationBean;
	    this.engineBean = engineBean;
    }

	@PostConstruct
	private void init() {
		cloudFoundryService = new CloudFoundryService(cloudFoundryConfigurtionBean.getApi(), cloudFoundryConfigurtionBean.getUsername(),
                cloudFoundryConfigurtionBean.getPassword());
		cloudFoundryValidationService = new CloudFoundryValidationService(engineBean.getPlatforms().getSupported());
	}
	
	@PostMapping(value = "/resources/{resourceId}")
	public ResponseEntity scale(@RequestHeader(value="secret") String secret,
                                   @RequestBody ScalingRequest requestBody, @PathVariable("resourceId") String resourceId) {
		
		if (secret.equals(scalerBean.getSecret())) {
			ResponseEntity<?> response = cloudFoundryValidationService.validateScalingRequest(resourceId, requestBody);
			if (response != null) 
				return response;
	        
			response = cloudFoundryService.scaleCFApplication(resourceId, requestBody);
			if (response != null)
				return response;
			
			return new ResponseEntity(HttpStatus.OK);
		}
        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
	}
	
	@PostMapping(value = "/namefromid/{resourceId}")
	public ResponseEntity nameFromId(@RequestHeader(value="secret") String secret, @PathVariable("resourceId") String resourceId,
			@RequestBody ApplicationNameRequest request) {
		
		if (secret.equals(scalerBean.getSecret())) {
			ResponseEntity response = cloudFoundryValidationService.validateNameRequest(resourceId, request);
			if (response != null)
				return response;
			
			String appName = cloudFoundryService.getAppNameFromId(resourceId, request.getContext());
			if (appName == null) {
				log.info("Tried to get the name of " + resourceId + ", but could not find the resource");
	        	return new ResponseEntity<>("{ \"error\" : \"no matching resource found\" }",HttpStatus.NOT_FOUND);
			}
			
			log.info("Returning name '" + appName + "' for the id '" + resourceId + "'.");
			request.setName(appName);
            return new ResponseEntity(HttpStatus.OK);
		}
        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
	}
}
