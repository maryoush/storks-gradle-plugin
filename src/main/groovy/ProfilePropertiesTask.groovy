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

    /**
     * A map to be consumedś
     */
    def effectiveProperties

    /**
     * Public property value
     */
    class PropertyValue {

        protected def val

        PropertyValue(def value) {
            this.val = value
        }

        @Override
        String toString() {
            "$val"
        }

    }

    /**
     * Private or passed dynamic - thus said to be confidential property value
     */
    class ConfidentialPropertyValue extends PropertyValue {


        ConfidentialPropertyValue(def value) {
            super(value)
        }


        @Override
        String toString() {
            def length = "$val".length()
            if (length < 3)
                '*'.multiply(length)
            else {
                if (length == 3)
                    anonymise(val, 1, 1) //a*a
                else if (length == 4)
                    anonymise(val, 2, 1) //a**a
                else if (length == 5)
                    anonymise(val, 3, 1) //a***a
                else if (length == 6)
                    anonymise(val, 3, 2) //a***aa
                else if (length == 7)
                    anonymise(val, 3, 3) //a***aaa
                else
                    anonymise(val, length - 4, 3) //a***..aa
            }
        }

        def anonymise = {

            input, stars, end ->
                def length = input.length()
                String first = input.charAt(0)
                first.concat('*'.multiply(stars)).concat(input.substring(length - end))
        }

    }


    ProfilePropertiesTask() {
        super()
    }


    def propsLoader = {
        file, wrapper ->
            def props = new Properties()
            props.load(new FileInputStream("$project.projectDir/$file"))
            props.collectEntries { pair -> wrapper(pair) }
    }

    def publicWrapper = {
        pair ->
            [pair.key, new PropertyValue(pair.value)]
    }

    def privateWrapper = {
        pair ->
            [pair.key, new ConfidentialPropertyValue(pair.value)]
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


        def targetProps = new Properties()

        targetProps << propsLoader("$environment-public.properties", publicWrapper)
        try {
            targetProps << propsLoader("$environment-private.properties", privateWrapper)
        }
        catch (ouch) {
            log.warn("Missing private properties configuration file : $environment-private.properties")
        }

        targetProps //
                .findAll { pair -> evaluateValue(pair.key) != null } //
                .each { pair -> targetProps.put(pair.key, new ConfidentialPropertyValue(evaluateValue(pair.key))) }


        log.info("###############################")
        log.info("Adjusting properties :")
        log.info("###############################")
        targetProps.each({ k, v -> log.warn("   $k -> $v") })
        effectiveProperties << targetProps.collectEntries { entry -> [entry.key, entry.value.val] }
    }

    def evaluate(def props) {

        props
                .each {
            Object privateVal = evaluateValue(it.key)

            if (privateVal != null) {
                props.put(it.key, privateVal)
            }
        }
        props
    }

    def evaluateValue(def key) {
        def privateVal
        try {
            log.debug(" Undefined private property : `$key`")
            privateVal = Eval.xy(project.properties, key, "x[y]")
            log.debug(" evaluated $privateVal")
            if (privateVal == null) {
                log.info("Trying to find fallback for  property  key " + key)
                privateVal = System.properties[key] ?: System.getenv(key) ?: null
            }
        }
        catch (ouch) {
            log.warn("Can't find property for key " + key + " with value " + value + " details , " + ouch)
            privateVal = System.properties[key] ?: System.getenv(key) ?: null
        }
        privateVal
    }


}