package io.github.yuweiguocn.classscanner.plugin

class ScannerResult {
    public Set<String> possibleRids
    public Set<String> getResource
    public Set<String> useReflect
    public List<Result> getResourceDetail
    public List<Result> useReflectDetail

    ScannerResult() {
        getResourceDetail = new ArrayList<>()
        useReflectDetail = new ArrayList<>()
        possibleRids = new TreeSet<>()
        getResource = new TreeSet<>()
        useReflect = new TreeSet<>()
    }
}