package auxiliarry_classes;
import java.util.*;

public class ClassInfo {
    private String className;
    private String parentClass;

    public boolean isMain;

    // maps from field/method name to their type and offset
    Map<String, IdentifierInfo> fieldsInfoMap;
    Map<String, MethodInfo> methodsInfoMap;
    // used to infer the offset from the class beginning
    // from which we can store newly found fields and methods
    // both methods and fields have different memory spaces
    private Integer currentFieldOffset;
    private Integer currentMethodOffset;

    public ClassInfo(String className) {
        // initialize the mapping from field/method name to their information
        Map<String, IdentifierInfo> fieldsInfoMapping = new LinkedHashMap<String, IdentifierInfo>();
        Map<String, MethodInfo> methodsInfoMapping = new LinkedHashMap<String, MethodInfo>();

        this.setClassName(className);
        this.setParentClass(null);
        this.setFieldsInfoMap(fieldsInfoMapping);
        this.setMethodsInfoMap(methodsInfoMapping);
        this.setCurrentFieldOffset(0);
        this.setCurrentMethodOffset(0);
        // it is not a main class by default
        this.isMain = false;
    }

    public ClassInfo(String className, String parentClass) {
        // initialize the mapping from field/method name to their information
        Map<String, IdentifierInfo> fieldsInfoMapping = new LinkedHashMap<String, IdentifierInfo>();
        Map<String, MethodInfo> methodsInfoMapping = new LinkedHashMap<String, MethodInfo>();

        this.setClassName(className);
        this.setParentClass(parentClass);
        this.setFieldsInfoMap(fieldsInfoMapping);
        this.setMethodsInfoMap(methodsInfoMapping);
        this.setCurrentFieldOffset(0);
        this.setCurrentMethodOffset(0);
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setParentClass(String parentClass) {
        this.parentClass = parentClass;
    }

    public void setCurrentFieldOffset(Integer currentFieldOffset) {
        this.currentFieldOffset = currentFieldOffset;
    }

    public void setCurrentMethodOffset(Integer currentMethodOffset) {
        this.currentMethodOffset = currentMethodOffset;
    }

    public void setFieldsInfoMap(Map<String, IdentifierInfo> fieldsInfoMap) {
        this.fieldsInfoMap = fieldsInfoMap;
    }

    public void setMethodsInfoMap(Map<String, MethodInfo> methodsInfoMap) {
        this.methodsInfoMap = methodsInfoMap;
    }

    public void setMainStatus(boolean status) {
        this.isMain = status;
    }

    public void incrementFieldOffset(Integer increment) {
        this.currentFieldOffset += increment;
    }

    public void incrementMethodOffset(Integer increment) {
        this.currentMethodOffset += increment;
    }

    public String getClassName() {
        return this.className;
    }

    public String getParentClass() {
        return this.parentClass;
    }

    public Integer getCurrentFieldOffset() {
        return this.currentFieldOffset;
    }

    public Integer getCurrentMethodOffset() {
        return this.currentMethodOffset;
    }


    public Map<String, IdentifierInfo> getFieldsInfoMap() {
        return this.fieldsInfoMap;
    }


    public Map<String, MethodInfo> getMethodsInfoMap() {
        return this.methodsInfoMap;
    }


    public MethodInfo getMethod(String requestedMethodName) {
        return this.getMethodsInfoMap().get(requestedMethodName);
    }

    public IdentifierInfo getField(String requestedFieldName) {
        return this.getFieldsInfoMap().get(requestedFieldName);
    }

    public boolean isBaseClass() {
        return (this.getParentClass() == null);
    }

    public boolean isMain() {
        return this.isMain;
    }

    // checks if method with given name
    // is defined in current class
    public boolean containsMethodWithName(String methodName) {
        return (this.getMethodsInfoMap().containsKey(methodName));
    }

    // checks if given method
    // overloads a method defined in the class
    public boolean functionOverloads(MethodInfo givenMethodInfo) {
        Map<String, MethodInfo> classMethods = this.getMethodsInfoMap();

        for (Map.Entry<String,MethodInfo> classMethodEntry : classMethods.entrySet()) {

            MethodInfo classMethodInfo = classMethodEntry.getValue();

            if(classMethodInfo.isOverloadedBy(givenMethodInfo)) {
                return true;
            }
        }

        return false;
    }

    // inserts information about class fields
    // into the information object
    public void addFields(Map<String, IdentifierInfo> newFieldsInfo) {

        for (Map.Entry<String,IdentifierInfo> newFieldInfo : newFieldsInfo.entrySet()) {
            this.getFieldsInfoMap().put(newFieldInfo.getKey(), newFieldInfo.getValue());
        }
    }


    // inserts information about class methods
    // into the information object
    public void addMethods(Map<String, MethodInfo> newMethodsInfo) {

        for (Map.Entry<String,MethodInfo> newMethodInfo : newMethodsInfo.entrySet()) {
            this.getMethodsInfoMap().put(newMethodInfo.getKey(), newMethodInfo.getValue());
        }
    }
}

