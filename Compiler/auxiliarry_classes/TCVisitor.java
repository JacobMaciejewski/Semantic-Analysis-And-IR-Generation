package auxiliarry_classes;

import syntaxtree.*;
import visitor.*;
import java.util.*;

// Type Checking Visitor is the second visitor to traverse the abstact syntax tree
// Its main goal is to type check all identifiers and expressions
public class TCVisitor extends GJDepthFirst<String, Void>{
    // mapping from class name to its info
    private Map<String, ClassInfo> symbolTable;
    // the class whose body we are currently traversing
    private ClassInfo traversedClass;
    // the method whose body we are currently traversing
    private MethodInfo traversedMethod;
    // checking parameters
    private boolean traversingParameters;
    // to store function call arguments
    private LinkedList<LinkedList<String>> argumentTypes;
    // tells whether to return identifier type or literal
    private boolean identifierAsString;


    public TCVisitor(Map<String, ClassInfo> symbolTable) {
        this.setSymbolTable(symbolTable);
        this.setTraversedClass(null);
        this.setTraversedMethod(null);
        this.setTraversingParameters(false);
        this.setArgumentTypes(null);
        this.setIdentifierAsString(false);
        LinkedList<LinkedList<String>> argumentTypesList = new LinkedList<LinkedList<String>>();
        this.setArgumentTypes(argumentTypesList);
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

    void setTraversingParameters(boolean status) {
        this.traversingParameters = status;
    }

    void setArgumentTypes(LinkedList<LinkedList<String>> argumentTypes){
        this.argumentTypes = argumentTypes;
    }

    void setIdentifierAsString(boolean status) {
        this.identifierAsString = status;
    }

    void startArgumentTypeGathering() {
        LinkedList<String> newArgumentTypes = new LinkedList<String>();
        this.getArgumentTypes().addFirst(newArgumentTypes);
    }

    void addArgumentType(String argumentType) {
        this.getArgumentTypes().getFirst().add(argumentType);
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

    LinkedList<LinkedList<String>> getArgumentTypes() {
        return this.argumentTypes;
    }

    LinkedList<String> getCurrentArgumentTypes() {
        LinkedList<String> currentArgumentTypes = this.getArgumentTypes().removeFirst();

        return currentArgumentTypes;
    }

    // checks if identifier should be returned as its value or as literal
    boolean identifierAsString() {
        return this.identifierAsString;
    }

    // checks if given type is a basic type (int/int[]/boolean)
    boolean isBasicType(String givenType) {
        return (givenType.equals("int") || givenType.equals("boolean") || givenType.equals("int[]"));
    }

    // checks if given type is user defined type (class type)
    boolean isComplexType(String givenType) {
        return this.getSymbolTable().containsKey(givenType);
    }

    // given type is basic or complex type
    boolean isValidType(String givenType) {
        return (this.isBasicType(givenType) || this.isComplexType(givenType));
    }

    boolean traversingParameters() {
        return this.traversingParameters;
    }

    boolean traversingMethod() {
        return (this.getTraversedMethod() != null);
    }

    // checks if two types are the same
    // in the case of classes, checks if first argument
    // inherits from second one
    boolean isOfType(String givenType, String targetType) {

        if(givenType == null) { // invalid type
            return false;
        }

        if(this.isBasicType(givenType)) { // target type is basic, simple equivalence of names suffices
            return (targetType.equals(givenType));
        }else{ //target type is a class, check if given type extends it

            return this.inheritsFrom(givenType, targetType);
        }
    }

    // checks if given identifiers (fields, parameters, local variables) refer to a valid type
    // throws dedicated error message for each case
    void checkIdentifiersInvalidType(Map<String, IdentifierInfo> identifiersList) throws Exception{

        String traversedClassName = this.getTraversedClass().getClassName();

        for (Map.Entry<String,IdentifierInfo> identifierEntry : identifiersList.entrySet()) {

            IdentifierInfo currentIdentifier = identifierEntry.getValue();
            String currentIdentifierName = currentIdentifier.getName();
            String currentIdentifierType = currentIdentifier.getType();

            // type of current identifier is invalid, throw error
            if(!this.isValidType(currentIdentifierType)) {

                // in method definition
                if(this.traversingMethod()) {
                    String currentlyTraversedMethodName = this.getTraversedMethod().getName();

                    // in parameters list (traversing parameters)
                    if(this.traversingParameters()){
                        throw new Exception("Class " + traversedClassName + " - Method " + currentlyTraversedMethodName + " : Parameter " + currentIdentifierName + " is of invalid type");
                    }else{ //in method body (traversing local variables)
                        throw new Exception("Class " + traversedClassName + " - Method " + currentlyTraversedMethodName + " : Local variable " + currentIdentifierName + " is of invalid type");
                    }
                }else{ // in class body (traversing fields)
                    throw new Exception("Class " + traversedClassName + " : Field " + currentIdentifierName + " is of invalid type");
                }
            }
        }
    }


    void checkArgumentTypes(MethodInfo truthFunction, List<String> argumentTypes) throws Exception{

        Iterator<Map.Entry<String, IdentifierInfo>>  functionParameters = truthFunction.getArguments().entrySet().iterator();
        Iterator<String> functionArguments = argumentTypes.iterator();
        Integer counter = 1;

        while(functionParameters.hasNext() && functionArguments.hasNext()) {
            String currentParameterType = functionParameters.next().getValue().getType();
            String currentArgumentType = functionArguments.next();
            // argument parameter type missmatch
            if(!this.isOfType(currentArgumentType, currentParameterType)) {
                throw new Exception(this.getExceptionHead() + counter.toString() + "th argument type missmatch in " + truthFunction.getName() + " function call");
            }
            counter++;
        }

        // check if there is a mismatch of number of arguments and parameters
        if(functionParameters.hasNext() || functionArguments.hasNext()) {
            throw new Exception(this.getExceptionHead() + "Argument-Parameter number missmatch in function " + truthFunction.getName() + " call");
        }
    }

    // check if function has a valid type
    void checkMethodInvalidType(MethodInfo currentMethod) throws Exception {

        if(!this.isValidType(currentMethod.getType())) {
            String currentClassName = this.getTraversedClass().getClassName();
            String currentMethodName = currentMethod.getName();

            throw new Exception("Class " + currentClassName + " : Method " + currentMethodName + " is of invalid type");
        }
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


    // returns the "Class <class name> - Method <method name> : " literal
    String getExceptionHead() {
        String currentClassName = this.getTraversedClass().getClassName();
        String currentMethodName = this.getTraversedMethod().getName();

        String exceptionHead = "Class " + currentClassName + " - Method " + currentMethodName + " : ";

        return exceptionHead;
    }


    // checks if first argument type inherits from the second one
    boolean inheritsFrom(String givenType, String targetType) {

        ClassInfo givenClass = this.getClass(givenType);
        ClassInfo targetClass = this.getClass(targetType);

        // if one of the types is not a class
        if((givenClass == null) || (targetClass == null)) {
            return false;
        }

        ClassInfo currentClass = givenClass;

        do // check all parent classes, for requested type 

            // found parent class with requested type
            if(targetType.equals(currentClass.getClassName())) {
                return true;
            }

        // while we are not in a base class
        while((currentClass = this.getParent(currentClass.getClassName())) != null); 


        return false;
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

    /* f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
    public String visit(Goal n, Void argu) throws Exception {
        String _ret=null;
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
    public String visit(MainClass n, Void argu) throws Exception {
        String _ret=null;
        this.setIdentifierAsString(true);
        String mainClassName = n.f1.accept(this, argu);
        String mainFunctionName = "main";
        this.setIdentifierAsString(false);

        MethodInfo mainFunction = this.getClassMethod(mainClassName, mainFunctionName);
        Map<String, IdentifierInfo> mainFunctionVariables = mainFunction.getVariables();

        this.setTraversedClass(this.getClass(mainClassName));
        this.setTraversedMethod(mainFunction);
        // check if main function variables are of valid type
        this.checkIdentifiersInvalidType(mainFunctionVariables);

        // check main function statements
        n.f15.accept(this, argu);

        return _ret;
    }

    /**
     * f0 -> ClassDeclaration()
    *       | ClassExtendsDeclaration()
    */
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
    public String visit(ClassDeclaration n, Void argu) throws Exception {
        String _ret=null;
        // get class name as literal
        this.setIdentifierAsString(true);
        String currentClassName = n.f1.accept(this, argu);
        this.setIdentifierAsString(false);
        // get current class fields
        Map<String, IdentifierInfo> currentClassFields = this.getClassFields(currentClassName);

        // traversing current class
        this.setTraversedClass(this.getClass(currentClassName));

        // check if class fields are of valid type
        this.checkIdentifiersInvalidType(currentClassFields);

        // making proper type checking for class functions
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
    public String visit(ClassExtendsDeclaration n, Void argu) throws Exception {
        String _ret=null;
        // get class name as literal
        this.setIdentifierAsString(true);
        String currentClassName = n.f1.accept(this, argu);
        this.setIdentifierAsString(false);
        // get current class fields
        Map<String, IdentifierInfo> currentClassFields = this.getClassFields(currentClassName);

        // traversing current class
        this.setTraversedClass(this.getClass(currentClassName));

        // check if class fields are of valid type
        this.checkIdentifiersInvalidType(currentClassFields);

        // making proper type checking for class functions
        n.f4.accept(this, argu);

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
    public String visit(MethodDeclaration n, Void argu) throws Exception {
        String _ret=null;
        // get current method name as literal
        this.setIdentifierAsString(true);
        String currentMethodName = n.f2.accept(this, argu);
        this.setIdentifierAsString(false);
        MethodInfo currentMethod = this.getClassMethod(this.getTraversedClass().getClassName(), currentMethodName);

        // traversing current method
        this.setTraversedMethod(currentMethod);

        // check if method is of invalid type
        this.checkMethodInvalidType(currentMethod);

        // check if arguments are of invalid type
        this.setTraversingParameters(true);
        this.checkIdentifiersInvalidType(currentMethod.getArguments());
        this.setTraversingParameters(false);

        // check if local variables are of invalid type
        this.checkIdentifiersInvalidType(currentMethod.getVariables());

        // type check method statements
        n.f8.accept(this, argu);

        // get return type and compare it to function defined type
        String returnType = n.f10.accept(this, argu);

        if(!this.isOfType(returnType, currentMethod.getType())) {
            throw new Exception(this.getExceptionHead() + "Return/Function type missmatch");
        }

        return _ret;
    }

    /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
    public String visit(Type n, Void argu) throws Exception {
        String typeLiteral;
        this.setIdentifierAsString(true);
        typeLiteral = n.f0.accept(this, argu);
        this.setIdentifierAsString(false);

        return typeLiteral;
    }

    /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(ArrayType n, Void argu) throws Exception {
        return "int[]";
    }

    /**
     * f0 -> "boolean"
    */
    public String visit(BooleanType n, Void argu) throws Exception {
        return "boolean";
    }

    /**
    * f0 -> "int"
    */
    public String visit(IntegerType n, Void argu) throws Exception {
        return "int";
    }  

   /**
    * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n, Void argu) throws Exception {
        String identifier = n.f0.toString();

        // return identifier as string
        if(this.identifierAsString()) {
            return identifier;
        }else // find an entity with the given name and return its type
        {
            IdentifierInfo identifierData = this.fetchVariable(identifier);

            if(identifierData == null) { // no entity with given identifier

                ClassInfo classWithIdentifierName = this.getClass(identifier);

                if(classWithIdentifierName == null) { // no class with identifier as its name
                    return null;
                }else
                {
                    return identifier;
                }

            }else
            {
                return identifierData.getType();
            }
        }
    }

    /**
    * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n, Void argu) throws Exception {
        return "int";
    }

    /**
     * f0 -> "true"
    */
    public String visit(TrueLiteral n, Void argu) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> "false"
    */
    public String visit(FalseLiteral n, Void argu) throws Exception {
        return "boolean";
    } 

    /**
    * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | IfStatement()
    *       | WhileStatement()
    *       | PrintStatement()
    */
    public String visit(Statement n, Void argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
    public String visit(Block n, Void argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public String visit(AssignmentStatement n, Void argu) throws Exception {
        String _ret=null;
        // get variable name as literal
        this.setIdentifierAsString(true);
        String variableName = n.f0.accept(this, argu);
        this.setIdentifierAsString(false);

        IdentifierInfo variableInfo = this.fetchVariable(variableName);

        // check if assigned identifier is undefined
        if(variableInfo == null) {
            throw new Exception(this.getExceptionHead() + "Variable " +  variableName + " to be assigned is undefined");
        }

        String variableType = variableInfo.getType();
        String expressionType = n.f2.accept(this, argu);

        // check if expression has different type from assigned variable
        if(!isOfType(expressionType, variableType)) {
            throw new Exception(this.getExceptionHead() + "Expression of invalid type to be assigned to variable " +  variableName);
        }

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
    public String visit(ArrayAssignmentStatement n, Void argu) throws Exception {
        String _ret=null;

        // get variable name as literal
        this.setIdentifierAsString(true);
        String variableName = n.f0.accept(this, argu);
        this.setIdentifierAsString(false);

        IdentifierInfo variableInfo = this.fetchVariable(variableName);

        // check if assigned array is undefined
        if(variableInfo == null) {
            throw new Exception(this.getExceptionHead() + "Array " + variableName  + " to be assigned is undefined");
        }

        // check if assigned array is of type int[]
        if(!this.isOfType(variableInfo.getType(), "int[]")) {
            throw new Exception(this.getExceptionHead() + "Assignment to a non integer array " + variableName);
        }

        String indexExpressionType = n.f2.accept(this, argu);

        // check if array indexing is of type int
        if(!this.isOfType(indexExpressionType, "int")) {
            throw new Exception(this.getExceptionHead() + "Index of array " + variableName + " is not of type int");
        }

        String assignExpressionType = n.f5.accept(this, argu);

        // check if value to be assigned to the array is of type int
        if(!this.isOfType(assignExpressionType, "int")) {
            throw new Exception(this.getExceptionHead() + "Value assigned to array " + variableName + " is not of type int");
        }

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
    public String visit(IfStatement n, Void argu) throws Exception {
        String _ret=null;

        String conditionType = n.f2.accept(this, argu);

        // check if condition is a boolean
        if(!this.isOfType(conditionType, "boolean")) {
            throw new Exception(this.getExceptionHead() + "If condition is not of type boolean");
        }

        // execute nested statements
        n.f4.accept(this, argu);
        n.f6.accept(this, argu);

        return _ret;
    }

    /**
     * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public String visit(WhileStatement n, Void argu) throws Exception {
        String _ret=null;

        String conditionType = n.f2.accept(this, argu);

        // check if condition is a boolean
        if(!this.isOfType(conditionType, "boolean")) {
            throw new Exception(this.getExceptionHead() + "While loop condition is not of type boolean");
        }

        // execute nested statements
        n.f4.accept(this, argu);

        return _ret;
    }

    /**
     * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public String visit(PrintStatement n, Void argu) throws Exception {
        String _ret=null;

        String valueToPrint = n.f2.accept(this, argu);

        if(!this.isOfType(valueToPrint, "int")) {
            throw new Exception(this.getExceptionHead() + "Trying to print non int type expression");
        }
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
    public String visit(Expression n, Void argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
    public String visit(AndExpression n, Void argu) throws Exception {

        // check if first clause is a boolean
        String firstClauseType = n.f0.accept(this, argu);
        if(!this.isOfType(firstClauseType, "boolean")) {
            throw new Exception(this.getExceptionHead() + "Non boolean clause in logical && expression");
        }
        // check if second clause is a boolean
        String secondClauseType = n.f2.accept(this, argu);
        if(!this.isOfType(secondClauseType, "boolean")) {
            throw new Exception(this.getExceptionHead() + "Non boolean clause in logical && expression");
        }

        return "boolean";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public String visit(CompareExpression n, Void argu) throws Exception {

        // check if first clause is a boolean
        String firstExpressionType = n.f0.accept(this, argu);
        if(!this.isOfType(firstExpressionType, "int")) {
            throw new Exception(this.getExceptionHead() + "Non integer expression in '<' comparison expression");
        }
        // check if second clause is a boolean
        String secondExpressionType = n.f2.accept(this, argu);
        if(!this.isOfType(secondExpressionType, "int")) {
            throw new Exception(this.getExceptionHead() + "Non integer expression in '<' comparison expression");
        }

        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public String visit(PlusExpression n, Void argu) throws Exception {

        // check if first clause is a boolean
        String firstExpressionType = n.f0.accept(this, argu);
        if(!this.isOfType(firstExpressionType, "int")) {
            throw new Exception(this.getExceptionHead() + "Non integer expression in addition expression");
        }
        // check if second clause is a boolean
        String secondExpressionType = n.f2.accept(this, argu);
        if(!this.isOfType(secondExpressionType, "int")) {
            throw new Exception(this.getExceptionHead() + "Non integer expression in addition expression");
        }

        return "int";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public String visit(MinusExpression n, Void argu) throws Exception {

        // check if first clause is a boolean
        String firstExpressionType = n.f0.accept(this, argu);
        if(!this.isOfType(firstExpressionType, "int")) {
            throw new Exception(this.getExceptionHead() + "Non integer expression in minus expression");
        }
        // check if second clause is a boolean
        String secondExpressionType = n.f2.accept(this, argu);
        if(!this.isOfType(secondExpressionType, "int")) {
            throw new Exception(this.getExceptionHead() + "Non integer expression in minus expression");
        }

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public String visit(TimesExpression n, Void argu) throws Exception {

        // check if first clause is a boolean
        String firstExpressionType = n.f0.accept(this, argu);
        if(!this.isOfType(firstExpressionType, "int")) {
            throw new Exception(this.getExceptionHead() + "Non integer expression in multiplication expression");
        }
        // check if second clause is a boolean
        String secondExpressionType = n.f2.accept(this, argu);
        if(!this.isOfType(secondExpressionType, "int")) {
            throw new Exception(this.getExceptionHead() + "Non integer expression in multiplication expression");
        }

        return "int";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n, Void argu) throws Exception {

        // check if primary expression returns an integer array
        String arrayPointerType = n.f0.accept(this, argu);
        if(!this.isOfType(arrayPointerType, "int[]")) {
            throw new Exception(this.getExceptionHead() + "Looked up expression is not of type int[]");
        }

        // check if index is of type int
        String firstExpressionType = n.f2.accept(this, argu);
        if(!this.isOfType(firstExpressionType, "int")) {
            throw new Exception(this.getExceptionHead() + "Index expression of looked up int[] expression is not of type int");
        }

        return "int";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public String visit(ArrayLength n, Void argu) throws Exception {
        String pointerType = n.f0.accept(this, argu);

        if(!this.isOfType(pointerType, "int[]")) {
            throw new Exception(this.getExceptionHead() + "Lenght function applied to non int type element");
        }
        return "int";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    public String visit(MessageSend n, Void argu) throws Exception {

        String instanceToApplyType = n.f0.accept(this, argu);
        // get function name as literal
        this.setIdentifierAsString(true);
        String functionToApplyName = n.f2.accept(this, argu);
        this.setIdentifierAsString(false);

        ClassInfo instanceClass = this.getClass(instanceToApplyType);

        // check if given object is an instance of a class
        if(instanceClass == null) {
            throw new Exception(this.getExceptionHead() + "Cannot apply function to a non-class instance element");
        }

        // tries to fetch information about applying function for instance's class
        MethodInfo functionToApply = this.fetchClassFunction(instanceToApplyType, functionToApplyName);

        // check if class upon whose instance function is applied exists
        if(functionToApply == null) {
            throw new Exception(this.getExceptionHead() + "Function " + functionToApplyName + " is not defined for class " + instanceToApplyType + " instance");
        }

        // gather function call arguments
        this.startArgumentTypeGathering();
        n.f4.accept(this, argu);
        // check if arguments correspond to function definition
        // if there is mismatch, error occurs
        this.checkArgumentTypes(functionToApply, this.getCurrentArgumentTypes());

        // application has the same return type as the function applied
        return functionToApply.getType();
    }

    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public String visit(ExpressionList n, Void argu) throws Exception {
        String _ret = null;
        // add current argument type to list
        this.addArgumentType(n.f0.accept(this, argu));
        // gather rest of the arguments
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
    * f0 -> ( ExpressionTerm() )*
    */
    public String visit(ExpressionTail n, Void argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> ","
    * f1 -> Expression()
    */
    public String visit(ExpressionTerm n, Void argu) throws Exception {
        String _ret=null;
        // add current argument type to list
        this.addArgumentType(n.f1.accept(this, argu));
        return _ret;
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
    public String visit(PrimaryExpression n, Void argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> "this"
    */
    public String visit(ThisExpression n, Void argu) throws Exception {
        return this.getTraversedClass().getClassName();
    }

    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(ArrayAllocationExpression n, Void argu) throws Exception {
        String indexExpressionType = n.f3.accept(this, argu);
        
        // check if integer array indexing is of type int
        if(!this.isOfType(indexExpressionType, "int")) {
            throw new Exception(this.getExceptionHead() + "Index expression is not of type int during array initialization");
        }

        return "int[]";
    }

    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n, Void argu) throws Exception {

        // get class name as literal
        this.setIdentifierAsString(true);
        String classToInstantiate = n.f1.accept(this, argu);
        this.setIdentifierAsString(false);

        // check if given class exists
        if(this.getClass(classToInstantiate) == null) {
            throw new Exception(this.getExceptionHead() + "Thre is no constructor for class " + classToInstantiate);
        }

        return classToInstantiate;
    }


    /**
    * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
    public String visit(Clause n, Void argu) throws Exception {

        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> "!"
    * f1 -> Clause()
    */
    public String visit(NotExpression n, Void argu) throws Exception {
        String clauseType = n.f1.accept(this, argu);

        // check if logical not is applied to boolean value
        if(!this.isOfType(clauseType, "boolean")) {
            throw new Exception(this.getExceptionHead() + "Logical not applied to non boolean clause");
        }
        return "boolean";
    }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    public String visit(BracketExpression n, Void argu) throws Exception {
        return n.f1.accept(this, argu);
    }
}
