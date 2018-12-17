package de.evoila.cf.autoscaler.engine;

import de.evoila.cf.autoscaler.api.ScalingRequest;
import de.evoila.cf.autoscaler.api.binding.BindingContext;
import de.evoila.cf.autoscaler.engine.exceptions.OrgNotFoundException;
import de.evoila.cf.autoscaler.engine.exceptions.ResourceNotFoundException;
import de.evoila.cf.autoscaler.engine.exceptions.SpaceNotFoundException;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.client.v2.applications.SummaryApplicationRequest;
import org.cloudfoundry.client.v2.applications.SummaryApplicationResponse;
import org.cloudfoundry.client.v2.organizations.GetOrganizationRequest;
import org.cloudfoundry.client.v2.organizations.GetOrganizationResponse;
import org.cloudfoundry.client.v2.spaces.GetSpaceRequest;
import org.cloudfoundry.client.v2.spaces.GetSpaceResponse;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ScaleApplicationRequest;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.uaa.clients.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author Marius Berger
 */
public class CloudFoundryService {

	private Logger log = LoggerFactory.getLogger(CloudFoundryService.class);
	private static final long TIMEOUT_SCALING = 15000L;

	private CloudFoundryClient cfClient;

	private String apiHost;
	private String cfUsername;
	private String cfSecret;

	public CloudFoundryService(String api, String username, String secret) {
		this.apiHost = api;
		this.cfUsername = username;
		this.cfSecret = secret;

		ConnectionContext con = DefaultConnectionContext.builder()
                .apiHost(apiHost)
                .build();

        TokenProvider prov = PasswordGrantTokenProvider.builder()
                .password(cfSecret)
                .username(cfUsername)
                .build();

        cfClient = ReactorCloudFoundryClient.builder()
                .connectionContext(con)
                .tokenProvider(prov)
                .build();
	}

	public ResponseEntity<?> scaleCFApplication(String resourceId, ScalingRequest request) throws OrgNotFoundException, SpaceNotFoundException, ResourceNotFoundException {

		String orgName = getCFOrganizationName(request.getContext().getOrganization_guid());
		String spaceName = getCFSpaceName(request.getContext().getSpace_guid());

        CloudFoundryOperations ops = DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cfClient)
                .organization(orgName)
                .space(spaceName)
                .build();

        String appName = getCFApplicationName(resourceId);

        if (appName == null) {
        	log.info("Tried to scale " + resourceId + ", but could not find the resource");
        	return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{ \"error\" : \"no matching resource found\" }");
        }

        ScaleApplicationRequest req = ScaleApplicationRequest
        		.builder()
        		.instances(request.getScale())
        		.name(appName)
        		.build();
        try {
        	log.info("Scaling '" + appName + "' / '" + resourceId + "' to " + request.getScale() + " instances.");
        	ops.applications().scale(req).block(java.time.Duration.ofMillis(TIMEOUT_SCALING));
        } catch (IllegalStateException ex) {
        	log.error("Ran into timeout of " + TIMEOUT_SCALING + "ms while waiting for a scaling request");
        	return new ResponseEntity<String>("{ \"error\" : \"timeout while waiting for the response of the cloudfoundry instance\" }",HttpStatus.REQUEST_TIMEOUT);
        }
        return null;
	}

    public String getCFOrganizationName(String orgId) throws OrgNotFoundException {
		try {
			GetOrganizationResponse organizationResponse = cfClient.organizations()
					.get(GetOrganizationRequest.builder().organizationId(orgId).build())
					.block();

			return organizationResponse.getEntity().getName();
		} catch (ClientV2Exception ex) {
			if (ex.getCode() == 30003)
				throw new OrgNotFoundException(orgId);
			throw ex;
		}
	}

    public String getCFSpaceName(String spaceId) throws SpaceNotFoundException {
        try {
            log.info("Looking for CF space name with id '"+spaceId+"'.");
            GetSpaceResponse spaceResponse = cfClient.spaces()
                    .get(GetSpaceRequest.builder().spaceId(spaceId).build())
                    .block();
            return spaceResponse.getEntity().getName();
        } catch (ClientV2Exception ex) {
            if (ex.getCode() == 40004)
                throw new SpaceNotFoundException(spaceId);
            throw ex;
        }
	}

	public String getCFApplicationName(String appId) throws ClientV2Exception, ResourceNotFoundException {
	    try {
            log.info("Looking for CF app name with id '"+appId+"'.");
            SummaryApplicationResponse appSummary = cfClient.applicationsV2()
                    .summary(SummaryApplicationRequest.builder().applicationId(appId).build())
                    .block();
            return appSummary.getName();
        } catch (ClientV2Exception ex) {
            if (ex.getCode() == 100004)
                throw new ResourceNotFoundException(appId);
            throw ex;
        }
	}
}
