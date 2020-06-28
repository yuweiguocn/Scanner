package io.github.yuweiguocn.classscanner.plugin

class Result {

    Result(String type,String lineNum, String className, String methodName, String reflect,String ids,String r) {
        this.type = type
        this.lineNum = lineNum
        this.className = replaceStr(className)
        this.methodName = methodName
        this.ids = replaceDot(ids)
        this.reflect = replaceStr(reflect)
        this.rClass = replaceDot(replaceSub(r))
    }

    String replaceDot(String str){
        if(str.endsWith(",")) {
            return str.substring(0, str.length() - 1)
        }
        return str
    }

    String replaceStr(String str) {
        return str.replace("/", ".")
    }

    String replaceSub(String str){
        String temp = str.replace("\$",".")
        return temp
    }

    /**
     * 类型：直接调用、间接调用
     */
    String type;

    /**
     * 行号
     */
    String lineNum;
    /**
     * 类名
     */
    String className;
    /**
     * 方法名
     */
    String methodName;
    /**
     * 上下文信息
     */
    String ids;

    /**
     * 使用的R类名
     */
    private String rClass;

    String getrClass() {
        return rClass
    }

    void setrClass(String rClass) {
        this.rClass = replaceDot(replaceSub(rClass))
    }
    /**
     * 调用的反射相关的信息
     */
    String reflect;
}