package auxiliarry_classes;

// can be used to store information
// about a field or an argument/variable
// in the later case offset is unidentified (-1)
public class IdentifierInfo {

    // field's type and offset
    String identifierName;
    private TupleObject<String, Integer> info;

    public IdentifierInfo(String identifierName, String identifierType) { 

        TupleObject<String, Integer> information = new TupleObject<String, Integer>(identifierType, -1);

        this.setName(identifierName);
        this.setInfo(information);
    } 

    public IdentifierInfo(String identifierName, String identifierType, Integer identifierOffset) { 

        TupleObject<String, Integer> information = new TupleObject<String, Integer>(identifierType, identifierOffset);

        this.setName(identifierName);
        this.setInfo(information);
    } 

    public void setName(String fieldName) {
        this.identifierName = fieldName;
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

    public String getName() {
        return this.identifierName;
    }

    public TupleObject<String, Integer> getInfo() {
        return this.info;
    }

    public String getType() {
        return this.getInfo().getItemX();
    }

    public Integer getOffset() {
        return this.getInfo().getItemY();
    }

    // fields are described by their offset
    // if it is defined (not -1)
    // then identifier refers to a field
    public boolean isField() {
        return (this.getOffset() != -1);
    }
   
}
