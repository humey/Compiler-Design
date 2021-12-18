package Models;

import Scanner.ErrorObject;

import java.util.ArrayList;

public class TreeNode {

    private TreeNode left, right, middle;
    private ENodeTypes nodeValue;
    private SymbolTableData symbolTableRecord;
    private ErrorObject error;
    private String errorCorrection;
    private ArrayList<ErrorObject> errors = new ArrayList<>();

    /*
        Instantiating TreeNodes with overloading based on passed in parameters
        Probably dont need this, but check later
     */

    public TreeNode() {
        this.nodeValue = null;
        this.left = null;
        this.middle = null;
        this.right = null;
        this.symbolTableRecord = null;
    }
    public TreeNode(ENodeTypes nodeValue) {
        this.nodeValue = nodeValue;
        this.left = null;
        this.middle = null;
        this.right = null;
        this.symbolTableRecord = null;
    }

    public TreeNode(ENodeTypes nodeValue, SymbolTableData symbolTableRecord) {
        this.nodeValue = nodeValue;
        this.left = null;
        this.middle = null;
        this.right = null;
        this.symbolTableRecord = symbolTableRecord;
    }

    public TreeNode(ENodeTypes nodeValue, ErrorObject error) {
        this.nodeValue = nodeValue;
        this.left = null;
        this.middle = null;
        this.right = null;
        this.error = error;
    }

    public TreeNode(ENodeTypes nodeValue, TreeNode left, TreeNode right) {
        this.nodeValue = nodeValue;
        this.left = left;
        this.middle = null;
        this.right = right;
        this.symbolTableRecord = null;
    }

    public TreeNode(ENodeTypes nodeValue, TreeNode left, TreeNode right, SymbolTableData data) {
        this.nodeValue = nodeValue;
        this.left = left;
        this.middle = null;
        this.right = right;
        this.symbolTableRecord = data;
    }

    public TreeNode(ENodeTypes nodeValue, TreeNode left, SymbolTableData symbolTableRecord) {
        this.nodeValue = nodeValue;
        this.left = left;
        this.middle = null;
        this.right = null;
        this.symbolTableRecord = symbolTableRecord;
    }

    public TreeNode(ENodeTypes nodeValue, TreeNode left, TreeNode middle, TreeNode right,  SymbolTableData symbolTableRecord) {
        this.nodeValue = nodeValue;
        this.left = left;
        this.middle = middle;
        this.right = right;
        this.symbolTableRecord = symbolTableRecord;
    }


    public String toString() {
        assert this.nodeValue != null;
        return this.nodeValue.nodeString;
    }

    /*
        Getters & Setters
    */
    public String getNodeValue() {
        assert this.nodeValue != null;
        return this.nodeValue.nodeString;
    }

    public TreeNode getLeft() {
        return left;
    }

    public TreeNode getRight() {
        return right;
    }

    public TreeNode getMiddle() {
        return middle;
    }

    public ENodeTypes getValue() {
        return this.nodeValue;
    }

    public void setRecord(SymbolTableData data) {
        this.symbolTableRecord = data;
    }

    public SymbolTableData getSymbolTableRecord() {
        return symbolTableRecord;
    }

    public void setLeft(TreeNode left) {
        this.left = left;
    }

    public void setMiddle(TreeNode middle) {
        this.middle = middle;
    }

    public void setRight(TreeNode right) {
        this.right = right;
    }

    public void setErrorCorrection(String correction) {
        this.errorCorrection = correction;
    }

    public ErrorObject getError() {
        return this.error;
    }

    public void setData(SymbolTableData data) {
        this.symbolTableRecord = data;
    }

    public ArrayList<ErrorObject> getErrors() {
        return this.errors;
    }

    public void addError(ErrorObject error) {
        this.errors.add(error);
    }

    public void setNodeType(ENodeTypes type) {
        this.nodeValue = type;
    }
}