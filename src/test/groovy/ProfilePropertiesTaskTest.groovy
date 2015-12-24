import org.gradle.api.Project
import spock.lang.Specification

class ProfilePropertiesTaskTest extends Specification {

    def assureTaskFailsForNonExistingProfile() {

        Project project = org.gradle.testfixtures.ProjectBuilder.builder()
                .withProjectDir(new File("case1"))
                .build()


        project.task('fancyTask', type: ProfilePropertiesTask)

        when:
        project.tasks["fancyTask"].execute()

        then:
        thrown(FileNotFoundException.class)

    }


    def assureTaskFailsForExistingProfile() {

        given:
        //System.setProperty("Paws-prod", "aws-prod")
        def props = [:]

        when:
        Project project = org.gradle.testfixtures.ProjectBuilder.builder()
                .withProjectDir(new File(ProfilePropertiesTaskTest.class.getResource("case2").getFile()))
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
                .withProjectDir(new File(ProfilePropertiesTaskTest.class.getResource("case3").getFile()))
                .build()


        project
                .task('fancyTask', type: ProfilePropertiesTask)
                .setProperty('effectiveProperties', props)

        then:
        project.tasks["fancyTask"].execute()

        props.foo == "bar"
        props.baz == "zraz"

        cleanup:
        System.clearProperty("baz")
    }

    def assureLoadPrivateProperties() {

        given:
        def props = [:]

        when:
        Project project = org.gradle.testfixtures.ProjectBuilder.builder()
                .withProjectDir(new File(ProfilePropertiesTaskTest.class.getResource("case4").getFile()))
                .build()


        project
                .task('fancyTask', type: ProfilePropertiesTask)
                .setProperty('effectiveProperties', props)

        then:
        project.tasks["fancyTask"].execute()

        props.foo == "bar"
        props.baz == "glaz"
        props.zlaz == "raz"

    }

    def assurePropertiesHierarchy() {

        given:
        def props = [:]
        System.setProperty("baz", "boo")
        System.setProperty("zlaz", "jednak-wlazl")


        when:
        Project project = org.gradle.testfixtures.ProjectBuilder.builder()
                .withProjectDir(new File(ProfilePropertiesTaskTest.class.getResource("case5").getFile()))
                .build()


        project
                .task('fancyTask', type: ProfilePropertiesTask)
                .setProperty('effectiveProperties', props)

        then:
        project.tasks["fancyTask"].execute()

        props.foo == "bar"
        props.baz == "boo"
        props.zlaz == "jednak-wlazl"

        cleanup:
        System.clearProperty("baz")
        System.clearProperty("zlaz")
    }

}