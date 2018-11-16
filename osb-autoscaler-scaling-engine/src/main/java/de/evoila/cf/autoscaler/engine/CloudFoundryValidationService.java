package de.evoila.cf.autoscaler.engine;

import de.evoila.cf.autoscaler.api.ApplicationNameRequest;
import de.evoila.cf.autoscaler.api.ScalingRequest;
import de.evoila.cf.autoscaler.api.binding.BindingContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * @author Marius Berger
 */
public class CloudFoundryValidationService {

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
		if (!validId(resourceId))
            return new ResponseEntity("{ \"error\" : \"resourceId contains invalid characters\" }", HttpStatus.BAD_REQUEST);
		if (!supportedPlatform(context.getPlatform()))
            return new ResponseEntity("{ \"error\" : \"platform is not supported\" }", HttpStatus.BAD_REQUEST);
		if (!validId(context.getSpace_guid()))
            return new ResponseEntity("{ \"error\" : \"space_guid contains invalid characters\" }", HttpStatus.BAD_REQUEST);
		if (!validId(context.getOrganization_guid()))
            return new ResponseEntity("{ \"error\" : \"organization_guid contains invalid characters\" }", HttpStatus.BAD_REQUEST);
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
