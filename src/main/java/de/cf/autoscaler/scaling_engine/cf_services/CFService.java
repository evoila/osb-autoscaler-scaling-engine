package de.cf.autoscaler.scaling_engine.cf_services;

import java.util.Iterator;
import java.util.List;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.ScaleApplicationRequest;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import de.cf.autoscaler.api.ScalingRequest;
import de.cf.autoscaler.api.binding.BindingContext;

public class CFService {
	
	private Logger log = LoggerFactory.getLogger(CFService.class);
	private static final long TIMEOUT_SCALING = 15000L;
	
	private CloudFoundryClient cfClient;
	
	private String apiHost;
	private String cf_username;
	private String cf_secret;

	public CFService(String api, String username, String secret) {
		this.apiHost = api;
		this.cf_username = username;
		this.cf_secret = secret;
		
		
		ConnectionContext con = DefaultConnectionContext.builder()
                .apiHost(apiHost)
                .build();

        TokenProvider prov = PasswordGrantTokenProvider.builder()
                .password(cf_secret)
                .username(cf_username)
                .build();

        cfClient = ReactorCloudFoundryClient.builder()
                .connectionContext(con)
                .tokenProvider(prov)
                .build();

//        ReactorDopplerClient dopplerClient = ReactorDopplerClient.builder()
//                .connectionContext(con)
//                .tokenProvider(prov)
//                .build();
//
//        ReactorUaaClient uaaClient = ReactorUaaClient.builder()
//                .connectionContext(con)
//                .tokenProvider(prov)
//                .build();
	}
	
	public ResponseEntity<?> scaleCFApplication(String resourceId, ScalingRequest request) {
		
        CloudFoundryOperations ops = DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cfClient)
                .organization(request.getContext().getOrganization_guid())
                .space(request.getContext().getSpace_guid())
                .build();
        
        String appName = getAppNameFromId(resourceId, request.getContext());
        
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
        	log.error("Ran into timeout of " + TIMEOUT_SCALING + "ms while waiting for a scaling request");
        	return new ResponseEntity<String>("{ \"error\" : \"timeout while wating for the response of the cloudfoundry instance\" }",HttpStatus.REQUEST_TIMEOUT);
        }
        return null;
	}
	
	public String getOrganizationNameFromId(String orgId) {
		CloudFoundryOperations ops = DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cfClient)
                .build();
		return getCFOrganizationName(orgId, ops);
	}
	
	public String getSpaceNameFromId(String spaceId, String orgName, BindingContext context) {
		
		CloudFoundryOperations ops = DefaultCloudFoundryOperations.builder()
				.cloudFoundryClient(cfClient)
				.organization(orgName)
				.build();
		return getCFSpaceName(spaceId, ops);
	}
	
	public String getAppNameFromId(String resourceId, BindingContext context) {
		String orgName = getOrganizationNameFromId(context.getOrganization_guid());
		
		//If already the names of the org is already given in the context as guid, might be removed later
		if (orgName == null || orgName.isEmpty())
			orgName = context.getOrganization_guid();

		String spaceName = getSpaceNameFromId(context.getSpace_guid(), orgName, context);
		
		//If already the name of the space is already given in the context as guid, might be removed later
		if (spaceName == null || spaceName.isEmpty())
			spaceName = context.getSpace_guid();
		
		log.info("Looking for name of app with id '" + resourceId + "' for organization '" + orgName + "' in space '" + spaceName + "'.");
		CloudFoundryOperations ops = DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cfClient)
                .organization(orgName)
                .space(spaceName)
                .build();
		return getCFApplicationName(resourceId, ops);
	}
	
	private String getCFOrganizationName(String orgId, CloudFoundryOperations operations) {
		Iterator<List< OrganizationSummary>> it = operations.organizations().list().buffer().toStream().iterator();
		while (it.hasNext()) {
			List<OrganizationSummary> l = it.next();
			for (OrganizationSummary sum : l) {
				if (sum.getId().equals(orgId))
					return sum.getName();
			}
		}
		return null;
	}
	
	private String getCFSpaceName(String spaceId, CloudFoundryOperations operations) {
		Iterator<List<SpaceSummary>> it = operations.spaces().list().buffer().toStream().iterator();
		while (it.hasNext()) {
			List<SpaceSummary> l = it.next();
			for (SpaceSummary sum : l) {
				if (sum.getId().equals(spaceId))
					return sum.getName();
			}
		}
		return null;
	}
	
	private String getCFApplicationName(String appId, CloudFoundryOperations operations) {
        Iterator<List<ApplicationSummary>> it = operations.applications().list().buffer().toStream().iterator();
        while( it.hasNext()) {
        	List<ApplicationSummary> l = it.next();
        	for (ApplicationSummary sum : l) {
        		if (sum.getId().equals(appId)) 
        			return sum.getName();
        	}
        }
        return null;
	}
}
