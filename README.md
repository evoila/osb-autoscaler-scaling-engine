# osb-autoscaler-scaling-engine
`osb-autoscaler-scaling-engine` is a spring boot application and is part of the osb-autoscaler-framework. When the [osb-autoscaler-core](https://github.com/evoila/osb-autoscaler-core) decided an action for a resource, the scaling-engine is called to execute it.

## API
Every call to the scaling-engine needs a `secret` header with the prearranged secret for authorization for now.
You can set the secret of the scaling-engine in the `application.properties` via the `scaler.secret` property.

| Endpoint | Body | Description |
| ------ | ------ | ------ |
| POST /resources/{resourceId} | [body](https://github.com/evoila/osb-autoscaler-api/blob/develop/src/main/java/de/cf/autoscaler/api/ScalingRequest.java) | Tries to identify and scale a resource by the given `resourceId` and the given [context](https://github.com/evoila/osb-autoscaler-api/blob/develop/src/main/java/de/cf/autoscaler/api/binding/BindingContext.java) to the given instance count `scale`. |
| POST /namefromid/{resourceId} | [body](https://github.com/evoila/osb-autoscaler-api/blob/develop/src/main/java/de/cf/autoscaler/api/ApplicationNameRequest.java)|Tries to identify a resource and return the name the platform has for the resource, if one exists. Returns the object from the request in the body with the updated name. |

Example body for a scaling request:
```json
{
"context" : {
	"platform" : "cloudfoundry",
	"space_guid" : "example_space",
	"organization_guid" : "example_org"
    },
"scale" : 42
}
```

Example body of a name request:
```json
{
    "id": "12345-ExampleId-67890",
    "name": "",
    "context": {
        "platform": "cloudfoundry",
        "space_guid": "example_space",
        "organization_guid": "example_org"
    }
}
```



## Supported platforms
- Cloudfoundry

## Dependencies
This project uses Apache Maven as build management tool. You should download the following repository and build it before building the scaling-engine. 
- [osb-autoscaler-api](https://github.com/evoila/osb-autoscaler-api/)

## Configuration

Remember to create an application.properties file with following valid properties.

| Property | Description |
| ------ | ------ |
| scaler.secret | Secret string for authorization of incoming calls |
| engine.platforms.supported | List of supported platforms seperated via ',' |
| cf.url | API Endpoint of the cloudfoundry instance |
| cf.adminname | Name of the authorized cloudfoundry user |
| cf.adminpassword | Password of the authorized cloudfoundry user |



### Version
v1.0
