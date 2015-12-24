import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * FIXME
 */
class ProfilePropertiesPlugin implements Plugin<Project> {

    @Override
    void apply(final Project project) {
        project.task('profileProperties', type: ProfilePropertiesTask)
    }
}
