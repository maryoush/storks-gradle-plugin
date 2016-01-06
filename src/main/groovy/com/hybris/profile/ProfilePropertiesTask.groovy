package com.hybris.profile

import groovy.util.logging.Slf4j
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.TaskAction


/**
 * Loads XXX-public.properties and XXX-private.properties merges them and puts them all into given {@link ProfilePropertiesTask#effectiveProperties}.
 * Where XXX is the env mnemonic steered by Gradle property - default value 'aws-stage' , other valid value 'aws-prod'.
 *
 * The sensitive properties values should be provided either in  env-private.properties or via System(Environment).
 * The order of evaluation is, the latter values overwrite previous ones : public -> private -> system/env properties.
 * It does also anonymised display of bound properties
 * @return
 */
@Slf4j
public class ProfilePropertiesTask extends AbstractTask {

    /**
     * Default profile name
     */
    def defaultEnv = "aws-stage"


    def publicFileSuffix = "public.properties"
    def privateFileSuffix = "private.properties"


    /**
     * A map to be consumed
     */
    def effectiveProperties



    ProfilePropertiesTask() {
        super()
    }



    def usage = {
        log.error("The effective properties needs to be provided explicitly to not null ")
        throw new IllegalStateException("The effective properties needs to be provided explicitly to not null ")
    }

    @TaskAction
    def performTask() {

        if (effectiveProperties == null) {
            usage()
        }

        log.debug("root dir " + project.getProjectDir())

        def environment = (project.hasProperty("aws-prod") ? "aws-prod" : defaultEnv)

        if (project.hasProperty("aws-prod")) {
            log.info("Loading profile properties for environment  >> $environment")
        } else {
            log.info("Loading profile properties for (default) environment >> $defaultEnv")
        }


        def propsLoader = new PropertiesLoader("$environment-$publicFileSuffix","$environment-$privateFileSuffix")
        effectiveProperties << propsLoader.load({ -> project.projectDir})

    }



}