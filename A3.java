/*
    Comp3290 Compiler Design
    A2.java class - Main runner for program - Commented out printing of token for A2, kept it just incase
                    Gets the list of tokens and passes to the parser to create our AST. On return if appends
                    the OutputHelpers string builder with proper padding. After all this appends everything together
                    prints to console and outputs the program listing with errors to a .txt file in the running DIR.
    Created by - Keeylan Hume C3320396
 */

import Generator.CodeGen;
import Models.Token;
import Models.TreeNode;
import Parser.Parser;
import Scanner.ErrorObject;
import Scanner.LexicalScanner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class A3 {

    private static LexicalScanner SuperFastScanner;
    private static OutputHelper OutputHelper;
    public static StringBuilder programListing;
    public static ArrayList<Token> tokenList;
    public static Parser p;
    private static StringBuilder finalOutput = new StringBuilder();
    private static String fileName;

    public static void main(String[] args) throws IOException {

        File f = new File(args[0]);
        fileName = getFileNameFromPath(f);

        programListing = new StringBuilder();
        SuperFastScanner = new LexicalScanner(args[0], programListing);
        OutputHelper = new OutputHelper();
        tokenList = new ArrayList<>();

        while(!SuperFastScanner.endOfFile()) {
            Token token = SuperFastScanner.getToken();

            if(token.getTokenNo() != Token.TUNDF) {
                tokenList.add(token);
            }
            else {

                OutputHelper.printError(token, SuperFastScanner.error);
            }
        }

        // Removes any carriage returns because Windows is silly
        // Append the program listing to our output builder
        SuperFastScanner.removeCarriageReturns(programListing.toString());
        finalOutput.append(programListing);

        // Create and build out output to our outputhelper string builder
        p = new Parser(tokenList);
        TreeNode root = p.createMyTree();
        OutputHelper.printTree(root);

        if(p.isCorrect()) {
            CodeGen c = new CodeGen();

            String is = c.generateMyCode(root);

            outPutProgramListingFile(programListing.toString());
            outputModFile(is);

            System.out.println(fileName + " compiled successfully");
            System.out.println("Current .mod output is as follows:" + "\n");

            System.out.println(is);

        }
        else {
            // Print semantic and syntax errors to console
            for(ErrorObject e : p.getSemanticErrors()) {

                System.out.println("Semantic error: " + e.toString());
            }

            for(ErrorObject e : p.getSyntaxErrors()) {

                System.out.println("Syntax error: " + e.toString());

            }
        }
    }

    public static void outPutProgramListingFile(String output) throws IOException {

        FileWriter outputTxt = new FileWriter(fileName + ".1st", false);
        outputTxt.write(output);
        outputTxt.close();
    }

    public static void outputModFile(String output) throws IOException {

        FileWriter outputTxt = new FileWriter(fileName + ".mod", false);
        outputTxt.write(output);
        outputTxt.close();

    }

    public static String getFileNameFromPath(File f) {

        String s = f.getName();

        String[] l = s.split("\\.");

        return l[0];
    }
}