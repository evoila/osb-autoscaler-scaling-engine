package de.evoila.cf.autoscaler.engine;

import java.util.List;

import de.evoila.cf.autoscaler.api.ApplicationNameRequest;
import de.evoila.cf.autoscaler.api.ScalingRequest;
import de.evoila.cf.autoscaler.api.binding.BindingContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author Marius Berger
 */
public class CloudFoundryValidationService {

	private List<String> supportedPlatforms;
	
	public CloudFoundryValidationService(List<String> supportedPlatforms) {
		this.supportedPlatforms = supportedPlatforms;
	}
	
	
	public ResponseEntity<?> validateNameRequest(String resourceId, ApplicationNameRequest request) {
		ResponseEntity<?> response = null;
		if ( (response = validateIdAndContext(resourceId, request.getContext())) != null)
			return response;
		return null;
	}
	
	public ResponseEntity<?> validateScalingRequest(String resourceId, ScalingRequest request) {
		ResponseEntity<?> response = null;
		if ( ( response = validateIdAndContext(resourceId, request.getContext())) != null )
			return response;
		if (request.getScale() < 0) 
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{ \"error\" : \"scale is smaller than 0\" }");
		
		return null;
	}
	
	private ResponseEntity<?> validateIdAndContext(String resourceId, BindingContext context) {
		if (!validId(resourceId))
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{ \"error\" : \"resourceId contains invalid characters\" }");
		if (!supportedPlatform(context.getPlatform()))
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{ \"error\" : \"platform is not supported\" }");
		if (!validId(context.getSpace_guid()))
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{ \"error\" : \"space_guid contains invalid characters\" }");
		if (!validId(context.getOrganization_guid()))
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{ \"error\" : \"organization_guid contains invalid characters\" }");
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
