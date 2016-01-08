package com.hybris.profile

import spock.lang.Specification

class ProfilePropertiesLoaderTest extends Specification {


    def loadFile = {
        name ->
            new File(ProfilePropertiesLoaderTest.class.getResource(name).getPath())
    }

    def assurePropsLoadedViaClassLoader() {

        given:
        System.setProperty("srub", "this will go from system")


        when:
        def propsLoader =
                new PropertiesLoader("${loadFile("/case7")}/someDir/aws-stage-tests-public.properties", //
                        "${loadFile("/case7")}/otherDir/aws-stage-tests-private.properties")

        then:
        def targetProps = propsLoader.load()


        targetProps.foo == "private foo"
        targetProps.pub == "not public any more"
        targetProps.grub == "this is grub"
        targetProps.srub == "this will go from system"

        cleanup:
        System.clearProperty("srub")

    }


    def assurePropsLoadedViaClassLoaderStatic() {

        given:
        System.setProperty("srub", "this will go from system")


        when:
        def targetProps = PropertiesLoader.loadProperties("/case7/someDir/aws-stage-tests-public.properties")


        then:
        targetProps.pub == "this is public"
        targetProps.grub == "this is grub"
        targetProps.srub == "this will go from system"

        cleanup:
        System.clearProperty("srub")

    }


    def assurePropsLoadedViaClassLoaderOnlyLoadPublic() {

        given:
        System.setProperty("srub", "this will go from system")


        when:
        def propsLoader =
                new PropertiesLoader("${loadFile("/case7")}/someDir/aws-stage-tests-public.properties",//
                        "${loadFile("/case7")}/this-does-not-exist")

        then:
        def targetProps = propsLoader.load()


        targetProps.pub == "this is public"
        targetProps.grub == "this is grub"
        targetProps.srub == "this will go from system"

        cleanup:
        System.clearProperty("srub")

    }
}