package auxiliarry_classes;

public class TupleObject<X, Y> { 
  private X x; 
  private Y y; 
  public TupleObject(X x, Y y) { 
    this.x = x; 
    this.y = y; 
  } 

  public X getItemX() { 
    return this.x;
  } 

  public Y getItemY() { 
    return this.y; 
  } 


  public void setItemX(X itemX) { 
    this.x = itemX;
  } 

  public void setItemY(Y itemY) { 
    this.y = itemY; 
  } 


} 