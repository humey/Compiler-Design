/*
    Comp3290 Compiler Design
    OutputHelper.java class - Handles the output to std output for Scanner & Parser
                              Doesn't use the printToken but left it in anyway
                              A2 uses PrintTree to and helper methods to do a pre-order traverse
                              Appends to the sb and any errors append to errorBuilder
    Created by - Keeylan Hume C3320396

 */

import Models.ENodeTypes;
import Models.Token;
import Models.TreeNode;
import Scanner.ErrorObject;

public class OutputHelper {

    /*
        Token output -
                        Max char allowed on a line 60
                        Prevoutput true if he last print was an error - False if a normal output

        Parser output -
                        Gets the difference of padding needed to keep cols of 10 within multiples of 7
                        Holds the error messages in its own builder to append after parsing
                        Recursively pre-order traverses the tree
    */
    private final int MAX_CHARS = 60;
    private int count = 0,  difference, colCount = 0;
    public StringBuilder scannerErrors = new StringBuilder(), errorBuilder = new StringBuilder(), sb = new StringBuilder();
    private boolean prevOutput;
    private int PADDING = 7;
    private final int MAX_COL = 10;
    private int numberOfErrors = 0;

    public OutputHelper() {}

    public void printToken(Token token) {

        if(prevOutput) {
            System.out.println();
        }

        System.out.print(sb.append(token.toString()));
        count += sb.length();

        if(count > MAX_CHARS) {
            this.count = 0;
            System.out.println();
        }

        sb.setLength(0);

        prevOutput = false;
    }

    public void printError(Token token, ErrorObject error) {

        // Append new line if needed
        if(this.count != 0) {
            scannerErrors.append("\n");
        }

        scannerErrors.append(token.toString()).append("\n");

        scannerErrors.append(error.toString());
        System.out.print(scannerErrors.toString());

        //scannerErrors.setLength(0);

        prevOutput = true;
    }

    public void printTree(TreeNode root) {

        if(root == null) {
            return;
        }

        if(root.getValue() != ENodeTypes.NUNDF) {


            difference = PADDING % root.getNodeValue().length();

            sb.append(root.getNodeValue());
            colCount++;

            // Padding after node value
            sb.append(" ".repeat(difference));

            if(colCount == 10) {
                sb.append("\n");
                colCount = 0;
            }

            if(root.getSymbolTableRecord() != null) {

                    while(root.getSymbolTableRecord().getName().length() > PADDING) {
                        PADDING *= 2;
                    }

                    difference = PADDING - root.getSymbolTableRecord().getName().length();
                    sb.append(root.getSymbolTableRecord().getName());
                    colCount++;

                    sb.append(" ".repeat(difference));

            }

            // Reset padding back to 7
            PADDING = 7;

            if(colCount == 10) {
                sb.append("\n");
                colCount = 0;
            }

            for(ErrorObject e : root.getErrors()) {

                errorBuilder.append(e.printParserErrors());

            }

        }
        else {
            // Save error messages for after tree print out and skip out this node
            errorBuilder.append(root.getError().printParserErrors()).append("\n");
        }

        preOrder(root.getLeft());
        preOrder(root.getMiddle());
        preOrder(root.getRight());

    }

    private void preOrder(TreeNode node) {
        printTree(node);
    }

    public String printOutput() {
        return sb.toString();
    }
}