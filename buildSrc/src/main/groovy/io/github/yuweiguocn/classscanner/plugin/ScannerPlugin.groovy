package io.github.yuweiguocn.classscanner.plugin


import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.Plugin
import org.gradle.api.Project

class ScannerPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create('scannerConfig', ScannerConfig)
        if (project.plugins.hasPlugin(AppPlugin)) {
            def androidEnv = project.extensions.findByType(AppExtension)
            androidEnv.registerTransform(new ScannerTransform(project))
        } else if (project.plugins.hasPlugin(LibraryPlugin)) {
            def androidEnv = project.extensions.findByType(AppExtension)
            androidEnv.registerTransform(new ScannerTransform(project))
        }else{
            throw new RuntimeException("Class Scanner can't support!");
        }
        //extends from AppExtension
        def android = project.extensions.android
        project.afterEvaluate {
            android.applicationVariants.all { variant ->
                ApplicationVariant applicationVariant = variant
                ScannerConfig.appId = applicationVariant.applicationId
            }
        }

    }
}