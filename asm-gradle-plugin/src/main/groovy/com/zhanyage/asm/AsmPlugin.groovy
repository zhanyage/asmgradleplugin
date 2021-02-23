package com.zhanyage.asm

import com.zhanyage.asm.transform.AsmTransformTest
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class AsmPlugin implements Plugin<Project> {
    private static final String TAG = "AsmPlugin"

    @Override
    void apply(Project project) {

        project.extensions.create("asm", AsmExtension)

        if (!project.plugins.hasPlugin('com.android.application')) {
            throw new GradleException('Asm Plugin, Android Application plugin required')
        }

        //当 project 配置完成之后会回调这个 方法，可以准备执行 task 了
        project.afterEvaluate {
            def android = project.extensions.android
            def configuration = project.asm
            android.applicationVariants.all { variant ->
                String output = configuration.output
                if (Util.isNullOrNil(output)) {
                    configuration.output = project.getBuildDir().getAbsolutePath() + File.separator + "systrace_output"
                    Log.i(TAG, "set asm output file to " + configuration.output)
                }
                Log.i(TAG, "set asm test is %s", configuration.enable)
                if (configuration.enable) {
                    AsmTransformTest.inject(project, variant)
                }
            }
        }
    }

}