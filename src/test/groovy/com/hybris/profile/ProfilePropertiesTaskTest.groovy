package com.hybris.profile

import org.gradle.api.Project
import org.gradle.api.tasks.TaskExecutionException
import spock.lang.Specification

class ProfilePropertiesTaskTest extends Specification {


    def loadFile = {
        name ->
            new File(ProfilePropertiesTaskTest.class.getResource(name).getFile())
    }

    def assureTaskFailsForNonExistingProfile() {

        Project project = org.gradle.testfixtures.ProjectBuilder.builder()
                .withProjectDir(new File("."))
                .build()


        project.task('fancyTask', type: ProfilePropertiesTask)
                .setProperty('effectiveProperties', new HashMap())
        when:
        project.tasks["fancyTask"].execute()

        then:
        TaskExecutionException t = thrown()
        t.getCause().getCause().class == FileNotFoundException.class

    }

    def assureFailsForMissingEffectiveProperties() {


        when:
        Project project = org.gradle.testfixtures.ProjectBuilder.builder()
                .withProjectDir(loadFile.call("/case2"))
                .build()


        project
                .task('fancyTask', type: ProfilePropertiesTask)

        project.tasks["fancyTask"].execute()

        then:
        TaskExecutionException t = thrown()
        t.getCause().class == IllegalStateException.class

    }


    def assureLoadsEmptyPublicProperties() {

        given:
        def props = [:]

        when:
        Project project = org.gradle.testfixtures.ProjectBuilder.builder()
                .withProjectDir(loadFile.call("/case2"))
                .build()


        project
                .task('fancyTask', type: ProfilePropertiesTask)
                .setProperty('effectiveProperties', props)

        then:
        project.tasks["fancyTask"].execute()

        props.isEmpty() == true
    }

    def assureLoadPublicProperties() {

        given:
        System.setProperty("baz", "zraz")
        def props = [:]

        when:
        Project project = org.gradle.testfixtures.ProjectBuilder.builder()
                .withProjectDir(loadFile.call("/case3"))
                .build()


        project
                .task('fancyTask', type: ProfilePropertiesTask)
                .setProperty('effectiveProperties', props)

        then:
        project.tasks["fancyTask"].execute()

        props.foo == "bar"
        props.baz == "zraz"
        props.some == "property with open text"

        cleanup:
        System.clearProperty("baz")
    }

    def assureLoadPrivateProperties() {

        given:
        def props = [:]

        when:
        Project project = org.gradle.testfixtures.ProjectBuilder.builder()
                .withProjectDir(loadFile.call("/case4"))
                .build()


        project
                .task('fancyTask', type: ProfilePropertiesTask)
                .setProperty('effectiveProperties', props)

        then:
        project.tasks["fancyTask"].execute()

        props.foo == "this will overwrite public"
        props.baz == "glaz"
        props.zlaz == "this is only private"

        //test data obfuscation
        props.one == "1"
        props.two == "22"
        props.three == "333"
        props.four == "4444"
        props.five == "55555"
        props.six == "666666"


    }

    def assurePropertiesHierarchy() {

        given:
        def props = [:]
        System.setProperty("baz", "this will go from system")
        System.setProperty("zlaz", "this will also go from system")


        when:
        Project project = org.gradle.testfixtures.ProjectBuilder.builder()
                .withProjectDir(loadFile.call("/case5"))
                .build()


        project
                .task('fancyTask', type: ProfilePropertiesTask)
                .setProperty('effectiveProperties', props)

        then:
        project.tasks["fancyTask"].execute()

        props.foo == "this will overwrite public"
        props.baz == "this will go from system"
        props.zlaz == "this will also go from system"

        cleanup:
        System.clearProperty("baz")
        System.clearProperty("zlaz")
    }


    def assureSystemPropertiesAlwaysWin() {

        given:
        def props = [:]
        System.setProperty("foo", "this will  go from system")


        when:
        Project project = org.gradle.testfixtures.ProjectBuilder.builder()
                .withProjectDir(loadFile.call("/case6"))
                .build()


        project
                .task('fancyTask', type: ProfilePropertiesTask)
                .setProperty('effectiveProperties', props)

        then:
        project.tasks["fancyTask"].execute()

        props.foo == "this will  go from system"


        cleanup:
        System.clearProperty("foo")
    }


    def assurePropsLoadedViaClassLoader() {

        given:
        System.setProperty("srub", "this will go from system")


        when:
        def propsLoader =
                new PropertiesLoader("someDir/aws-stage-tests-public.properties", "otherDir/aws-stage-tests-private.properties")

        then:
        def targetProps = propsLoader.load(loadFile("/case7").getAbsolutePath())


        targetProps.foo == "private foo"
        targetProps.pub == "not public any more"
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
                new PropertiesLoader("someDir/aws-stage-tests-public.properties", "this-does-not-exist")

        then:
        def targetProps = propsLoader.load({ -> Class.getResource("/case7").path })


        targetProps.pub == "this is public"
        targetProps.grub == "this is grub"
        targetProps.srub == "this will go from system"

        cleanup:
        System.clearProperty("srub")

    }


}