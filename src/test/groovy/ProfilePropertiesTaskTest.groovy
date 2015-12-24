import org.gradle.api.Project
import org.gradle.api.tasks.TaskExecutionException
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
        TaskExecutionException t = thrown()
        t.getCause().getCause().class == FileNotFoundException.class

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

        props.foo == "this will overwrite public"
        props.baz == "glaz"
        props.zlaz == "this is only private"

    }

    def assurePropertiesHierarchy() {

        given:
        def props = [:]
        System.setProperty("baz", "this will go from system")
        System.setProperty("zlaz", "this will also go from system")


        when:
        Project project = org.gradle.testfixtures.ProjectBuilder.builder()
                .withProjectDir(new File(ProfilePropertiesTaskTest.class.getResource("case5").getFile()))
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

}