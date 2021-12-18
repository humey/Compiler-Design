package Scanner;

/*
    Comp3290 Compiler Design
    Scanner.java class - Used to scan the given source file and tokenize the appropriate values
                         Will be extended to pass the tokens to the parser in future.
                         This will implement outputting the tokens and errors for A1
    Created by - Keeylan Hume C3320396
 */

import Models.EScannerState;
import Models.Token;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class LexicalScanner {

    // Models.Token scanner has found
    private Token token, awaitToken;

    // String builder used for mutability and faster access/changes
    private StringBuilder sb = new StringBuilder();
    private StringBuilder programListing;

    // The scanners state
    private EScannerState state;

    // Stores error information
    public ErrorObject error;

    // Pointer for where the reader is up to on the line
    private int currentPos, startPos, inputLineNo, colNo;

    // Peek variable to hold the next char value
    private int peek;

    private boolean EOF = false, tokenWaiting = false;

    // Variables used to read in the source file - File, FileReader and BufferedReader
    private final BufferedReader br;
    private final FileInputStream inputFile;
    private final InputStreamReader isr;

    /*
        Constructor takes in the source file
        Initiates the reader and default values
    */
    public LexicalScanner(String inputFile, StringBuilder programListing) throws FileNotFoundException {

        this.colNo = 0;
        this.currentPos = 0;
        this.startPos = 0;
        this.inputLineNo = 1;
        this.awaitToken = new Token();

        this.state = EScannerState.START;

        // Loading input file
        this.inputFile = new FileInputStream(inputFile);
        this.isr = new InputStreamReader(this.inputFile, StandardCharsets.UTF_8);
        this.br = new BufferedReader(isr);

        this.programListing = programListing;
        programListing.append(addLineNumber());
    }

    private String addLineNumber() {
        StringBuilder sbLine = new StringBuilder();
        sbLine.append("Line ").append(inputLineNo).append(": ");

        return sbLine.toString();
    }

    public boolean endOfFile() {
        return EOF;
    }

    /*
        Main scanner function - First checks if theres a previously awaiting token if not
        Sets a new token, if our buffer and peek are empty consume until we get a proper char
        consume all white space again, check if we are at EOF - this needs to be here in case a full whitespace file
        Call our checkState function. Each state in the switch has its own top level comment
     */
    public Token getToken() throws IOException {

        if(tokenWaiting) {
            tokenWaiting = false;
            return this.awaitToken;
        }

        this.token = new Token();
        this.error = null;

        // Peek will only be empty on the first token
        if(peek == 0) {
            peek = br.read();
        }

        consumeAllWhiteSpace();

        // input file is finished
        if(peek == -1 || peek == 65535) {
            EOF = true;
            this.token.setTokenDetails(Token.T_EOF, inputLineNo, colNo);

            return this.token;
        }

            // Transition the scanner to a state
            checkingState();

            switch(this.state) {

                /*
                    IDENT case
                    Our buffer has a potential identifier or keyword token - check the reserved keywords and hold value in temp
                    temp is used to stop double checking the value, if its a keyword, create the token delete, reset and return
                    If temp is -1 we have a normal IDENT, set the token, delete, reset and return - Nice and easy
                */
                case IDENT:

                    int temp;
                    temp = this.token.checkReservedWords(sb.toString());

                    if(temp != -1) {
                        this.token.setTokenDetails(temp, inputLineNo, colNo);

                        deleteFromBufferAndReset();
                        return this.token;
                    }
                    // Is not a keyword, must be an ident
                    this.token.setTokenDetails(Token.TIDEN, sb.substring(startPos,currentPos), inputLineNo, colNo);

                    deleteFromBufferAndReset();

                    return this.token;

                /*
                    INTEGER case
                    Our buffer start with 0-9 - We need to check if peek is holding a .
                    If so we then need to consume, and check if peek is holding a digit this means we have a float
                    consume until we have a deliminator, set token, delete, reset and return
                    If above fails, we just have an integer but we also have the . we consumed
                    Create the awaitingToken with our . then deleteFromEndOfBuffer that fixed up our pointers and buffer
                    then create the integer token, delete, reset and return
                    if all the above fails we are only left with a boring integer literal, consume, delete, reset and return
                */
                case INTEGER:

                     if(peek == '.') {
                         consumeNextChar();

                         if(isDigit((char) peek)) {
                             // WE HAVE A FLOAT - Continue to consume all the integers until we cant no more baby
                             consumeAllIntegers();

                             this.token.setTokenDetails(Token.TFLIT, sb.substring(startPos,currentPos), inputLineNo, colNo);
                             deleteFromBufferAndReset();

                             return this.token;
                         }

                         this.awaitToken.setTokenDetails(Token.TDOTT, inputLineNo, colNo);
                         deleteFromEndOfBuffer(1);
                         tokenWaiting = true;

                         this.token.setTokenDetails(Token.TILIT, sb.substring(startPos,currentPos), inputLineNo, colNo);
                         deleteFromBufferAndReset();

                         return this.token;
                     }

                    this.token.setTokenDetails(Token.TILIT, sb.substring(startPos,currentPos), inputLineNo, colNo);
                    deleteFromBufferAndReset();

                    return this.token;

                /*
                    DELIMINATOR case
                    We have a deliminator in our buffer, switch on this and check if its a deliminator
                    that has multiple tuple values, <= <<, >= >> .. check peek, if true consume, create token
                    delete, reset and return.
                    Check the other potential tuples with only one possible value, !=. +=. == ... consume, create
                    delete, reset and return
                    If above faile we have a boring single deliminator, create, delete, reset and return
                */
                case DELIM:

                    switch(sb.charAt(startPos)) {

                        case '<':
                            switch(peek) {
                                case '<':
                                case '=':

                                    programListing.append(sb.toString());

                                    consumeNextChar();

                                    this.token.setTokenDetails(this.token.checkDeliminators(sb.toString()), inputLineNo, colNo);



                                    deleteFromBufferAndReset();

                                    return this.token;
                            }
                            break;

                        case '>':
                            switch(peek) {
                                case '>':
                                case '=':

                                    programListing.append(sb.toString());

                                    consumeNextChar();

                                    this.token.setTokenDetails(this.token.checkDeliminators(sb.toString()), inputLineNo, colNo);



                                    deleteFromBufferAndReset();

                                    return this.token;
                            }
                            break;

                        case '!':
                        case '=':
                        case '+':
                        case '-':
                        case '*':
                        case '/':
                            if (peek == '=' && !tokenWaiting) {

                                programListing.append(sb.toString());

                                consumeNextChar();
                                this.token.setTokenDetails(this.token.checkDeliminators(sb.toString()), inputLineNo, colNo);



                                deleteFromBufferAndReset();

                                return this.token;
                            }
                            break;
                    }

                    this.token.setTokenDetails(this.token.checkDeliminators(sb.charAt(startPos)), inputLineNo, currentPos);

                    programListing.append(sb.toString());

                    deleteFromBufferAndReset();

                    return this.token;
                /*
                    COMMENT case
                    We have a /-- or /** in our buffer, switch on this to check which one
                    consume until we have grabbed it all, delete, reset and call getToken to return to main
                    TODO - Dont remove comments add to the program listing
                 */

                case COMMENT:
                    switch(sb.substring(startPos,currentPos)) {

                        // Single line comment
                        case "/--":
                            while(peek != '\n') {
                                //programListing.append((char)peek);
                                consumeNextChar();
                            }
                            deleteFromBufferAndReset();

                            // No token to return, find new state
                            return getToken();

                        // Multi-line comment
                        case "/**":

                            while(!sb.toString().contains("**/")) {
                                //programListing.append((char)peek);
                                consumeNextChar();
                            }
                            deleteFromBufferAndReset();

                            return getToken();
                    }

                /*
                    STRING case
                    Our first char is a ".. We consume unless we encounter a \n in peek, if we have this creates an error object
                    Due to windows using \r and \n printing out the straight lexeme with \r removes the lexeme, so made a remove carriage
                    function, then set the TUNDF token, delete, reset and return.
                    If there was no \n we still have the closing " in the buffer so consume, create token, delete, reset and return
                 */
                case STRING:
                    // Consume until we see either a closing " or a \n
                    programListing.append("\"");
                     while(!isString((char) peek) && peek != '\n') {
                         consumeNextChar();
                     }

                    // Error state
                    if(peek == '\n') {

                        this.error = new ErrorObject("Lexical error", removeCarriageReturns(sb.substring(startPos,currentPos)),inputLineNo, colNo);
                        this.token.setTokenDetails(Token.TUNDF, inputLineNo, colNo);

                        deleteFromBufferAndReset();
                        return this.token;
                    }

                    // Still have the " in the peek variable
                    consumeNextChar();

                    this.token.setTokenDetails(Token.TSTRG, sb.substring(startPos,currentPos), inputLineNo, colNo);
                    deleteFromBufferAndReset();

                    return this.token;

                /* ERROR Case
                    Consumes until we encounter either a digit, alphanumeric, white space or deliminator
                    checkErrorState checks if the last buffer index + peek creates an allowed token and temp holds this value
                    if it does, consume the deliminator, set awaiting token, delete these from the buffer and fix currentPos
                    create an error object with the left over undefined symbols in the buffer
                    Sorry for the spaghetti code, that one case of != took me by surprise after finishing everything else
                 */
                case ERROR:

                    while(!isLetter((char) peek) && !isDigit((char) peek) &&
                            this.token.checkDeliminators((char) peek) == -1 && !checkForNonTokenDeliminators()) {

                        consumeNextChar();

                    }

                    temp = checkErrorState(sb.charAt(currentPos - 1));

                    if(temp != -1) {
                        consumeNextChar();

                        this.awaitToken.setTokenDetails(temp, inputLineNo, colNo);
                        deleteFromEndOfBuffer(2);

                        tokenWaiting = true;
                    }

                    // if sb is not empty create the error object, else we dont need too
                    if(sb.length() != 0) {
                        this.error = new ErrorObject("Lexical error", sb.toString(), inputLineNo, colNo);
                        this.token.setTokenDetails(Token.TUNDF, inputLineNo, colNo);

                        deleteFromBufferAndReset();

                        return this.token;
                    }

                return getToken();
        }

        return this.token;
    }

    /*
        Helper function to delete chars from end of the buffer
        takes in the amount to delete and fixed colNo and currentPos
     */
    private void deleteFromEndOfBuffer(int amount) {
        sb.delete(sb.length() - amount, sb.length());
        currentPos -= amount;
        colNo -= amount;
    }

    /*
        Helper function to clear up the main switch statement
        Check if the passed in char value + peek creates an allowed token
        Returns -1 if not, else the token number
     */
    private int checkErrorState(char i) {
        String sbTemp = String.valueOf(i) + (char) peek;
        return this.token.checkDeliminators(sbTemp);
    }

    /*
        Main state transition for the scanner
        We consume until we hit a deliminator - Add to the buffer if its empty
        Then check what state we are in from the first char in our buffer
     */
    private void checkingState() throws IOException {
        // We should already have consumed all white spaces
        consumeUntilDeliminator();

        if(sb.length() == 0) {
            sb.append((char) peek);
            peek = (char) br.read();
            colNo++;
            currentPos++;
        }

        // Checking if the first position in integer
        if(isDigit(sb.charAt(startPos))) {
            this.state = EScannerState.INTEGER;
            return;

        }

        // Check if first position is a letter
        if(isLetter(sb.charAt(startPos))) {
            this.state = EScannerState.IDENT;
            return;

        }

        // Check if the first position is a deliminator
        if(this.token.checkDeliminators(sb.charAt(startPos)) != -1) {

            // Check if we have a / --. or **
            if(sb.charAt(startPos) == '/' && peek == '-' || peek == '*') {
                programListing.append("/");
                consumeNextChar();
                if(peek == '-' || peek == '*') {
                    consumeNextChar();
                    this.state = EScannerState.COMMENT;
                    return;
                }

                // Not a comment - Save TMINS in an awaitingToken
                // Delete from buffer, reset currentpos
                this.awaitToken.setTokenDetails(Token.TMINS, inputLineNo, colNo);
                sb.deleteCharAt(currentPos - 1);
                currentPos--;
                tokenWaiting = true;
            }

            this.state = EScannerState.DELIM;
            return;

        }

        // Check if the first char is a " - make sure peek is not a new line
        if(isString(sb.charAt(startPos))) {
            if(peek != '\n'){
                this.state = EScannerState.STRING;
                return;
            }

            this.state = EScannerState.ERROR;
            return;

        }

        // If the above fail the first position will be an error
        this.state = EScannerState.ERROR;
    }

    public boolean isString(char c) {
        return (c == '\"');
    }

    public boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z');
    }

    public boolean isDigit(char c) {
        return (c >= '0' && c <= '9');
    }

    private boolean checkForNonTokenDeliminators() {

        switch(peek) {

            case 10: // Newline - Increase the lineNo and reset colNo
                this.inputLineNo++;
                this.colNo = 0;
                programListing.append("\n");
                programListing.append(addLineNumber());
                return true;
            case 32: // Whitespace
                return true;
            case 9:  // Tab
                programListing.append("\t");
            case 13: // Carriage return
                return true;
        }

        return false;
    }

    // Refactor function - reduce code duplication
    private void deleteFromBufferAndReset() throws IOException {
        if(sb.length() == 1) {
            sb.deleteCharAt(startPos);
        }
        else {
            sb.delete(startPos,currentPos);
        }

        currentPos = 0;
        this.state = EScannerState.START;
    }

    // Refactor function - consume next char - reduced code duplication
    private void consumeNextChar() throws IOException {
        sb.append((char) peek);
        programListing.append((char) peek);
        peek = br.read();
        colNo++;
        currentPos++;
    }

    /*
        Consume until we hit a deliminator - Refactored function to reduce code duplication
        Need to check if the first char is a digit, if so we just consume all the digits
        Else we consume until we hit a deliminator or whitespace
    */
    private void consumeUntilDeliminator() throws IOException {

        if(isDigit((char) peek)) {
           consumeAllIntegers();
           return;
        }

        while(this.token.checkDeliminators((char) peek) == -1 && !checkForNonTokenDeliminators()
                && isLetter((char) peek) || isDigit((char) peek)) {

            consumeNextChar();

        }
    }

    // Refactor function - consume until we hit a non whitespace - reduced code duplication
    private void consumeAllWhiteSpace() throws IOException {
        while(checkForNonTokenDeliminators()) {
            peek = br.read();
            programListing.append(" ");
            colNo++;
        }
    }

    // Refactor qfunction - consumes until we hit a non-integer char
    private void consumeAllIntegers() throws IOException {
        while(!isLetter((char) peek) && this.token.checkDeliminators((char) peek) == -1 && !checkForNonTokenDeliminators()) {
            consumeNextChar();
        }
    }

    public String removeCarriageReturns(String s) {
        // Use this to remove all carriage returns from programListing
        return s.replace("\r","");
    }
}