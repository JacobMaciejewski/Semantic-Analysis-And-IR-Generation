package auxiliarry_classes;

import java.util.*;

public class MethodInfo {

    private String methodName;
    // method's type and offset
    private TupleObject<String, Integer> info;
    // map from argument/local variable name to their type
    private Map<String, IdentifierInfo> arguments;
    private Map<String, IdentifierInfo> variables;

    public MethodInfo(String name, String methodType, Integer methodOffset) { 

        TupleObject<String, Integer> information = new TupleObject<String, Integer>(methodType, methodOffset);
        Map<String, IdentifierInfo> methodArguments = new LinkedHashMap<String, IdentifierInfo>();
        Map<String, IdentifierInfo> methodVariables = new LinkedHashMap<String, IdentifierInfo>();

        this.setName(name);
        this.setInfo(information);
        this.setArguments(methodArguments);
        this.setVariables(methodVariables);
    }

    public MethodInfo(String name, String methodType) { 

        TupleObject<String, Integer> information = new TupleObject<String, Integer>(methodType, -1);
        Map<String, IdentifierInfo> methodArguments = new LinkedHashMap<String, IdentifierInfo>();
        Map<String, IdentifierInfo> methodVariables = new LinkedHashMap<String, IdentifierInfo>();

        this.setName(name);
        this.setInfo(information);
        this.setArguments(methodArguments);
        this.setVariables(methodVariables);
    } 


    public void setName(String methodName) {
        this.methodName = methodName;
    }

    public void setInfo(TupleObject<String, Integer> info) {
        this.info = info;
    }

    public void setType(String givenType) {
        this.getInfo().setItemX(givenType);
    }


    public void setOffset(Integer givenOffset) {
        this.getInfo().setItemY(givenOffset);
    }

    public void setArguments(Map<String, IdentifierInfo> arguments) {
        this.arguments = arguments;
    }

    public void setVariables(Map<String, IdentifierInfo> variables) {
        this.variables = variables;
    }

    public String getName() {
        return this.methodName;
    }

    public TupleObject<String, Integer> getInfo() {
        return this.info;
    }

    public Map<String, IdentifierInfo> getArguments() {
        return this.arguments;
    }

    public Map<String, IdentifierInfo> getVariables() {
        return this.variables;
    }

    public String getType() {
        return this.getInfo().getItemX();
    }

    public Integer getOffset() {
        return this.getInfo().getItemY();
    }

    // add new parameter's info to the method info structure
    public void addParameter(IdentifierInfo newParameterInfo) {
        this.getArguments().put(newParameterInfo.getName(), newParameterInfo);
    }

    // store information about local variables
    public void addLocalVariables(Map<String, IdentifierInfo> localVariables) {

        for (Map.Entry<String,IdentifierInfo> variableInfo : localVariables.entrySet()) {
            this.getVariables().put(variableInfo.getKey() , variableInfo.getValue());
        }
    }

    // checks if method is overloaded by the given method
    // it happens when they have:
    // different types
    // different argument types
    // different number of arguments
    public boolean isOverloadedBy(MethodInfo otherMethodInfo) {

        // get arguments iterators
        Iterator<Map.Entry<String, IdentifierInfo>> myArguments = this.getArguments().entrySet().iterator();
        Iterator<Map.Entry<String, IdentifierInfo>> otherArguments = otherMethodInfo.getArguments().entrySet().iterator();

        // check only in the case of same names
        if(this.getName().equals(otherMethodInfo.getName())) {
            // different type
            if(!this.getType().equals(otherMethodInfo.getType())) {
                return true;
            }

            // iterate through all arguments of both lists
            // check if they have the same types
            while(myArguments.hasNext() && otherArguments.hasNext()) {

                String myArgumentType = myArguments.next().getValue().getType();
                String otherArgumentType = otherArguments.next().getValue().getType();

                // different argument types
                if(!myArgumentType.equals(otherArgumentType)) {
                    return true;
                }
            }

            // one of the arguments list contains more arguments than the other one
            // overloading occurs
            if(myArguments.hasNext() || otherArguments.hasNext()) {
                return true;
            }
        }
        
        return false;
    }

    // checks if current method overrides
    // one defined in parent class
    public boolean overrides() {
        return (this.getOffset() == -1);
    }

}