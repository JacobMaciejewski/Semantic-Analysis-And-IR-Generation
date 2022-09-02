import syntaxtree.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import auxiliarry_classes.*;

public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length == 0){
            System.err.println("Usage: java Main <inputFile1> ... <inputFileN>");
            System.exit(1);
        }
        for(String inputFile: args) {
        FileInputStream fis = null;
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println("File: " + inputFile);
        inputFile = "./target_java/" + inputFile;
        try {
            fis = new FileInputStream(inputFile);
            MiniJavaParser parser = new MiniJavaParser(fis);
            System.err.println("Program parsed successfully.");
            MGVisitor MGEvaluation = new MGVisitor();
            Goal root = parser.Goal();
            try {
                root.accept(MGEvaluation, null);
                System.err.println("Metadata Gathering Visitor parsed successfully");
                fis = new FileInputStream(inputFile);
                parser = new MiniJavaParser(fis);
                
                TCVisitor TCEvaluation = new TCVisitor(MGEvaluation.getSymbolTable());
                root = parser.Goal();
                try {
                    root.accept(TCEvaluation, null);
                    System.err.println("Type Checking Visitor parsed successfully");
                    // MGEvaluation.printOffsets();
                    fis = new FileInputStream(inputFile);
                    parser = new MiniJavaParser(fis);
                    
                    LLVMVisitor LLVMVEvaluation = new LLVMVisitor(inputFile, MGEvaluation.getSymbolTable());
                    root = parser.Goal();
                    try {
                        root.accept(LLVMVEvaluation, null);
                        System.err.println("LL representation produced successfully");
                    }
                    catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                }
                catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
            catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
        catch (ParseException ex) {
            System.out.println(ex.getMessage());
        }
        catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
        }
        finally {
                try {
                    if (fis != null) fis.close();
                }
                catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
        }
        System.out.println("-------------------------------------------------------------------------------------");
        }
    }

}