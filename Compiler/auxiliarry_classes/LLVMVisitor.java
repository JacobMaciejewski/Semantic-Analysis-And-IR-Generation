package auxiliarry_classes;

import syntaxtree.*;
import visitor.*;
import java.util.*;

import java.io.*;
import java.nio.file.*;

public class LLVMVisitor extends GJDepthFirst<String, String>{

    // mapping from class name to its info
    private Map<String, ClassInfo> symbolTable;
    // the class whose body we are currently traversing
    private ClassInfo traversedClass;
    // the method whose body we are currently traversing
    private MethodInfo traversedMethod;
    // the file that we are currently writing to
    private String fileToWrite;
    PrintWriter fileWriter;
    // text that we will be writing to file
    private String textToWrite;
    // counters for proper label and register naming
    int registerCounter;
    int ifLabelCounter;
    int loopLabelCounter;
    // to store the registers of the arguments of current function call
    LinkedList<LinkedList<String>> argumentRegisters;
    
    public LLVMVisitor(String traversedFile, Map<String, ClassInfo> symbolTable) {
        // set the name of the file we will be writting into
        // which is the name of the traversed java file with .ll suffix
        this.setFileToWrite(this.fileNameToLLVM(traversedFile));
        this.setSymbolTable(symbolTable);
        this.setTraversedClass(null);
        this.setTraversedMethod(null);
        // construct a list containing a list of active arguments for analyzed calls
        this.initializeArgumentRegisters();
        this.setTextToWrite("");
        // set all counters to 0
        this.enterScope();
    }

    void setSymbolTable(Map<String, ClassInfo> symbolTable) {
        this.symbolTable = symbolTable;
    }

    // set given class as the traversed one
    // initially we are not traversing a method
    void setTraversedClass(ClassInfo traversedClass) {
        this.traversedClass = traversedClass;
        this.setTraversedMethod(null);
    }

    void setTraversedMethod(MethodInfo traversedMethod) {
        this.traversedMethod = traversedMethod;
    }

    void setFileToWrite(String fileToWrite) {
        this.fileToWrite = fileToWrite;
    }

    void setTextToWrite(String textToWrite) {
        this.textToWrite = textToWrite;
    }

    // add a new list of arguments to the list containing all argument lists for active function calls
    void initializeArgumentGathering() {
        // a new list of arguments (a new function call found)
        LinkedList<String> newArgumentList = new LinkedList<String>();
        this.getArgumentRegisters().add(newArgumentList);
    }

    // remove the most recent list of arguments (gathered all the arguments of the most recent call)
    void terminateArgumentGathering() {
        this.getArgumentRegisters().removeLast();
    }

    // adds the register of the most recently traversed argument
    // to the list containing the arguments of the most recent call
    void addNewArgument(String argumentRegister) {
        this.getArgumentRegisters().peekLast().add(argumentRegister);
    }

    void initializeArgumentRegisters() {
        LinkedList<LinkedList<String>> newArgumentRegisters = new LinkedList<LinkedList<String>>();
        this.setArgumentRegisters(newArgumentRegisters);
    }

    void setArgumentRegisters(LinkedList<LinkedList<String>> argumentRegisters) {
        this.argumentRegisters = argumentRegisters;
    }

    void setFileWriter(PrintWriter fileWriter) {
        this.fileWriter = fileWriter;
    }

    PrintWriter getFileWriter() {
        return fileWriter;
    }

    LinkedList<LinkedList<String>> getArgumentRegisters() {
        return this.argumentRegisters;
    }

    Map<String, ClassInfo> getSymbolTable(){
        return this.symbolTable;
    }

    ClassInfo getTraversedClass() {
        return this.traversedClass;
    }

    MethodInfo getTraversedMethod() {
        return this.traversedMethod;
    }

    ClassInfo getClass(String className) {
        return this.getSymbolTable().get(className);
    }

    // returns the class info about parent class
    // if base class name is given, null is returned
    ClassInfo getParent(String childClass) {

        if(this.getClass(childClass).isBaseClass()) {
            return null;
        }else
        {
            return this.getClass(this.getClass(childClass).getParentClass()); 
        }
    }

    Map<String, IdentifierInfo> getClassFields(String className) {
        return this.getClass(className).getFieldsInfoMap();
    }

    IdentifierInfo getClassField(String className, String fieldName) {
        return this.getClassFields(className).get(fieldName);
    }

    Map<String, MethodInfo> getClassMethods(String className) {
        return this.getClass(className).getMethodsInfoMap();
    }

    MethodInfo getClassMethod(String className, String methodName) {
        return this.getClassMethods(className).get(methodName);
    }

    String getFileToWrite() {
        return this.fileToWrite;
    }

    String getTextToWrite() {
        return this.textToWrite;
    }

    void setRegisterCounter(int registerCounter) {
        this.registerCounter = registerCounter;
    }

    void setIfLabelCounter(int ifLabelCounter) {
        this.ifLabelCounter = ifLabelCounter;
    }

    void setLoopLabelCounter(int loopLabelCounter) {
        this.loopLabelCounter = loopLabelCounter;
    }

    int getRegisterCounter() {
        return this.registerCounter;
    }

    int getIfLabelCounter() {
        return this.ifLabelCounter;
    }

    int getLoopLabelCounter() {
        return this.loopLabelCounter;
    }

    // produces a new register
    String getNewRegister() {
        int currentCounter = this.getRegisterCounter();
        this.setRegisterCounter(currentCounter + 1);

        return "%_" + currentCounter;
    }

    // produces a new if label
    String getNewIfLabel() {
        int currentCounter = this.getIfLabelCounter();
        this.setIfLabelCounter(currentCounter + 1);

        return String.valueOf(currentCounter);
    }

    // preduces a new loop label
    String getNewLoopLabel() {
        int currentCounter = this.getLoopLabelCounter();
        this.setLoopLabelCounter(currentCounter + 1);

        return String.valueOf(currentCounter);
    }

    // reinitialize all label and register counters
    void enterScope() {
        this.setRegisterCounter(0);
        this.setIfLabelCounter(0);
        this.setLoopLabelCounter(0);
    }

    boolean traversingMethod() {
        return (this.getTraversedMethod() != null);
    }

    // fetches information about the variable with specified name
    // returns null is no such variable has been declared anywhere in the inheritance tree
    IdentifierInfo fetchVariable(String requestedVariable) {
        ClassInfo currentClass = this.getTraversedClass();
        MethodInfo currentMethod = this.getTraversedMethod();

        Map<String, IdentifierInfo> currentArguments = currentMethod.getArguments();
        Map<String, IdentifierInfo> currentLocalVariables = currentMethod.getVariables();

        // check for variable name in local variables
        if(currentLocalVariables.containsKey(requestedVariable)) {
            return currentLocalVariables.get(requestedVariable);
        }

        // check for variable name in arguments
        if(currentArguments.containsKey(requestedVariable)) {
            return currentArguments.get(requestedVariable);
        }

        do //for each class in inheritance tree, check for requested variable in field space
            if(currentClass.getFieldsInfoMap().containsKey(requestedVariable)) {
                return currentClass.getFieldsInfoMap().get(requestedVariable);
            }
        while((currentClass = this.getParent(currentClass.getClassName())) != null);

        // variable not found
        return null;
    }

    // returns information about the first instance of definition of requested function
    // in requested class inheritance tree
    MethodInfo fetchClassFunction(String classType, String FunctionName) {

        ClassInfo currentClass = this.getClass(classType);

        do //traverse all classes in the inheritance tree
            if(currentClass.getMethodsInfoMap().containsKey(FunctionName)) {
                return currentClass.getMethodsInfoMap().get(FunctionName);
            }
        while((currentClass = this.getParent(currentClass.getClassName())) != null);

        return null;
    }

    String fileNameToLLVM(String fileName) {
        int slashIndex = fileName.lastIndexOf("/");
        String temporaryFileName = fileName.substring(slashIndex + 1);
        temporaryFileName = this.removeSuffix(temporaryFileName, ".java");

        return "./output_ll/" + temporaryFileName + ".ll";
    }

    // remove a specific suffix from a string
    String removeSuffix(String s, String suffix)
    {
        if (s != null && suffix != null && s.endsWith(suffix)) {
            return s.substring(0, s.length() - suffix.length());
        }
        return s;
    }

    // writes the given string to the target file
    void stringToFile(String stringToWrite) throws Exception {
        this.setTextToWrite(stringToWrite);
        this.flushToFile();
    }

    // same as string to file, but add a tab at the beginning and enter at end
    // for pretty printing
    void instructionToFile(String stringToWrite) throws Exception {
        this.stringToFile("\t" + stringToWrite + "\n");
    }

    // writes the gathered string to the target file 
    void flushToFile() throws Exception {
        this.getFileWriter().write(this.getTextToWrite());
        if(this.getFileWriter().checkError()) {
            throw new Exception( "\nCould not write to file " + this.getFileToWrite());
        }
        // Files.write( Paths.get(this.getFileToWrite()), this.getTextToWrite().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND );
        // text to write to file is reinitialized
        this.setTextToWrite("");
    }

    // adds the given text to the head of the total text
    // that we will be then writting to the target ll file
    void appendTextToWrite(String textTail) {
        this.setTextToWrite(this.getTextToWrite() + textTail);
    }

    // adds the given text to the head of the total text
    // that we will be then writting to the target ll file
    void extendTextToWrite(String textHead) {
        this.setTextToWrite(textHead + this.getTextToWrite());
    }

    // matches given java types to llvm ones
    String toLLVMType(String givenType) {
        if(givenType.equals("int")){
            return "i32";
        }
        else if(givenType.equals("boolean")){
            return "i1";
        }
        else if(givenType.equals("int[]")){
            return "i32*";
        }
        else{
            return "i8*";
        }
    }

    // returns the name of the class nearest to the given one
    // in the inheritance hierarchy overriding given method
    String getLatestOverridingClassName(ClassInfo targetClass, MethodInfo targetMethod) {

        String targetMethodName = targetMethod.getName();
        ClassInfo currentClass = targetClass;
        do //for each class in inheritance tree, check for requested method overriding
            if(currentClass.getMethodsInfoMap().containsKey(targetMethodName)) {
                return currentClass.getClassName();
            }
        while((currentClass = this.getParent(currentClass.getClassName())) != null);

        return null;
    }

    // returns a string containing all the string representation of methods contained in the
    // inheritance tree of target class
    String constructVTableMembers(ClassInfo targetClass, LinkedList<MethodInfo> VTableMethods) throws Exception {
       
        String vTableMembers = "";
        // the name of the class in which the latest overriding of current method occured
        String definitionClassName;
        Boolean firstMethod = true;

        for (MethodInfo VTableMethod : VTableMethods) {
            // get the name of the class in which the function was last overriden
            definitionClassName = this.getLatestOverridingClassName(targetClass, VTableMethod);
            String currentVTableMember = this.constructVTableMember(definitionClassName, VTableMethod);

            // check if method has already been added to virtual table
            if(firstMethod) {
                firstMethod = false;
                vTableMembers = vTableMembers.concat(currentVTableMember);
            }else{
                vTableMembers = vTableMembers.concat("," + currentVTableMember);
            }
        }

        return vTableMembers;
    }

    // constructs the string representing a method entry in the virtual table
    String constructVTableMember(String currentClassName, MethodInfo currentMethod) throws Exception {

        String currentMethodName = currentMethod.getName();
        Map<String, IdentifierInfo> currentMethodArguments = currentMethod.getArguments();

        // add the information about the type of the method
        String methodEntryHead = "\n\ti8* bitcast (" + this.toLLVMType(currentMethod.getType()) + " (";

        // gather the types of the arguments
        String methodEntryArgs = "i8*";
        // for each argument get its corresponding LLVM type
        for (Map.Entry<String,IdentifierInfo> currentMethodArgumentEntry : currentMethodArguments.entrySet()) {
            IdentifierInfo currentMethodArgument = currentMethodArgumentEntry.getValue();
            String argumentType = this.toLLVMType(currentMethodArgument.getType());


            methodEntryArgs=methodEntryArgs.concat("," + argumentType);
        }

        // add the information about method and class names
        String methodEntryTail = ")* @" + currentClassName + "." + currentMethodName + " to i8*)";

        // append current method entry into the virtual table string
        return methodEntryHead + methodEntryArgs + methodEntryTail;
    }

    // traverse the inheritance tree from top to bottom
    // adding newly defined methods into the virtual table of the target class
    void gatherVTableMethods(ClassInfo currentClass, LinkedList<MethodInfo> VTableMethods) throws Exception {

        ClassInfo parentClass;
        String currentClassName = currentClass.getClassName();
        Map<String, MethodInfo> currentClassMethods = currentClass.getMethodsInfoMap();
        Boolean newMethod;

        // parent class exists, gather from it first
        if ((parentClass = this.getParent(currentClassName)) != null) {
            this.gatherVTableMethods(parentClass, VTableMethods);
        }

        // for each method of current class in the inheritance tree
        // check if it has already been added to the virtual table
        // if no, add it
        for (Map.Entry<String,MethodInfo> currentClassMethodEntry : currentClassMethods.entrySet()) {
            MethodInfo currentClassMethod = currentClassMethodEntry.getValue();
            String currentClassMethodName = currentClassMethod.getName();

            // initialy regarding method as non overriding
            newMethod = true;
            for (MethodInfo VTableMethod : VTableMethods) {
                // check if method has already been added to virtual table
                if(VTableMethod.getName().equals(currentClassMethodName)) {
                    newMethod = false;
                    break;
                }
            }

            // found a new method in the inheritance tree
            // add it to the virtual table
            if(newMethod) {
                VTableMethods.add(currentClassMethod);
            }
        }
    }

    void initializeVTable(ClassInfo targetClass, LinkedList<MethodInfo> VTableMethods) throws Exception {

        String targetClassName = targetClass.getClassName();
        // gathering all unique methods in the inheritance tree
        this.gatherVTableMethods(targetClass, VTableMethods);
        Integer totalMethodsNum = VTableMethods.size();

        // specify the size of the virtual table
        String vTableHead = "@." + targetClassName + "_vtable = global";
        String vTableSize = " [" + totalMethodsNum + " x i8*] [";

        // add the virtual table metadata
        this.appendTextToWrite(vTableHead + vTableSize);
        this.appendTextToWrite(this.constructVTableMembers(targetClass, VTableMethods));
        this.appendTextToWrite("]\n\n");
        // insert target class virtual table string to the ll file
        this.flushToFile();
    }

    // fetch method info for each class throughout its inheritance tree
    // construct the corresponding virtual table strings and store them in target ll file
    void initializeVTables() throws Exception {

        // for each class, get all the functions in its inheritance tree
        for (Map.Entry<String,ClassInfo> classEntry : this.getSymbolTable().entrySet()) {

            // stack containing all the methods in current class inheritance tree
            LinkedList<MethodInfo> currentVTableMethods = new LinkedList<MethodInfo>();
            // current class 
            ClassInfo currentClass = classEntry.getValue();

            // initializing current class v-table and printing it into the target ll file
            if(!currentClass.isMain()) {
                this.initializeVTable(currentClass, currentVTableMethods);
            }else
            {   // class containing main function has an empty virtual table
                this.appendTextToWrite("@." + currentClass.getClassName() + "_vtable = global [0 x i8*] []\n\n");
            }
        }
    }

    // write auxiliarry ll functions into the target ll file
    void flushAuxiliarryMethods() throws Exception {
        String declarations = "declare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\ndeclare void @exit(i32)\n\n";
        String globals = "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n@_cNSZ = constant [15 x i8] c\"Negative size\\0a\\00\"\n";
        String printFunction = "define void @print_int(i32 %i) {\n\t%_str = bitcast [4 x i8]* @_cint to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n\tret void\n}\n\n";
        String errorFunction = "define void @throw_oob() {\n\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str)\n\tcall void @exit(i32 1)\n\tret void\n}\n\n";
        String negativeIndexFunction = "define void @throw_nsz() {\n\t%_str = bitcast [15 x i8]* @_cNSZ to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str)\n\tcall void @exit(i32 1)\n\tret void\n}\n\n";

        this.appendTextToWrite(declarations + globals + printFunction + errorFunction + negativeIndexFunction);
        this.flushToFile();
    }

    boolean isBasicLLVMType(String llVMType) {
        return (!llVMType.equals("i8*") && !llVMType.equals("i32*"));
    }

    // allocates and initializes local variables in stack
    // stores the string output in ll file
    void flushLocalVariables(Map<String, IdentifierInfo> localVariables) throws Exception {

        for (Map.Entry<String,IdentifierInfo> localVariableEntry : localVariables.entrySet()) {
            IdentifierInfo localVariable = localVariableEntry.getValue();
            String localVariableName = "%" + localVariable.getName();
            String localVariableType = this.toLLVMType(localVariable.getType());

            // allocate memory in stack 
            this.instructionToFile(localVariableName + " = alloca " + localVariableType);
            // initialize memory with 0
            // if(this.isBasicLLVMType(localVariableType)) {
            //     this.instructionToFile("store " + localVariableType + " 0, " + localVariableType + "* " + localVariableName);
            // }else{
            //     this.instructionToFile("store " + localVariableType + " null, " + localVariableType + "* " + localVariableName);
            // }
        }
        this.stringToFile("\n");
    }

        // allocates and initializes local variables in stack
    // stores the string output in ll file
    void flushLocalParameters(Map<String, IdentifierInfo> localParameters) throws Exception {

        for (Map.Entry<String,IdentifierInfo> localParameterEntry : localParameters.entrySet()) {
            IdentifierInfo localParameter = localParameterEntry.getValue();
            String localParameterName = localParameter.getName();
            String localParameterType = this.toLLVMType(localParameter.getType());

            // allocate memory in stack 
            this.instructionToFile("%" + localParameterName + " = alloca " + localParameterType);
            // initialize memory with 0
            this.instructionToFile("store " + localParameterType + " %." + localParameterName + ", " + localParameterType + "* %" + localParameterName);
        }
        this.stringToFile("\n");
    }

    boolean requestIdentifier(String request) {
        return (request.equals("identifier"));
    }

    // returns a string containing function definition 
    String constructLLArgumentList(Map<String, IdentifierInfo> methodArguments) {

        String argumentListString = "i8* %this";
        // for each argument print its type to a string
        for (Map.Entry<String,IdentifierInfo> methodArgumentEntry : methodArguments.entrySet()) {
            IdentifierInfo methodArgumentInfo = methodArgumentEntry.getValue();
            String methodArgumentName = "%." + methodArgumentInfo.getName();
            String methodArgumentType = this.toLLVMType(methodArgumentInfo.getType());

            argumentListString = argumentListString.concat(", " + methodArgumentType + " " + methodArgumentName);
        }

        return argumentListString;
    }

    // returns a string containing the types of function arguments seperated by commas
    String constructLLArgumentTypes(Map<String, IdentifierInfo> methodArguments) {
        String argumentTypesString = "";

        for (Map.Entry<String,IdentifierInfo> methodArgumentEntry : methodArguments.entrySet()) {
            IdentifierInfo methodArgumentInfo = methodArgumentEntry.getValue();
            String methodArgumentType = this.toLLVMType(methodArgumentInfo.getType());

            argumentTypesString = argumentTypesString.concat(", " + methodArgumentType);
        }

        return argumentTypesString;
    }

    // returns the size of the class in bytes
    // class size = last field offset + 8 bytes of virtual table
    int getClassSize(ClassInfo givenClass) {
        return (givenClass.getCurrentFieldOffset() + 8);
    }

    int getVTableSize(ClassInfo givenClass) {
        LinkedList<String> uniqueMethods = new LinkedList<String>();
        ClassInfo currentClass= givenClass;

        do{ //for each class in inheritance tree, add its unique methods
            Map<String, MethodInfo> currentMethodsInfo = currentClass.getMethodsInfoMap();

            for (Map.Entry<String,MethodInfo> currentMethodEntry : currentMethodsInfo.entrySet()) {
                MethodInfo currentMethod = currentMethodEntry.getValue();
                String currentMethodName = currentMethod.getName();

                // new method, add it to the list
                if(!uniqueMethods.contains(currentMethodName)) {
                    uniqueMethods.add(currentMethodName);
                }
            }
        }while((currentClass = this.getParent(currentClass.getClassName())) != null);

        return uniqueMethods.size();
    }

    // renames the register so it contains the type of the object contained
    String storeTypeInRegister(String objectType, String objectRegister) {

        String newRegisterHead = "<" + objectType + ">";
        String registerNumber;

        registerNumber = objectRegister.substring(2);
 
        return "%_" + newRegisterHead + registerNumber;
    }

    // checks if given type is object in llvm syntax
    boolean isLLVMObjectType(String givenType) {
        return (givenType.equals("i8*"));
    }

    // extracts the type of the object contained in the register
    // from its head
    String extractInstanceType(String instanceRegister) {
        int startIndex = instanceRegister.indexOf("<");
		int endIndex = instanceRegister.indexOf(">");

        return instanceRegister.substring(startIndex + 1, endIndex);
    }

    // extracts the actual register name from the object instance register name
    String extractInstanceRegister(String instanceRegister) {
        String temporaryString = instanceRegister;
        int registerNameStartIndex = temporaryString.lastIndexOf(">") + 1;

        return "%_" + instanceRegister.substring(registerNameStartIndex);
    }

    // returns a string containing argument types and names, seperated by commas
    String getArgumentDuplets(String objectRegister, LinkedList<String> registersList, String argumentTypes) {

        String dupletsString = "i8* " + objectRegister;

        if(argumentTypes.equals("")) {
            return dupletsString;
        }else{
            // throw away the first comma and white space
            argumentTypes = argumentTypes.substring(2);
        }

        List<String> typesList = Arrays.asList(argumentTypes.split(", "));
        Iterator<String> registersIterator = registersList.iterator();
        Iterator<String> typesIterator = typesList.iterator();

        // for each argument add its llvm type and name
        while(registersIterator.hasNext() && typesIterator.hasNext()){
            String currentRegister = registersIterator.next();
            String currentType = typesIterator.next();

            dupletsString = dupletsString.concat(", " + currentType + " " + currentRegister);
        }

        return dupletsString;
    }

    // found online, link: https://www.baeldung.com/java-check-string-number
    // checks if given string represents a number
    boolean isANumber(String givenString) {
        if (givenString == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(givenString);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    // checks if given string represents a register (starts with %)
    boolean isRegister(String givenEntity) {
        return (givenEntity.startsWith("%"));
    }

    boolean isObjectRegister(String givenRegister) {
        return (givenRegister.startsWith("%_<"));
    }


    String loadRegister(String entity, boolean returnTypeInfo, boolean returnValueRegister) throws Exception {

        // given entity can either be an integer or a boolean
        // returned as it is, no need for register, as no assignment will take place
        if(this.isANumber(entity)) {
            return entity;
        }

        // simply return the local pointer to the class
        if(entity.equals("this")){
            return "%this";
        }

        // given entity is a register containing an instance of a class
        if(this.isObjectRegister(entity)) {
            // return register with type metadata
            if(returnTypeInfo) {
                return entity;
            } // return only the register name
            else {
                return this.extractInstanceRegister(entity);
            }
        } 
        // given entiry is already a register, just return it
        if(this.isRegister(entity)) {
            return entity;
        }

        // entity is an identifier, construct a register
        IdentifierInfo givenIdentifier = this.fetchVariable(entity);
        String givenIdentifierType = givenIdentifier.getType();
        String givenIdentifierLLVMType = this.toLLVMType(givenIdentifierType);
        int givenIdentifierOffset = givenIdentifier.getOffset() + 8;
        String newReferenceRegister; 
        String newRegister;
        
        if(givenIdentifier.isField()) {
            String auxRegister = this.getNewRegister();
            // get reference to field to file
            this.instructionToFile(auxRegister + " = getelementptr i8, i8* %this, i32 " + givenIdentifierOffset);
            newReferenceRegister = this.getNewRegister();
            // bitcast field to its type
            this.instructionToFile(newReferenceRegister + " = bitcast i8* " + auxRegister + " to " + givenIdentifierLLVMType + "*");
        }else{ // entity is a local variable, get its reference from local register
            newReferenceRegister = "%" + entity;
        }

        // load the value pointed by the requested register
        if(returnValueRegister) {
            newRegister = this.getNewRegister();
            // load the value of the field to file
            this.instructionToFile(newRegister + " = load " + givenIdentifierLLVMType + ", " + givenIdentifierLLVMType + "* " + newReferenceRegister);
            // requested value is of an object type, include the info if requested
            if(this.isLLVMObjectType(givenIdentifierLLVMType) && returnTypeInfo){
                newRegister = this.storeTypeInRegister(givenIdentifierType,newRegister);
            }
            return newRegister;
        }

        // reference register returned by default
        return newReferenceRegister;
    }

    /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
    public String visit(Goal n, String argu) throws Exception {
        String _ret=null;
        // empty the contents of the file, overriting it
        PrintWriter myFileWriter = new PrintWriter(this.getFileToWrite());
        this.setFileWriter(myFileWriter);
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
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
    public String visit(MainClass n, String argu) throws Exception {
        String _ret=null;
        // initializing virtual tables for all classes
        this.initializeVTables();
        // flushing auxiliarry error methods into ll file
        this.flushAuxiliarryMethods();

        String mainClassName = n.f1.accept(this, "identifier");

        ClassInfo mainClassInfo = this.getClass(mainClassName);
        MethodInfo mainClassMethod = this.getClassMethod(mainClassName, "main");

        this.setTraversedClass(mainClassInfo);
        this.setTraversedMethod(mainClassMethod);

        this.stringToFile("define i32 @main() {\n");

        // allocate memory in stack for local variables and initialize their registers
        // store the result in the ll file
        this.flushLocalVariables(mainClassMethod.getVariables());
        
        // flush main function statements
        n.f15.accept(this, argu);
        this.instructionToFile("ret i32 0");
        this.stringToFile("}\n\n");

        return _ret;
    }

    /**
     * f0 -> ClassDeclaration()
    *       | ClassExtendsDeclaration()
    */
    public String visit(TypeDeclaration n, String argu) throws Exception {
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
    public String visit(ClassDeclaration n, String argu) throws Exception {
        String _ret=null;
        // traversing a new class
        String currentClassName = n.f1.accept(this, "identifier");
        this.setTraversedClass(this.getClass(currentClassName));

        // flush class method declarations into the ll file
        n.f4.accept(this, argu);
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
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
        String _ret=null;
        // traversing a new class
        String currentClassName = n.f1.accept(this, "identifier");
        this.setTraversedClass(this.getClass(currentClassName));

        // flush class method declarations into the ll file
        n.f6.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public String visit(VarDeclaration n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
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
    public String visit(MethodDeclaration n, String argu) throws Exception {
        String _ret=null;

        String currentMethodName = n.f2.accept(this, "identifier");
        ClassInfo currentClassInfo = this.getTraversedClass();
        String currentClassName = currentClassInfo.getClassName();

        // enter new method
        MethodInfo currentMethodInfo = this.getClassMethod(currentClassName, currentMethodName);
        this.setTraversedMethod(currentMethodInfo);
        // new space of register and labels names
        this.enterScope();

        String currentMethodType = this.toLLVMType(currentMethodInfo.getType());
        // storing the output value or register of the returned expression
        String returnedExpression;

        // flush method definition head
        String llArgumentsList = this.constructLLArgumentList(currentMethodInfo.getArguments());
        this.stringToFile("define " + currentMethodType + " @" + currentClassName + "." + currentMethodName + "(" + llArgumentsList + "){\n");
        // allocate memory for arguments and local variables, flush string to file
        this.flushLocalVariables(currentMethodInfo.getVariables());
        this.flushLocalParameters(currentMethodInfo.getArguments());

        // flush method statements to ll file
        n.f8.accept(this, argu);
        // get the expression to return
        returnedExpression = this.loadRegister(n.f10.accept(this, argu), false, true);
        this.instructionToFile("ret " + currentMethodType + " " + returnedExpression);
        this.stringToFile("}\n\n");
        return _ret;
    }

    /**
     * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
    public String visit(FormalParameterList n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(FormalParameter n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
    */
    public String visit(FormalParameterTail n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> ","
    * f1 -> FormalParameter()
    */
    public String visit(FormalParameterTerm n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
    public String visit(Type n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(ArrayType n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "boolean"
    */
    public String visit(BooleanType n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "int"
    */
    public String visit(IntegerType n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | IfStatement()
    *       | WhileStatement()
    *       | PrintStatement()
    */
    public String visit(Statement n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
    public String visit(Block n, String argu) throws Exception {
        String _ret=null;
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public String visit(AssignmentStatement n, String argu) throws Exception {
        this.instructionToFile("");
        String _ret=null;
        String identifierRegister = n.f0.accept(this, argu);
        String expressionRegister = n.f2.accept(this, argu);

        IdentifierInfo currrentIdentifierInfo = this.fetchVariable(identifierRegister);
        String currentIdentifierType = this.toLLVMType(currrentIdentifierInfo.getType());

        // get the reference to the entity with given identifier
        String toAssignRegister = this.loadRegister(identifierRegister,false,false);

        String assignedRegister = this.loadRegister(expressionRegister,false,true);

        this.instructionToFile("store " + currentIdentifierType + " " + assignedRegister + ", " + currentIdentifierType + "* " + toAssignRegister);

        return _ret;
    }

    /**
     * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
        this.instructionToFile("");
        String _ret=null;

        // registers
        String arrayRegister = this.loadRegister(n.f0.accept(this, argu),false,true);
        String indexRegister = this.loadRegister(n.f2.accept(this, argu),false,true);
        String valueToAssignRegister;
        String arraySizeRegister = this.getNewRegister();
        String indexBoundComparisonRegister = this.getNewRegister();
        String incrementedIndexRegister = this.getNewRegister();
        String targetToAssignReferenceRegister = this.getNewRegister();

        // labels
        String inBound = "boundCheck" + this.getNewIfLabel();
        String outBound = "boundCheck" + this.getNewIfLabel();

        // extract array size to file
        this.instructionToFile(arraySizeRegister + " = load i32, i32* " + arrayRegister);
        // compare index and bound to file
        this.instructionToFile(indexBoundComparisonRegister + " = icmp ult i32 " + indexRegister + ", " + arraySizeRegister);
        // jump to resulting label to file
        this.instructionToFile("br i1 " + indexBoundComparisonRegister + ", label %" + inBound + ", label %" + outBound);
                
        // out of bound jump to file
        this.instructionToFile("\n" + outBound + ":");
        this.instructionToFile("call void @throw_oob()");
        this.instructionToFile("br label %" + inBound);
        
        // in bound jump to file
        this.instructionToFile("\n" + inBound + ":");
        // increment index to ommit first element, aka array size to file
        this.instructionToFile(incrementedIndexRegister + " = add i32 " + indexRegister + ", 1");
        // get reference to value to store in to file
        this.instructionToFile(targetToAssignReferenceRegister + " = getelementptr i32, i32*  " + arrayRegister + ", i32 " + incrementedIndexRegister);
        valueToAssignRegister = this.loadRegister(n.f5.accept(this, argu), false, true);
        // assign value to array index to file
        this.instructionToFile("store i32 " + valueToAssignRegister + ", i32* " + targetToAssignReferenceRegister);

        return _ret;
    }

    /**
     * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    public String visit(IfStatement n, String argu) throws Exception {
        this.instructionToFile("");
        String _ret=null;

        // labels
        String thenLabel = "ifStatement" + this.getNewIfLabel();
        String elseLabel = "ifStatement" + this.getNewIfLabel();
        String outIfLabel = "ifStatement" + this.getNewIfLabel();
        // register
        String conditionRegister = this.loadRegister(n.f2.accept(this, argu), false, true);

        // condition check to file
        this.instructionToFile("br i1 " + conditionRegister + ", label %" + thenLabel + ", label %" + elseLabel);
        // then label to file
        this.instructionToFile("\n" + thenLabel + ":");
        n.f4.accept(this, argu);
        this.instructionToFile("br label %" + outIfLabel);
        // then label to file
        this.instructionToFile("\n" + elseLabel + ":");
        n.f6.accept(this, argu);
        this.instructionToFile("br label %" + outIfLabel);
        // after if statement to file
        this.instructionToFile("\n" + outIfLabel + ":");

        return _ret;
    }

    /**
     * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public String visit(WhileStatement n, String argu) throws Exception {
        this.instructionToFile("");
        String _ret=null;

        // labels
        String checkCondition = "whileStatement" + this.getNewLoopLabel();
        String whileBody = "whileStatement" + this.getNewLoopLabel();
        String outWhile = "whileStatement" + this.getNewLoopLabel();

        this.instructionToFile("br label %"+ checkCondition);
        this.instructionToFile("\n" + checkCondition + ":");
        String newConditionRegister = this.loadRegister(n.f2.accept(this, argu), false, true);
        this.instructionToFile("br i1 " + newConditionRegister + ", label %" + whileBody + ", label %" + outWhile);

        // while body to file
        this.instructionToFile("\n" + whileBody + ":");
        n.f4.accept(this, argu);
        this.instructionToFile("br label %"+ checkCondition);

        // out of while body to file
        this.instructionToFile("\n" + outWhile + ":");

        return _ret;
    }

    /**
     * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public String visit(PrintStatement n, String argu) throws Exception {
        this.instructionToFile("");
        String _ret=null;

        String toPrintRegister = this.loadRegister(n.f2.accept(this, argu), false, true);
        this.instructionToFile("call void (i32) @print_int(i32 " + toPrintRegister +")");

        return _ret;
    }

    /**
     * f0 -> AndExpression()
    *       | CompareExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup()
    *       | ArrayLength()
    *       | MessageSend()
    *       | Clause()
    */
    public String visit(Expression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
    public String visit(AndExpression n, String argu) throws Exception {
        this.instructionToFile("");
        // registers
        String firstClauseRegister;
        String secondClauseRegister;
        String endResultRegister = this.getNewRegister();
        // labels
        String  falseFirstClause = "andExpr" + getNewIfLabel();
        String  checkSecondClause = "andExpr" + getNewIfLabel();
        String  passingClause = "andExpr" + getNewIfLabel();
        String  calculateAndValue = "andExpr" + getNewIfLabel();

        // load first clause value to file
        firstClauseRegister = this.loadRegister(n.f0.accept(this, argu), false, true);

        // check if skip or jump to second clause to file
        this.instructionToFile("br i1 " + firstClauseRegister + ", label %" + checkSecondClause + ", label %" + falseFirstClause);

        // jump to false first clause label to file
        this.instructionToFile("\n" + falseFirstClause + ":");
        this.instructionToFile("br label %" + calculateAndValue);

        // jump to second check to file
        this.instructionToFile("\n" + checkSecondClause + ":");
        secondClauseRegister = this.loadRegister(n.f2.accept(this, argu), false, true);
        this.instructionToFile("br label %" + passingClause);

        // jump to passing label to file
        this.instructionToFile("\n" + passingClause + ":");
        this.instructionToFile("br label %" + calculateAndValue);
        
        // evaluate and to file
        this.instructionToFile("\n" + calculateAndValue + ":");
        // initially false, if second clause reached, and takes its value
        this.instructionToFile(endResultRegister + " = phi i1 [ 0, %" + falseFirstClause + " ] , [ " + secondClauseRegister + ", %" + passingClause + " ]");

        return endResultRegister;
    }


    /**
     * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public String visit(CompareExpression n, String argu) throws Exception {
        this.instructionToFile("");
        // registers
        String firstFactorRegister = this.loadRegister(n.f0.accept(this, null), false, true);
        String secondFactorRegister = this.loadRegister(n.f2.accept(this, null), false, true);
        String compareResultRegister = this.getNewRegister();
        
        this.instructionToFile(compareResultRegister + " = icmp slt i32 " + firstFactorRegister + ", " + secondFactorRegister);

        return compareResultRegister;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public String visit(PlusExpression n, String argu) throws Exception {
        this.instructionToFile("");
        // registers
        String firstFactorRegister = this.loadRegister(n.f0.accept(this, null), false, true);
        String secondFactorRegister = this.loadRegister(n.f2.accept(this, null), false, true);
        String addResultRegister = this.getNewRegister();
        
        this.instructionToFile(addResultRegister + " = add i32 " + firstFactorRegister + ", " + secondFactorRegister);

        return addResultRegister;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public String visit(MinusExpression n, String argu) throws Exception {
        this.instructionToFile("");
        // registers
        String firstFactorRegister = this.loadRegister(n.f0.accept(this, null), false, true);
        String secondFactorRegister = this.loadRegister(n.f2.accept(this, null), false, true);
        String minusResultRegister = this.getNewRegister();
        
        this.instructionToFile(minusResultRegister + " = sub i32 " + firstFactorRegister + ", " + secondFactorRegister);

        return minusResultRegister;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public String visit(TimesExpression n, String argu) throws Exception {
        this.instructionToFile("");
        // registers
        String firstFactorRegister = this.loadRegister(n.f0.accept(this, null), false, true);
        String secondFactorRegister = this.loadRegister(n.f2.accept(this, null), false, true);
        String timesResultRegister = this.getNewRegister();
        
        this.instructionToFile(timesResultRegister + " = mul i32 " + firstFactorRegister + ", " + secondFactorRegister);

        return timesResultRegister;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n, String argu) throws Exception {
        this.instructionToFile("");
        String arrayExpression = n.f0.accept(this, argu);
        String indexExpression = n.f2.accept(this, argu);

        // registers
        String arrayRegister = this.loadRegister(arrayExpression, false, true);
        String indexRegister = this.loadRegister(indexExpression, false, true);
        String firstElementReferenceRegister = this.getNewRegister();
        String firstElementRegister = this.getNewRegister();
        String indexBoundComparisonRegister = this.getNewRegister();
        String actualFirstElementIndexRegister = this.getNewRegister();
        String targetValueReferenceRegister = this.getNewRegister(); 
        String targetValueRegister = this.getNewRegister();

        // labels
        String inBoundLabel = "arrayLookup" + this.getNewIfLabel();
        String outBoundLabel = "arrayLookup" + this.getNewIfLabel();
        String outCheckLabel = "arrayLookup" + this.getNewIfLabel();

        // get first element reference to file
        this.instructionToFile(firstElementReferenceRegister + " = getelementptr i32, i32* " + arrayRegister + ", i32 0");
        // get first element to file
        this.instructionToFile(firstElementRegister + " = load i32, i32* " + firstElementReferenceRegister);
        // size index comparison to file
        this.instructionToFile(indexBoundComparisonRegister + " = icmp ult i32 " + indexRegister + ", " + firstElementRegister);
        // label jump to file
        this.instructionToFile("br i1 " + indexBoundComparisonRegister + ", label %" + inBoundLabel + ", label %" + outBoundLabel);
        // out of bounds label to file
        this.instructionToFile("\n" + outBoundLabel + ":");
        this.instructionToFile("call void @throw_oob()");
        this.instructionToFile("br label %" + outCheckLabel);
        // in bounds label to file
        this.instructionToFile("\n" + inBoundLabel + ":");
        // add one to the requested index in order
        // as the first element of the array contains its size
        this.instructionToFile(actualFirstElementIndexRegister + " = add i32 " + indexRegister + ", 1");
        // reference to target index value to file
        this.instructionToFile(targetValueReferenceRegister + " = getelementptr i32, i32*  " + arrayRegister + ", i32 " + actualFirstElementIndexRegister + "\n");
        // actual index value to file
        this.instructionToFile(targetValueRegister + " = load i32, i32* " + targetValueReferenceRegister);
        this.instructionToFile("br label %" + outCheckLabel);
        // out of bound check to file
        this.instructionToFile("\n" + outCheckLabel + ":");
        return targetValueRegister;
    }
    
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public String visit(ArrayLength n, String argu) throws Exception {
        this.instructionToFile("");
        String expressionOutput = n.f0.accept(this, argu);
        // registers
        String arrayRegister = this.loadRegister(expressionOutput, false, true);
        String firstElementReferenceRegister = this.getNewRegister();
        String firstElement = this.getNewRegister();

        // first element reference to file
        this.instructionToFile(firstElementReferenceRegister + " = getelementptr i32, i32* " + arrayRegister + ", i32 0");
        // first element to file
        this.instructionToFile(firstElement + " = load i32, i32* " + firstElementReferenceRegister);

        return firstElement;
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    public String visit(MessageSend n, String argu) throws Exception {
        this.instructionToFile("");
        String instanceExpression = n.f0.accept(this, argu);
        // registers
        String instanceRegister = this.loadRegister(instanceExpression, true, true);
        String instanceVTableRegister = this.getNewRegister();
        String vTableRegister = this.getNewRegister();
        String methodPointerReferenceRegister = this.getNewRegister();
        String methodPointerRegister = this.getNewRegister();
        String castedMethodPointerRegister = this.getNewRegister();
        String methodCallRegister = this.getNewRegister();

        // extract information about current class instance
        String classInstanceType;
        if(instanceRegister.equals("%this")) {
            classInstanceType = this.getTraversedClass().getClassName();
        }else{
            classInstanceType = this.extractInstanceType(instanceRegister);
            instanceRegister = this.extractInstanceRegister(instanceRegister);
        }

        // get the name of the called method
        String calledMethodName = n.f2.accept(this, argu);
        MethodInfo calledMethodInfo = this.fetchClassFunction(classInstanceType, calledMethodName);
        int calledMethodOffset = calledMethodInfo.getOffset() / 8;
        String calledMethodJavaType = calledMethodInfo.getType();
        String calledMethodType = this.toLLVMType(calledMethodJavaType);

        // pointer to virtual table to file 
        this.instructionToFile(instanceVTableRegister + " = bitcast i8* " + instanceRegister + " to i8***");
        // virtual table cast to file
        this.instructionToFile(vTableRegister + " = load i8**, i8*** " + instanceVTableRegister);
        // extract method pointer reference to file
        this.instructionToFile(methodPointerReferenceRegister + " = getelementptr i8*, i8** " + vTableRegister + ", i32 " + calledMethodOffset);
        // extract method pointer to file
        this.instructionToFile(methodPointerRegister + " = load i8*, i8** " + methodPointerReferenceRegister);
        // cast method pointer to file
        String argumentTypes = constructLLArgumentTypes(calledMethodInfo.getArguments());
        this.instructionToFile(castedMethodPointerRegister + " = bitcast i8* " + methodPointerRegister + " to " + calledMethodType + " (i8*" + argumentTypes + ")*");
        
        // gather called function parameters register names
        this.initializeArgumentGathering();
        n.f4.accept(this, argu);
        String argumentTypeNameDuplets = this.getArgumentDuplets(instanceRegister, this.getArgumentRegisters().peekLast(), argumentTypes);
        // function call to file
        this.instructionToFile(methodCallRegister + " = call " + calledMethodType + " " + castedMethodPointerRegister + "(" + argumentTypeNameDuplets + ")");
        this.terminateArgumentGathering();

        // if an object is returned, contain its type info
        if(this.isLLVMObjectType(calledMethodType)) {
            methodCallRegister = this.storeTypeInRegister(calledMethodJavaType,methodCallRegister);
        }

        // return the output of the function call
        return methodCallRegister;
    }

    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public String visit(ExpressionList n, String argu) throws Exception {
        String _ret=null;
        String newArgumentRegister = this.loadRegister(n.f0.accept(this, argu), false, true);
        // add the register of the new argument to the list of arguments of current function call
        this.addNewArgument(newArgumentRegister);
        // gather the rest of the arguments
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
    * f0 -> ( ExpressionTerm() )*
    */
    public String visit(ExpressionTail n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> ","
    * f1 -> Expression()
    */
    public String visit(ExpressionTerm n, String argu) throws Exception {
        String _ret=null;
        String newArgumentRegister = this.loadRegister(n.f1.accept(this, argu), false, true);
        // add the register of the new argument to the list of arguments of current function call
        this.addNewArgument(newArgumentRegister);
        return _ret;
    }

    /**
     * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
    public String visit(Clause n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | BracketExpression()
    */
    public String visit(PrimaryExpression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n, String argu) throws Exception {
        return n.f0.tokenImage;
    }

    /**
     * f0 -> "true"
    */
    public String visit(TrueLiteral n, String argu) throws Exception {
        return "1";
    }

    /**
     * f0 -> "false"
    */
    public String visit(FalseLiteral n, String argu) throws Exception {
        // false value is represented by a zero
        return "0";
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n, String argu) throws Exception {
            return n.f0.toString();
    }

    /**
     * f0 -> "this"
    */
    public String visit(ThisExpression n, String argu) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(ArrayAllocationExpression n, String argu) throws Exception {
        this.instructionToFile("");
        String ExpressionResult = n.f3.accept(this, argu);

        // registers
        String indexRegister = this.loadRegister(ExpressionResult, false, true);
        String arrayPointerRegister = this.getNewRegister();
        String oneCompRegister = this.getNewRegister();
        String incrementedIndexRegister = this.getNewRegister();
        String arrayBytePointerRegister = this.getNewRegister();


        // index negativity check labels
        String negativeIndexLabel = "arrayAlloc" + this.getNewIfLabel();
        String positiveIndexLabel = "arrayAlloc" + this.getNewIfLabel();

        // index 1 comparison to file
        this.instructionToFile(oneCompRegister + "  = icmp slt i32 " + indexRegister + ", 1");
        // label choice to file
        this.instructionToFile("br i1 " + oneCompRegister + ", label %" + negativeIndexLabel + ", label %" + positiveIndexLabel);
        // negative index label case to file
        this.instructionToFile("\n" + negativeIndexLabel + ":");
        this.instructionToFile("call void @throw_oob()");
        this.instructionToFile("br label %" + positiveIndexLabel);
        // positive index label to file
        this.instructionToFile("\n" + positiveIndexLabel + ":");
        // increment index to file
        this.instructionToFile(incrementedIndexRegister + " = add i32 " + indexRegister + ", 1");
        // array memory allocation to file
        this.instructionToFile(arrayBytePointerRegister + " = call i8* @calloc(i32 " + incrementedIndexRegister + ", i32 4)");
        this.instructionToFile("\n\t" + arrayPointerRegister + " = bitcast i8* " + arrayBytePointerRegister + " to i32*");
        // store array size as the first element in array to file
        this.instructionToFile("store i32 " + indexRegister + ", i32* " + arrayPointerRegister + "\n");

        return arrayPointerRegister;
    }

    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n, String argu) throws Exception {
        this.instructionToFile("");
        String newClassName = n.f1.accept(this, null);
        ClassInfo newClassInfo = this.getClass(newClassName);
        // get info about the size of the new instance
        // and the size of its class virtual table
        int newClassSize = this.getClassSize(newClassInfo);
        int newVTableSize = this.getVTableSize(newClassInfo);
        // register in which the new instance pointer
        String newInstanceRegister = this.getNewRegister();
        String bitcastRegister = this.getNewRegister();
        String vTableRegister = this.getNewRegister();

        // memory allocation to file
        this.instructionToFile(newInstanceRegister + " = call i8* @calloc(i32 1, i32 " + newClassSize + ")");
        // bitcast to file
        this.instructionToFile(bitcastRegister + " = bitcast i8* " + newInstanceRegister + " to i8***");
        // store vtable in head of the instance to file
        this.instructionToFile(vTableRegister + " = getelementptr[" + newVTableSize + " x i8*], [" + newVTableSize + " x i8*]* @." + newClassName + "_vtable, i32 0, i32 0");
        this.instructionToFile("store i8** " + vTableRegister + ", i8*** " + bitcastRegister);

        // store information about object pointer type in the register name
        return this.storeTypeInRegister(newClassName, newInstanceRegister);
    }

    /**
    * f0 -> "!"
    * f1 -> Clause()
    */
    public String visit(NotExpression n, String argu) throws Exception {
        String clauseResult = n.f1.accept(this, argu);
        String clauseRegister = this.loadRegister(clauseResult, false, true);

        String negatedClauseRegister = this.getNewRegister();
        // store the negation expression in the new register
        this.instructionToFile(negatedClauseRegister + " = xor i1 " + clauseRegister + ", 1");

        return negatedClauseRegister;
    }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    public String visit(BracketExpression n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }   
}
