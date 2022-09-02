all: compile

compile:
	java -jar ../jtb132di.jar -te minijava.jj
	java -jar ../javacc5.jar minijava-jtb.jj
	javac Main.java

clean:
	rm ./auxiliarry_classes/*.class
	rm -f *.class *~ minijava-jtb.jj
	rm -f JavaCharStream.java ParseException.java Token.java TokenMgrError.java MiniJavaParser.java MiniJavaParserConstants.java MiniJavaParserTokenManager.java
	rm -rf visitor
	rm -rf syntaxtree
