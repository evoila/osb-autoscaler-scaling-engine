package de.evoila.cf.autoscaler.engine;

import de.evoila.cf.autoscaler.api.ApplicationNameRequest;
import de.evoila.cf.autoscaler.api.ScalingRequest;
import de.evoila.cf.autoscaler.api.binding.BindingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * @author Marius Berger
 */
public class CloudFoundryValidationService {

    private Logger log = LoggerFactory.getLogger(CloudFoundryValidationService.class);

	private List<String> supportedPlatforms;
	
	public CloudFoundryValidationService(List<String> supportedPlatforms) {
		this.supportedPlatforms = supportedPlatforms;
	}
	
	
	public ResponseEntity validateNameRequest(String resourceId, ApplicationNameRequest request) {
		ResponseEntity response;
		if ((response = validateIdAndContext(resourceId, request.getContext())) != null)
			return response;
		return null;
	}
	
	public ResponseEntity validateScalingRequest(String resourceId, ScalingRequest request) {
		ResponseEntity response;
		if (( response = validateIdAndContext(resourceId, request.getContext())) != null)
			return response;
		if (request.getScale() < 0) 
			return new ResponseEntity("{ \"error\" : \"scale is smaller than 0\" }", HttpStatus.BAD_REQUEST);
		
		return null;
	}
	
	private ResponseEntity validateIdAndContext(String resourceId, BindingContext context) {
	    String errorMessage = null;
		if (!validId(resourceId))
            errorMessage = "{ \"error\" : \"resourceId contains invalid characters\" }";
		if (!supportedPlatform(context.getPlatform()))
            errorMessage = "{ \"error\" : \"platform is not supported\" }";
		if (!validId(context.getSpace_guid()))
            errorMessage = "{ \"error\" : \"space_guid contains invalid characters\" }";
		if (!validId(context.getOrganization_guid()))
            errorMessage = "{ \"error\" : \"organization_guid contains invalid characters\" }";

		if (errorMessage != null) {
		    log.info("Error validation request with: " + errorMessage);
            return new ResponseEntity(errorMessage, HttpStatus.BAD_REQUEST);
        } else
            return null;
	}
	
	private boolean supportedPlatform(String platform) {
		return supportedPlatforms.contains(platform);
	}
	
	public static boolean validId(String id) {
		if (id == null || id.isEmpty())
			return false;
		
		if (!id.matches("\\w+")) {
			for (int i = 0; i < id.length(); i++) {
				if (String.valueOf(id.charAt(i)).matches("\\W+") && id.charAt(i) != '-' ) {
					return false;	
				}
			}
		}
		return true;
	}
}
