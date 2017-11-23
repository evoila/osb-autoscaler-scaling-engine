package de.cf.autoscaler.scaling_engine;


import java.util.Iterator;
import java.util.List;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.ScaleApplicationRequest;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
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

import de.cf.autoscaler.api.ScalingRequest;


@Controller
public class EngineController {
	
	private Logger log = LoggerFactory.getLogger(EngineController.class);
	
	private static final long TIMEOUT_SCALING = 15000L;

	@Value("${scaler.secret}")
	private String secret;
	
	@Value("${cf.url}")
	private String apiHost;
	
	@Value("${cf.adminname}")
	private String cf_username;
	
	@Value("${cf.adminpassword}")
	private String cf_secret;
	
	@Value("${cf.organization}")
	private String cf_organization;
	
	@Value("${cf.space}")
	private String cf_space;
	
	@Value("#{'${engine.platforms.supported}'.split(',')}")
	private List<String> supportedPlatforms;
	
	@RequestMapping (value = "/resources/{resourceId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> scale(@RequestHeader(value="secret") String secret,
			@RequestBody ScalingRequest requestBody, @PathVariable("resourceId") String resourceId) {
		
		if (secret.equals(this.secret)) {
			ResponseEntity<?> response = validateRequest(resourceId, requestBody);
			if (response != null) 
				return response;

			response = scaleCFApplication(resourceId, requestBody);
			if (response != null) {
				return response;
				
			} else {
				log.info("Tried to scale " + resourceId + ", but could not find the resource");
				
			}
			log.info("Scaling " + resourceId + " to " + requestBody.getScale());
			return ResponseEntity.status(HttpStatus.OK).body("{}");
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{ \"error\" : \"wrong secret\" }");
	}

	private ResponseEntity<?> validateRequest(String resourceId, ScalingRequest request) {
		if (!validId(resourceId))
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{ \"error\" : \"resourceId contains invalid characters\" }");
		if (!supportedPlatform(request.getContext().getPlatform()))
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{ \"error\" : \"platform is not supported\" }");
		if (!validId(request.getContext().getSpace_guid()))
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{ \"error\" : \"space_guid contains invalid characters\" }");
		if (!validId(request.getContext().getOrganization_guid()))
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{ \"error\" : \"organization_guid contains invalid characters\" }");
		if (request.getScale() < 0) 
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{ \"error\" : \"scale is smaller than 0\" }");
		
		return null;
	}
	
	private boolean validId(String id) {
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
	
	private boolean supportedPlatform(String platform) {
		return supportedPlatforms.contains(platform);
	}
	
	private ResponseEntity<?> scaleCFApplication(String resourceId, ScalingRequest request) {
		
        ConnectionContext con = DefaultConnectionContext.builder()
                .apiHost(apiHost)
                .build();

        TokenProvider prov = PasswordGrantTokenProvider.builder()
                .password(cf_secret)
                .username(cf_username)
                .build();

        CloudFoundryClient cfClient = ReactorCloudFoundryClient.builder()
                .connectionContext(con)
                .tokenProvider(prov)
                .build();

        ReactorDopplerClient dopplerClient = ReactorDopplerClient.builder()
                .connectionContext(con)
                .tokenProvider(prov)
                .build();

        ReactorUaaClient uaaClient = ReactorUaaClient.builder()
                .connectionContext(con)
                .tokenProvider(prov)
                .build();

        CloudFoundryOperations ops = DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cfClient)
                .dopplerClient(dopplerClient)
                .uaaClient(uaaClient)
                .organization(cf_organization)
                .space(cf_space)
                .build();
        
        String appName = getCFApplicationName(resourceId, ops);
        
        if (appName == null) {
        	return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{ \"error\" : \"no matching resource found\" }");
        }
        
        ScaleApplicationRequest req = ScaleApplicationRequest
        		.builder()
        		.instances(request.getScale())
        		.name(appName)
        		.build();
        try {
        	ops.applications().scale(req).block(java.time.Duration.ofMillis(TIMEOUT_SCALING));
        } catch (IllegalStateException ex) {
        	return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("{ \"error\" : \"timeout while wating for the response of the cloudfoundry instance\" }");
        }
        return null;
	}
	
	private String getCFApplicationName(String appId, CloudFoundryOperations operations) {
        Iterator<List<ApplicationSummary>> it = operations.applications().list().buffer().toStream().iterator();
        while(it.hasNext()) {
        	List<ApplicationSummary> l = it.next();
        	for (ApplicationSummary sum : l) {
        		if (sum.getId().equals(appId)) 
        			return sum.getName();
        	}
        }
        return null;
	}
}
