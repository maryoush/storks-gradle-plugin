![Storks](https://stash.hybris.com/projects/CI/repos/cdm-commons/browse/storks.png?raw)

# Overview
A plugin which binds a properties sourced from different origins
* a public property file (profile-public.properties)
* a private property file (profile-private.properties)
* a system, env variables

The order of precedence is as follows.
The public properties are the least important and get overwritten by private ones.
The env (set by environment variables) or system ones (provided by -P) overwrite the public or private.

There has to be at least public properties provided others are optional.

# Input 
* Plugin expects a _profile name_ to be provided , possible values 'aws-prod' or 'aws-stage' if none provided or
unrecognized one provided a 'aws-stage' is used.

* Plugin injects a bound properties into map provided from outside - _effectiveProperties_.

# Run, test locally

./gradlew build test

# Install locally to maven repository

./gradlew publishToMavenLocal

# Integration

## CF deploy integration

Integrates via arbitrary properties('cfProperties') map

```

dependencies {
		....
		classpath group: 'com.hybris.profile', name: 'storks-gradle-plugin', version: '$version'
	}
	
	
apply plugin: 'storks'


def cfProperties = [:]

cloudfoundry {
	env = cfProperties
}

task prepareCfProperties(type: ProfilePropertiesTask) {
	effectiveProperties cfProperties
}

cfLogin {
	dependsOn 'prepareCfProperties'
}

...

cfDeploy {
	dependsOn 'prepareCfProperties'
}

cloudfoundry {
		target = 'https://....'
		domain = '....'
		....
	}


```

## Local deploy integration

Integrates via system properties variable


```
dependencies {
		....
		classpath group: 'com.hybris.profile', name: 'storks-gradle-plugin', version: '$version'
	}
	
	
apply plugin: 'storks'


task prepareSystemProperties(type: com.hybris.profile.ProfilePropertiesTask) {
	effectiveProperties System.properties
}


task acceptanceTests(type: Test) {
	include 'com/hybris/cdm/graph/**/*Spec*'
	systemProperties System.getProperties()
}

jettyEclipseRun {
	dependsOn 'prepareSystemProperties'
	contextPath = ''
}

task localAcceptanceTests(type: Test) {
	include 'com/hybris/cdm/graph/**/*Spec*'
	systemProperties System.getProperties()
}


daemonJettyEclipseRun{
	dependsOn 'prepareSystemProperties'
	dependsOn 'war'
	daemon true
}


```

## Client side configuartion (acceptance tests)

Use the 'PropertiesLoader''s 'loadProperties' method point the public, [private] properties files for client side configuration set up.


```
	static Map<String, String> props = PropertiesLoader.loadProperties("/acceptance/aws-prod-test-public.properties")

	static def defaultConfig = [
			testTenant                       : props.getOrDefault("TEST_TENANT", "storktest"),
			testClient                       : props.getOrDefault("TEST_CLIENT", "storks.storkstest"),
			secureGraphServiceURL            : props.getOrDefault("SECURE_GRAPH_URL", getSecureGraphServiceURL()),
			pubSubURL                        : props.getOrDefault("PUB_SUB_URL", "http://XXXXX.stage.internal.YYYY.io"),
			pubSubProductNodeChangedEventType: props.getOrDefault("PUBSUB_EVENT_TYPE", "enricherResolutionSegment"),
			securedGraphClientName           : props.getOrDefault("SECURED_GRAPH_CLIENT_NAME", "hybris.securedgraph"),
			dispatcherClientName             : props.getOrDefault("DISPATCHER_CLIENT_NAME", "hybris.dispatcher"),
			userName                         : props.getOrDefault("REST_CLIENT_USERNAME", "XXXX")
	]
	
```