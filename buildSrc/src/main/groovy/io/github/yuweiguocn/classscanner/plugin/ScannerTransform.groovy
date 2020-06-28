package io.github.yuweiguocn.classscanner.plugin

import com.android.annotations.NonNull
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.ide.common.internal.WaitableExecutor
import io.github.yuweiguocn.classscanner.plugin.utils.L
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.security.MessageDigest

class ScannerTransform extends Transform {
    final Project project
    final WaitableExecutor waitableExecutor

    static String getStringMD5(String str) {
        return MessageDigest.getInstance("MD5").digest(str.bytes).encodeHex().toString()
    }

    ScannerTransform(@NonNull Project project) {
        this.project = project
        this.waitableExecutor = WaitableExecutor.useGlobalSharedThreadPool()
    }

    @Override
    String getName() {
        return "classScanner"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        L.DEBUG = project.scannerConfig.debug
        ScannerVisitor visitor = new ScannerVisitor(project.scannerConfig)
        if (!transformInvocation.isIncremental()) {
            transformInvocation.outputProvider.deleteAll()
        }
        waitableExecutor.execute {
            transformInvocation.inputs.each { TransformInput input ->
                input.jarInputs.each { JarInput jarInput ->
                    def jarName = jarInput.name
                    def md5 = getStringMD5(jarInput.file.getAbsolutePath())
                    File dest = transformInvocation.outputProvider.getContentLocation(jarName + md5,
                            jarInput.contentTypes, jarInput.scopes, Format.JAR)
                    FileUtils.copyFile(jarInput.file, dest)
                    visitor.visit(jarInput.file, true)
                }
                input.directoryInputs.each { DirectoryInput directoryInput ->
                    File dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name,
                            directoryInput.contentTypes, directoryInput.scopes,
                            Format.DIRECTORY)
                    FileUtils.copyDirectory(directoryInput.file, dest)
                    visitor.visit(directoryInput, true)
                }
            }

            transformInvocation.inputs.each { TransformInput input ->
                input.jarInputs.each { JarInput jarInput ->
                    visitor.visit(jarInput.file, false)
                }
                input.directoryInputs.each { DirectoryInput directoryInput ->
                    visitor.visit(directoryInput, false)
                }
            }
        }
        this.waitableExecutor.waitForAllTasks()
        visitor.writeResult(project.buildDir.absolutePath)
        visitor.log()
    }
}