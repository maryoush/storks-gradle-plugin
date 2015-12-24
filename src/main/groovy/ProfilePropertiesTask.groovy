import groovy.util.logging.Slf4j
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.TaskAction


/**
 * Loads XXX-public.properties and XXX-private.properties merges them and puts them all into given {@link #effectiveProperties}.
 * Where XXX is the env mnemonic steered by Gradle property - default value 'aws-stage' , other valid value 'aws-prod'.
 *
 * The sensitive properties values should be provided either in  env-private.properties or via System(Environment).
 * The order of evaluation is, the latter values overwrite previous ones : public -> private -> system/env properties.
 * The property needs to mentioned in
 * @return
 */
@Slf4j
public class ProfilePropertiesTask extends AbstractTask {

    def defaultEnv = "aws-stage"

    def effectiveProperties

    ProfilePropertiesTask() {
        super()
    }


    @TaskAction
    def performTask() {

        log.warn("root dir " + project.getProjectDir())

        def environment = (project.hasProperty("aws-prod") ? "aws-prod" : defaultEnv)

        if (project.hasProperty("aws-prod")) {
            log.info("Loading profile properties for environment  >> $environment")
        } else {
            log.info("Loading profile properties for (default) environment >> $defaultEnv")
        }


        def targetProps = new Properties()
        def publicProps = new Properties()
        publicProps.load(new FileInputStream("$project.projectDir/$environment-public.properties"))


        targetProps << evaluate(publicProps)

        def privateProps = new Properties()
        try {
            privateProps.load(new FileInputStream("$project.projectDir/$environment-private.properties"))
            log.debug("priv : $privateProps")

            targetProps.putAll(evaluate(privateProps.findAll { targetProps.containsKey(it.key) == false }))
            targetProps.putAll(evaluate(privateProps.findAll {
                (System.properties[it.key] ?: System.getenv(it.key) ?: null) == null
            }))
        }
        catch (ouch) {
            log.warn("Missing private properties configuration file : $environment-private.properties")
        }


        log.warn("###############################")
        log.warn("Adjusting properties :")
        log.warn("###############################")
        targetProps.each({ k, v -> log.warn("   $k -> $v") })
        effectiveProperties << targetProps
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