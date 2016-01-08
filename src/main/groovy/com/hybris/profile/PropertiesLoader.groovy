package com.hybris.profile

import groovy.util.logging.Slf4j

/**
 * Created by i303813 on 06/01/16.
 */

@Slf4j
class PropertiesLoader {


    private final String publicFile;

    private final String privateFile;


    PropertiesLoader(def publicFile, def privateFile) {
        this.privateFile = privateFile
        this.publicFile = publicFile
    }

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


    private def propsLoader = {
        fullFilePath, wrapper ->
            def props = new Properties()
            log.debug("Loading fullFilePath : $fullFilePath")
            props.load(new FileInputStream(fullFilePath))
            props.collectEntries { pair -> wrapper(pair) }
    }

    private def publicWrapper = {
        pair ->
            [pair.key, new PropertyValue(pair.value)]
    }

    private def privateWrapper = {
        pair ->
            [pair.key, new ConfidentialPropertyValue(pair.value)]
    }


    private def load() {
        def targetProps = new HashMap<String, String>()

        targetProps << propsLoader(publicFile, publicWrapper)
        try {
            if (privateFile != null) {
                targetProps << propsLoader(privateFile, privateWrapper)
            } else {
                log.warn("No private file provided ")
            }
        }
        catch (ouch) {
            log.warn("Missing private properties configuration file : $privateFile", ouch)
        }

        log.info("###############################")
        log.info("Adjusting properties :")
        log.info("###############################")
        targetProps //
                .findAll { pair -> evaluateValue(pair.key) != null } //
                .each { pair -> targetProps.put(pair.key, new ConfidentialPropertyValue(evaluateValue(pair.key))) }


        targetProps.each { k, v -> log.info("   $k -> $v") }

        targetProps
                .collectEntries { entry -> [entry.key, entry.value.val] }
    }


    static def loadFile = {
        name ->
            def resource = PropertiesLoader.class.getResource(name)

            if (resource != null && resource.file != null) {
                resource.file
            } else {
                name
            }
    }


    public static loadProperties(def publicFile) {
        def props = new PropertiesLoader(loadFile(publicFile), null)
        return props.load()
    }

    public static loadProperties(def publicFile, def privateFile) {
        def props = new PropertiesLoader(loadFile(publicFile), loadFile(privateFile))
        return props.load()
    }


    private def evaluateValue(def key) {
        def privateVal
        try {
            log.debug(" Undefined private property : `$key`")
            privateVal = Eval.xy(System.properties, key, "x[y]")
            log.debug(" evaluated $privateVal")
            if (privateVal == null) {
                log.debug("Trying to find fallback for property key " + key)
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
