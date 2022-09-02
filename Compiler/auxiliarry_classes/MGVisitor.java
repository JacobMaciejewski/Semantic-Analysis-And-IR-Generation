package auxiliarry_classes;

import syntaxtree.*;
import visitor.*;
import java.util.*;

// Metadata Gatherer Visitor used in the first traversal of the abstract syntax tree
// it gathers the methods, fields of each class in the target program
// stores their types and offsets
public class MGVisitor extends GJDepthFirst<String, Void>{
    // mapping from class name to its info
    private Map<String, ClassInfo> symbolTable;
    // set of identifiers that we are currently gathering
    private Map<String, IdentifierInfo> activeIdentifiers;
    // set of methods that we are currently gathering
    private Map<String, MethodInfo> activeMethods;
    // the class whose body we are currently traversing
    ClassInfo traversedClass;
    // the method whose body we are currently traversing
    MethodInfo traversedMethod;

    public MGVisitor() {
        // initialize class name -> class info mapping
        Map<String, ClassInfo> initializedSymbolTable = new LinkedHashMap<String, ClassInfo>();
        this.setSymbolTable(initializedSymbolTable);
        // initially no class or method is traversed
        this.setTraversedClass(null);
        this.setTraversedMethod(null);
        // initially we are not gathering variables
        this.setActiveIdentifiers(null);
    }

    public void setSymbolTable(Map<String, ClassInfo> symbolTable) {
        this.symbolTable = symbolTable;
    }

    // sets currently traversed class
    // initially we are in the field space (no method traversed)
    public void setTraversedClass(ClassInfo traversedClass) {
        this.traversedClass = traversedClass;
        this.traversedMethod = null;
    }

    public void setTraversedMethod(MethodInfo traversedMethod) {
        this.traversedMethod = traversedMethod;
    }

    public void setActiveIdentifiers(Map<String, IdentifierInfo> activeIdentifiers) {
        this.activeIdentifiers = activeIdentifiers;
    }

    public void setActiveMethods(Map<String, MethodInfo> activeMethods) {
        this.activeMethods = activeMethods;
    }

    public Map<String, ClassInfo> getSymbolTable() {
        return this.symbolTable;
    }

    public Map<String, IdentifierInfo> getActiveIdentifiers() {
        return this.activeIdentifiers;
    }

    public Map<String, MethodInfo> getActiveMethods() {
        return this.activeMethods;
    }

    public ClassInfo getTraversedClassInfo() {
        return this.traversedClass;
    }

    public MethodInfo getTraversedMethodInfo() {
        return this.traversedMethod;
    }

    // checking if we are currently within method definition
    public boolean inMethodDefinition() {
        return (this.getTraversedMethodInfo() != null);
    }

    public boolean inExtendedClass() {
        return (!this.getTraversedClassInfo().isBaseClass());
    }

    // initializes a new instance of mapping
    // that will store the information about
    // all the identifiers that we will be gathering
    public void startIdentifiersGathering() {
        Map<String, IdentifierInfo> activeIdentifiersInfo = new LinkedHashMap<String, IdentifierInfo>();
        this.setActiveIdentifiers(activeIdentifiersInfo); 
    }

    // initializes a new instance of mapping
    // that will store the information about
    // all the methods that we will be gathering
    public void startMethodsGathering() {
        Map<String, MethodInfo> activeMethodsInfo = new LinkedHashMap<String, MethodInfo>();
        this.setActiveMethods(activeMethodsInfo); 
    }

    // adds given identifier info to the active identifiers set
    public void addActiveIdentifier(IdentifierInfo identifierInfo) {
        String newIdentifierName = identifierInfo.getName();
        this.getActiveIdentifiers().put(newIdentifierName, identifierInfo);
    }

    // adds given method info to the active methods set
    public void addActiveMethod(MethodInfo methodInfo) {
        String newMethodName = methodInfo.getName();
        this.getActiveMethods().put(newMethodName, methodInfo);
    }

    public void checkForDuplicateIdentifier(String newIdentifierName) throws Exception {
        String currentClass = this.getTraversedClassInfo().getClassName();

        if(this.getActiveIdentifiers().containsKey(newIdentifierName)) {

            // local variable redefinition
            if(this.inMethodDefinition()){
                String currentMethod = this.getTraversedMethodInfo().getName();

                throw new Exception("Class " + currentClass + " - Method " + currentMethod + " : Variable " + newIdentifierName + " has already been declared");
            }else{ //field redefinition
                throw new Exception("Class " + currentClass + " : Field " + newIdentifierName + " has already been declared");
            }
        }
    }

    public void checkForDuplicateMethod(String newMethodName) throws Exception {
        String currentClass = this.getTraversedClassInfo().getClassName();

        if(this.getActiveMethods().containsKey(newMethodName)) {
                throw new Exception("Class " + currentClass + " :  Method " + newMethodName + " has already been declared");
        }
    }

    // checks if we are redefining a parameter in the same method definition
    public void checkForDuplicateParameter(String newParameterName) throws Exception {
        if(this.getTraversedMethodInfo().getArguments().containsKey(newParameterName)) {
            String currentClass = this.getTraversedClassInfo().getClassName();
            String currentMethod = this.getTraversedMethodInfo().getName();
            throw new Exception("Class " + currentClass + " - Method " + currentMethod + " : Parameter " + newParameterName + " duplicate");
        }
    }

    // checks if there is a name conflict between
    // currently traversed method parameters and 
    // local variables
    public void checkForParameterVariableConflict() throws Exception{

        Map<String, IdentifierInfo> traversedMethodParameters = this.getTraversedMethodInfo().getArguments();
        Map<String, IdentifierInfo> localVariables = this.getActiveIdentifiers();

        String currentClass = this.getTraversedClassInfo().getClassName();
        String currentMethod = this.getTraversedMethodInfo().getName();

        // check if any parameter name appears in the set of local variables namespace
        for (Map.Entry<String,IdentifierInfo> parameter : traversedMethodParameters.entrySet()) {
            for (Map.Entry<String,IdentifierInfo> variable : localVariables.entrySet()) {

                String parameterName = parameter.getKey();
                String variableName = variable.getKey();
                // parameter name same with a local variable name
                // throw error
                if(parameterName.equals(variableName)){
                    throw new Exception("Class " + currentClass + " - Method " + currentMethod + " : Parameter/Variable name conflict for " + parameterName);
                }
            
            }
        }
    }

    // checks if parent class has been defined
    // ergo is contained in the symbol table
    public void checkParentUndefined(String currentClassName, String parentClassName) throws Exception{
        if(!this.getSymbolTable().containsKey(parentClassName)) {
            throw new Exception("Class " + currentClassName + " : Parent class " + parentClassName + " has not been defined");
        }
    }

    // checks if class methods are overloading
    // extended classes methods
    public void checkOverloading() throws Exception{

        Map<String,MethodInfo> currentClassMethods = this.getActiveMethods();

        for (Map.Entry<String,MethodInfo> method : currentClassMethods.entrySet()) {

            String currentMethodName = method.getKey();
            MethodInfo currentMethodInfo = method.getValue();

            // currently checked class in the inheritance hierarchy
            // initially we are in the traversed class
            String extendedClassName = this.getTraversedClassInfo().getClassName();
            ClassInfo extendedClass = this.getTraversedClassInfo();

            do {
                // check for current extended class in inheritance hierarchy
                extendedClassName = extendedClass.getParentClass();
                extendedClass = this.getSymbolTable().get(extendedClassName);

                // there is a method instance in current extended class
                // with the same name as checked method
                if(extendedClass.containsMethodWithName(currentMethodName)) {

                    // extended class method is overloaded by current function
                    // resulting in error
                    if(extendedClass.functionOverloads(currentMethodInfo)) {
                        throw new Exception("Class " + this.getTraversedClassInfo().getClassName() + " : Function " + currentMethodName + " overloads");

                    }else //current method simply overrides
                    {
                        break;
                    }

                }

            }while(!extendedClass.isBaseClass());
        }
    }

    public void checkMainArgumentConflict(String mainClassName, String argumentName) throws Exception {

        // one of main function's local variables
        // has the same name as the argument
        if(this.getActiveIdentifiers().containsKey(argumentName)) {
            throw new Exception("Class " + mainClassName + " - Method main : Parameter/Variable name conflict for " + argumentName);
        }
    }

    public void checkClassRedefinition(String givenClassName) throws Exception{
        if(this.getSymbolTable().containsKey(givenClassName)) {
            throw new Exception("Class " + givenClassName + " has already been defined");
        }
    }

    public void initializeClassOffsets(ClassInfo newClassInfo) {

        // if class is not a base class
        // its initial offsets are the final offsets of the parent class
        if(!newClassInfo.isBaseClass())
        {
            // get extended class
            String extendedClassName = newClassInfo.getParentClass();
            ClassInfo extendedClassInfo = this.getSymbolTable().get(extendedClassName);

            // inherit its offsets
            newClassInfo.setCurrentFieldOffset(extendedClassInfo.getCurrentFieldOffset());
            newClassInfo.setCurrentMethodOffset(extendedClassInfo.getCurrentMethodOffset());
        }

    }

    // checks if class overrides given method 
    // (method with same name exists higher in the inheritance hierarchy)
    public boolean classOverridesMethod(ClassInfo givenClassInfo, MethodInfo givenMethodInfo) {

        if(givenClassInfo.isBaseClass()) {
            return false;
        }

        // initially in given class
        String extendedClassName = givenClassInfo.getClassName();
        ClassInfo extendedClass = givenClassInfo;

        do {
                // check for current extended class in inheritance hierarchy
                extendedClassName = extendedClass.getParentClass();
                extendedClass = this.getSymbolTable().get(extendedClassName);

                // extended class contains method with given method's name
                if(extendedClass.containsMethodWithName(givenMethodInfo.getName())) {
                    return true;
                }

        }while(!extendedClass.isBaseClass());

        return false;
    }

    // returns the size of the given type in bytes
    public Integer typeToBytes(String givenType) {

        if(givenType.equals("int")) {
            return 4;
        }else if(givenType.equals("boolean")){
            return 1;
        }else{ // all pointers are of size 8
            return 8;
        }
    }

    public void updateClassOffsets(ClassInfo newClassInfo, Map<String, IdentifierInfo> newClassFields, Map<String, MethodInfo> newClassMethods) {

        // for each field, give it the proper offset
        // while simultaneously updating general field offset counter of the class
        for (Map.Entry<String,IdentifierInfo> newFieldInfo : newClassFields.entrySet()) {

            Integer fieldOffset = newClassInfo.getCurrentFieldOffset();
            String currentFieldType = newFieldInfo.getValue().getType(); 
            
            // set the proper offset for current field
            newFieldInfo.getValue().setOffset(fieldOffset);

            // increment general offset counter for class fields
            newClassInfo.incrementFieldOffset(this.typeToBytes(currentFieldType));
        }

        if(!newClassInfo.isMain()) { //main has no offset
            // for each method, give it the proper offset
            // while simultaneously updating general method offset counter of the class
            // no increment in case of overriden method
            for (Map.Entry<String,MethodInfo> newMethodInfo : newClassMethods.entrySet()) {

                Integer methodOffset = newClassInfo.getCurrentMethodOffset();

                // newly defined method, no overriding so offset should be updated
                if(!this.classOverridesMethod(newClassInfo, newMethodInfo.getValue())) {

                    // set the proper offset for current field
                    newMethodInfo.getValue().setOffset(methodOffset);
                    // increment general offset counter for class fields
                    newClassInfo.incrementMethodOffset(this.typeToBytes("function"));
                }
            }
        }
    }

    public void addClassInfo(ClassInfo newClassInfo, Map<String, IdentifierInfo> newClassFields, Map<String, MethodInfo> newClassMethods) {

        // in the case of a class that extends
        // inherit parent's offsets
        this.initializeClassOffsets(newClassInfo);

        // updates the offsets of fields and methods and the general offsets
        this.updateClassOffsets(newClassInfo, newClassFields, newClassMethods);

        // insert fields and methods into class information object
        newClassInfo.addFields(newClassFields);
        newClassInfo.addMethods(newClassMethods);

        // add class entry into the symbol table
        this.getSymbolTable().put(newClassInfo.getClassName(), newClassInfo);
    }

    // constructs a mapping to a single method information object
    // containing informatio about main function
    public Map<String, MethodInfo> constructMainFunction(String mainArgumentName, Map<String, IdentifierInfo> mainFunctionVariables) {

        Map<String, MethodInfo> mainFunctionMap = new LinkedHashMap<String, MethodInfo>();
        MethodInfo mainFunction = new MethodInfo("main", "static void");
        IdentifierInfo mainArgument = new IdentifierInfo(mainArgumentName, "String[]");

        mainFunction.addParameter(mainArgument);
        mainFunction.addLocalVariables(mainFunctionVariables);

        mainFunctionMap.put(mainFunction.getName(), mainFunction);

        return mainFunctionMap;
    }


    public void printOffsets() {

        Map<String, ClassInfo> allClassInfo = this.getSymbolTable();

        // for each class in the mini java file
        for (Map.Entry<String,ClassInfo> currentClassEntry : allClassInfo.entrySet()) {

            ClassInfo currentClassInfo = currentClassEntry.getValue();
            String currentClassName = currentClassInfo.getClassName();

            // get current class fields and functions
            Map<String, IdentifierInfo> currentClassFields = currentClassInfo.getFieldsInfoMap();
            Map<String, MethodInfo> currentClassMethods = currentClassInfo.getMethodsInfoMap();

            // for each field print its offset
            for (Map.Entry<String,IdentifierInfo> currentFieldEntry : currentClassFields.entrySet()) {
                IdentifierInfo currentFieldInfo = currentFieldEntry.getValue();
                String currentFieldName = currentFieldInfo.getName();
                Integer currentFieldOffset = currentFieldInfo.getOffset();

                System.out.println(currentClassName + "." + currentFieldName + " : " + currentFieldOffset);
            }

            // for each non-overriding function, print its offset
            for (Map.Entry<String,MethodInfo> currentMethodEntry : currentClassMethods.entrySet()) {
                MethodInfo currentMethodInfo = currentMethodEntry.getValue();
                String currentMethodName = currentMethodInfo.getName();
                Integer currentMethodOffset = currentMethodInfo.getOffset();


                if(!currentMethodInfo.overrides()) {
                    System.out.println(currentClassName + "." + currentMethodName + " : " + currentMethodOffset);
                }
                
            }
        }
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
    */

    @Override
    public String visit(MainClass n, Void argu) throws Exception {
        String _ret=null;
        String mainClassName = n.f1.accept(this, null);
        String mainFunctionArgumentName = n.f11.accept(this, null);

        // main class information
        ClassInfo mainClass = new ClassInfo(mainClassName);
        mainClass.setMainStatus(true);
        Map<String, IdentifierInfo> mainFunctionVariables;
        Map<String, MethodInfo> mainClassMethod;
        // main class has no fields by default
        Map<String, IdentifierInfo> mainClassFields = new LinkedHashMap<String, IdentifierInfo>();

        this.setTraversedClass(mainClass);

        // gather only fields (main has no other methods than main)
        this.startIdentifiersGathering();
        n.f14.accept(this, null);

        // check if single main function argument
        // appears in its local variables name space
        this.checkMainArgumentConflict(mainClassName, mainFunctionArgumentName);

        mainFunctionVariables = this.getActiveIdentifiers();
        // the single main method constructor
        mainClassMethod = this.constructMainFunction(mainFunctionArgumentName, mainFunctionVariables);

        // add main class into the symbol table
        this.addClassInfo(mainClass, mainClassFields, mainClassMethod);

        return _ret;
    }

    /**
    * f0 -> ClassDeclaration()
    *       | ClassExtendsDeclaration()
    */
    @Override
    public String visit(TypeDeclaration n, Void argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
    @Override
    public String visit(ClassDeclaration n, Void argu) throws Exception {
        String _ret=null;
        String className = n.f1.accept(this, argu);

        // initializing a new class information object
        ClassInfo newClassInfo = new ClassInfo(className);

        this.checkClassRedefinition(className);

        // we traversing the newly defined class
        this.setTraversedClass(newClassInfo);

        // gather class fields (checks for field name duplicates)
        this.startIdentifiersGathering();
        n.f3.accept(this, argu);
        // get fields for class information construction
        Map<String, IdentifierInfo> newClassFields = this.getActiveIdentifiers();

        // gather class methods (checks for method name duplicates)
        this.startMethodsGathering();
        n.f4.accept(this, argu);
        // get methods for class information construction
        Map<String, MethodInfo> newClassMethods = this.getActiveMethods();

        // add currently traversed class info to symbol table
        this.addClassInfo(newClassInfo, newClassFields, newClassMethods);

        return _ret;
    }

    /**
     * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
    @Override
    public String visit(ClassExtendsDeclaration n, Void argu) throws Exception {
        String _ret=null;
        // get class info
        String className = n.f1.accept(this, argu);
        String extendedClassName = n.f3.accept(this, argu);

        this.checkClassRedefinition(className);
        // check if parent class has been defined
        // before current class
        this.checkParentUndefined(className, extendedClassName);

        // initializing a new class information object
        ClassInfo newClassInfo = new ClassInfo(className, extendedClassName);

        // we traversing the newly defined class
        this.setTraversedClass(newClassInfo);

        // gather class fields (checks for field name duplicates)
        this.startIdentifiersGathering();
        n.f5.accept(this, argu);

        // get fields for class information construction
        Map<String, IdentifierInfo> newClassFields = this.getActiveIdentifiers();

        // gather class methods (checks for method name duplicates)
        this.startMethodsGathering();
        n.f6.accept(this, argu);

        // check if one of current class methods is overloading
        // a method defined in parent class
        this.checkOverloading();
        // get methods for class information construction
        Map<String, MethodInfo> newClassMethods = this.getActiveMethods();

        // add currently traversed class info to symbol table
        this.addClassInfo(newClassInfo, newClassFields, newClassMethods);

        return _ret;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    @Override
    public String visit(VarDeclaration n, Void argu) throws Exception {
        String _ret=null;
        // get identifier info
        String identifierType = n.f0.accept(this, argu);
        String identifierName = n.f1.accept(this, argu);

        // constuct an object containing information
        // about a newly defined identifier
        IdentifierInfo newIdentifierInfo = new IdentifierInfo(identifierName, identifierType);

        // checks if we are within a class fields definition space or within a method
        // checks for field/local variable double definition
        this.checkForDuplicateIdentifier(identifierName);
        // add it to the set of active identifiers
        // in the case of field, no offset is specified (done in class check)
        this.addActiveIdentifier(newIdentifierInfo);

        return _ret;
    }

    /**
     * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
    @Override
    public String visit(MethodDeclaration n, Void argu) throws Exception {
        
        String _ret=null;

        String newMethodType = n.f1.accept(this, argu);
        String newMethodName = n.f2.accept(this, argu);

        MethodInfo newMethodInfo = new MethodInfo(newMethodName, newMethodType);

        // we traversing the newly defined method
        this.setTraversedMethod(newMethodInfo);

        // store method's parameters (checking for parameters duplicates)
        n.f4.accept(this, argu);

        // gather method's local variables (checking for variables duplicates)
        this.startIdentifiersGathering();
        n.f7.accept(this, argu);

        // check if there is a name conflict between currently traversed
        // method's arguments and local variables
        this.checkForParameterVariableConflict();

        // store local variables for currently traversed method
        this.getTraversedMethodInfo().addLocalVariables(this.getActiveIdentifiers());

        // checking if current method has already been defined in current class
        this.checkForDuplicateMethod(newMethodName);

        // add current method's information to the active pool
        this.addActiveMethod(newMethodInfo);

        return _ret;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    @Override
    public String visit(FormalParameter n, Void argu) throws Exception {
        String _ret=null;
        
        String parameterType = n.f0.accept(this, argu);
        String parameterName = n.f1.accept(this, argu);

        // construct information object for current parameter
        // update current method's information
        IdentifierInfo newParameterInfo = new IdentifierInfo(parameterName, parameterType);

        // check if we have a second definition with the same name
        // in the arguments of currently traversed method
        this.checkForDuplicateParameter(parameterName);

        // if no redefinition
        // simply add the newly defined parameter into currenly traversed method's parameters
        this.getTraversedMethodInfo().addParameter(newParameterInfo);

        return _ret;
    }

    /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
    @Override
    public String visit(Type n, Void argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    @Override
    public String visit(ArrayType n, Void argu) throws Exception {
        return "int[]";
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    @Override
    public String visit(Identifier n, Void argu) throws Exception {
        return n.f0.toString();
    }

    /**
    * f0 -> "boolean"
    */
    @Override
    public String visit(BooleanType n, Void argu) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "int"
    */
    @Override
    public String visit(IntegerType n, Void argu) throws Exception {
        return n.f0.toString();
    }
}