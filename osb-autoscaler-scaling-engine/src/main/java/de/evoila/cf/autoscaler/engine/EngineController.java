package de.evoila.cf.autoscaler.engine;

import de.evoila.cf.autoscaler.api.ApplicationNameRequest;
import de.evoila.cf.autoscaler.api.ScalingRequest;
import de.evoila.cf.autoscaler.engine.exceptions.OrgNotFoundException;
import de.evoila.cf.autoscaler.engine.exceptions.ResourceNotFoundException;
import de.evoila.cf.autoscaler.engine.exceptions.SpaceNotFoundException;
import de.evoila.cf.autoscaler.engine.properties.CloudFoundryConfigurationBean;
import de.evoila.cf.autoscaler.engine.properties.EngineBean;
import de.evoila.cf.autoscaler.engine.properties.ScalerBean;
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
	public ResponseEntity scale(@RequestHeader("X-Auth-Token") String xAuthToken,
                                   @RequestBody ScalingRequest requestBody, @PathVariable("resourceId") String resourceId) {
		
		if (xAuthToken.equals(scalerBean.getSecret())) {
			ResponseEntity<?> response = cloudFoundryValidationService.validateScalingRequest(resourceId, requestBody);
			if (response != null) 
				return response;

			try {
				response = cloudFoundryService.scaleCFApplication(resourceId, requestBody);
				if (response != null)
					return response;
			} catch (OrgNotFoundException | SpaceNotFoundException | ResourceNotFoundException ex) {
				log.error("Could not find one of the components", ex);
			}
			return new ResponseEntity(HttpStatus.OK);
		}
        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
	}
	
	@PostMapping(value = "/namefromid/{resourceId}")
	public ResponseEntity<?> nameFromId(@RequestHeader("X-Auth-Token") String xAuthToken, @PathVariable("resourceId") String resourceId,
			@RequestBody ApplicationNameRequest request) {
		
		if (xAuthToken.equals(scalerBean.getSecret())) {
			ResponseEntity response = cloudFoundryValidationService.validateNameRequest(resourceId, request);
			if (response != null)
				return response;

			String appName = "";
			try {
				appName = cloudFoundryService.getCFApplicationName(resourceId);
			} catch (ResourceNotFoundException ex) {
				log.info("Tried to get the name of " + resourceId + ", but could not find the resource");
				return new ResponseEntity<String>("{ \"error\" : \"no matching resource found\" }",HttpStatus.NOT_FOUND);
			}

			log.info("Returning name '" + appName + "' for the id '" + resourceId + "'.");
			request.setName(appName);
            return new ResponseEntity<ApplicationNameRequest>(request, HttpStatus.OK);
		}
        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
	}
}
