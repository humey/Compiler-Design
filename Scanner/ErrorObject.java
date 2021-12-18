package Scanner;

/*
    Comp3290 Compiler Design
    Scanner.ErrorObject.java class - Handles any error objects
                             Type of error - when theres more later this can be better implemented like a map or enum
                             The error data and lineNo, colNo
    Created by - Keeylan Hume C3320396

 */

public class ErrorObject {

    // Error type - Semantic / Syntax
    private String errorType, errorData;
    private int lineNo, colNo;

    public ErrorObject(String errorType, String errorData, int lineNo, int colNo) {

        this.errorType = errorType;
        this.errorData = errorData;
        this.lineNo = lineNo;
        this.colNo = colNo;

    }

    public ErrorObject(String errorData, int lineNo) {
        this.errorData = errorData;
        this.lineNo = lineNo;
    }

    public void setErrorType(String type) {
        this.errorType = type;
    }

    public String toString() {
        return String.format("%s %s ", errorData, lineNo);
    }

    public String printParserErrors() {

        return String.format("%s %s \n", errorData, lineNo);

    }
}
