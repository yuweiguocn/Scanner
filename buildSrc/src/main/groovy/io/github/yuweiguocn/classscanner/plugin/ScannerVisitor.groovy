package io.github.yuweiguocn.classscanner.plugin

import com.android.SdkConstants
import com.android.build.api.transform.DirectoryInput
import com.google.gson.Gson
import io.github.yuweiguocn.classscanner.plugin.utils.GsonUtils
import io.github.yuweiguocn.classscanner.plugin.utils.FileUtils
import io.github.yuweiguocn.classscanner.plugin.utils.L
import org.apache.http.util.TextUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

class ScannerVisitor {

    /**
     * 存放第一次扫描class结果，只要包含反射类相关就将类名和方法名保存到列表中
     */
    Map<String, String> reflectMap = new HashMap<>();
    Map<String, String> getIdMap = new HashMap<>();

    /**
     * 存放应用R资源包含的类型
     */
    Set<String> rTypes = new HashSet<>();
    ScannerResult scannerResult = new ScannerResult();
    String packageName
    String ignorePackageName
    boolean needGetIdentifierResult
    boolean needReflectResult

    ScannerVisitor(ScannerConfig config) {
        this.needGetIdentifierResult = config.needGetIdentifierResult
        this.needReflectResult = config.needReflectResult
        this.packageName = config.packageName.replace(".","/")
        this.ignorePackageName = config.ignorePackageName.replace(".","/")
    }
    /**
     * 打印调试日志
     */
    void log() {
        Gson gson = GsonUtils.getGson();
        L.d("getIdMap=" + gson.toJson(getIdMap));
        L.d("rtypes=" + gson.toJson(rTypes));
        L.d("result=" + gson.toJson(scannerResult));
    }


    void visit(DirectoryInput directoryInput, boolean isFirst) {
        Files.walk(directoryInput.file.toPath(), Integer.MAX_VALUE).filter {
            Files.isRegularFile(it)
        }.each { Path path ->
            File file = path.toFile()
            if (file.name.endsWith(".jar")) {
                visit(file, isFirst)
            } else {
                visit(file.newInputStream(), isFirst)
            }
        }
    }


    void visit(File file, boolean isFirst) {
        Map<String, String> zipProperties = ['create': 'false']
        URI zipDisk = URI.create("jar:${file.toURI().toString()}")
        FileSystem zipFs = null
        try {
            zipFs = FileSystems.newFileSystem(zipDisk, zipProperties)
            Path root = zipFs.rootDirectories.iterator().next()
            Files.walk(root, Integer.MAX_VALUE).filter {
                Files.isRegularFile(it)
            }.each { Path path ->
                String pathString = path.toString().substring(1).replace("\\", "/")
                if (!pathString.endsWith(SdkConstants.DOT_CLASS)) {
                    return
                }
                visit(path.newInputStream(), isFirst)
            }
        } catch (e) {
            e.printStackTrace()
        } finally {
            FileUtils.closeQuietly(zipFs)
        }
    }

    void visit(InputStream inputStream, boolean isFirst) {
        ClassReader cr = new ClassReader(inputStream)
        ClassNode cn = new ClassNode(Opcodes.ASM5);
        cr.accept(cn, 0);

        if (!TextUtils.isEmpty(packageName)){
            String[] packs = packageName.split(",")
            for (int i = 0; i < packs.length; i++) {
                if(!cn.name.startsWith(packs[i])){
                    return
                }
            }
        } else if (!TextUtils.isEmpty(ignorePackageName)){
            String[] packs = ignorePackageName.split(",")
            for (int i = 0; i < packs.length; i++) {
                if(cn.name.startsWith(packs[i])){
                    return
                }
            }
        }

        if (isFirst && isAppR(cn.name)) {
            String type = cn.name.substring(appRLength())
            rTypes.add(type)
            return
        }

        L.d("class=" + cn.name)
        String className = cn.name
        for (MethodNode mn : cn.methods) {
            L.d("method=" + mn.name)
            String methodName = mn.name
            int line = 0
            Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());
            analyzer.analyze(cn.name, mn);
            for (int i = 0; i < mn.instructions.size(); i++) {
                AbstractInsnNode n = mn.instructions.get(i);
                if (n instanceof LdcInsnNode) {
                    if (n.cst instanceof String) {
                        L.d("LdcInsnNode str=" + n.cst.toString())
                    } else if (n.cst instanceof Type) {
                        L.d("LdcInsnNode str=" + ((Type) n.cst).getClassName())
                    }
                } else if (n instanceof FieldInsnNode) {
                    L.d("FieldInsnNode " + n.owner + " " + n.name + " " + n.desc)
                } else if (n instanceof MethodInsnNode) {
                    L.d("MethodInsnNode " + n.owner + " " + n.name + " " + n.desc)
                    if (isFirst) {
                        if (isGetResource(n)) {
                            getIdMap.put(className + "." + methodName, getMethodR(mn.instructions, i))
                        }
                    } else {
                        if (n.owner.startsWith("java/lang/reflect/") || isGetId(n.owner, n.name) || getIdMap.containsKey(n.owner + "." + n.name) || reflectMap.containsKey(n.owner + "." + n.name)) {
                            String reflect = n.owner + "." + n.name
                            String type
                            if (n.owner.startsWith("java/lang/reflect/") || isGetId(n.owner, n.name)) {
                                type = "directCall"
                            } else {
                                type = "indirectCall"
                            }
                            Result result = new Result(type, String.valueOf(line), className, methodName, reflect, getMethodContext(mn.instructions, i), getMethodR(mn.instructions, i))
                            if (isGetResource(n) || getIdMap.containsKey(n.owner + "." + n.name)) {
                                if (getIdMap.containsKey(n.owner + "." + n.name)) {
                                    result.setrClass(getIdMap.get(n.owner + "." + n.name))
                                }
                                scannerResult.getResource.add(getSimplifyResult(result))
                                scannerResult.getResourceDetail.add(result)
                            } else {
                                scannerResult.useReflect.add(getSimplifyResult(result))
                                scannerResult.useReflectDetail.add(result)
                            }
                        }
                    }
                } else if (n instanceof LineNumberNode) {
                    line = n.line
                }
            }
        }
    }

    String getSimplifyResult(Result result) {
        return result.type + " " + result.className + "." + result.methodName + "(): " + result.lineNum + " { " + result.reflect + " }"
    }


    /**
     * 获取应用R资源的所有类型
     * @param name
     * @return
     */
    boolean isAppR(String name) {
        return name.startsWith(ScannerConfig.appId.replace(".", "/") + "/R\$")
    }

    int appRLength() {
        return (ScannerConfig.appId.replace(".", "/") + "/R\$").length()
    }


    boolean isGetResource(AbstractInsnNode n) {
        return (n.owner == "java/lang/reflect/Field" && n.name == "getInt" && n.previous instanceof InsnNode && n.previous.opcode == Opcodes.ACONST_NULL) || isGetId(n.owner, n.name)
    }

    boolean isGetId(String owner, String name) {
        if(needGetIdentifierResult){
            return owner == "android/content/res/Resources" && name == "getIdentifier"
        }
        return false
    }

    /**
     * 获取方法中从index前面所有符合资源名称的字符串常量，逗号分隔
     * @param list
     * @param index
     * @return
     */
    String getMethodContext(InsnList list, int index) {
        StringBuilder builder = new StringBuilder()
        for (int i = 0; i < index; i++) {
            AbstractInsnNode n = list.get(i);
            if (n instanceof LdcInsnNode) {
                if (n.cst instanceof String) {
                    Pattern pt = Pattern.compile("[\\w]+");
                    if (pt.matcher(n.cst.toString()).matches()) {
                        builder.append(n.cst.toString())
                        builder.append(",")
                    }
                }
            }
        }
        return builder.toString()
    }

    /**
     * 获取方法中从index前面所有包含R文件名称的信息，逗号分隔
     * @param list
     * @param index
     * @return
     */
    String getMethodR(InsnList list, int index) {
        StringBuilder builder = new StringBuilder()
        for (int i = 0; i < index; i++) {
            AbstractInsnNode n = list.get(i);
            if (n instanceof LdcInsnNode) {
                if (n.cst instanceof Type) {
                    if (((Type) n.cst).getClassName().contains(".R\$")) {
                        builder.append(((Type) n.cst).getClassName())
                        builder.append(",")
                    }
                }
            }
        }
        return builder.toString()
    }


    void writeResult(String buildPath) {
        StringBuilder builder = new StringBuilder()
        builder.append(buildPath)
        builder.append(File.separator)
        builder.append("outputs")
        builder.append(File.separator)
        builder.append("scanner_result.json")
        File file = new File(builder.toString())
        file.mkdirs()
        if (file.exists()) {
            file.delete()
        }
        for (int i = 0; i < scannerResult.getResourceDetail.size(); i++) {
            Result result = scannerResult.getResourceDetail[i];
            if (!TextUtils.isEmpty(result.rClass) && !TextUtils.isEmpty(result.ids)) {
                //TODO ywg 这里暂时先取第一个R
                String r = result.rClass.split(",")[0]
                String[] ids = result.ids.split(",")
                for (int j = 0; j < ids.length; j++) {
                    if (rTypes.contains(ids[j])) {
                        // getResources().getIdentifier("testident", "id", getPackageName());
                        //如果是这种情况，则R的类型改为R.id
                        r = "R." + ids[j]
                        break
                    }
                }
                StringBuilder builderId = new StringBuilder()
                for (int j = 0; j < ids.length; j++) {
                    if (!rTypes.contains(ids[j])) {
                        builderId.append(r)
                        builderId.append(".")
                        builderId.append(ids[j])
                        builderId.append(",")
                    }
                }
                String id = builderId.toString()
                if (!TextUtils.isEmpty(id)) {
                    scannerResult.possibleRids.add(id.substring(0,id.length()-1))
                }

            }
        }
        if (!needReflectResult) {
            scannerResult.useReflect.clear()
            scannerResult.useReflectDetail.clear()
        }
        file.withOutputStream {
            it.write(GsonUtils.getGson().toJson(scannerResult).getBytes("utf-8"))
        }
    }


}