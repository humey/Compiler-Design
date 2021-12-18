package Models;

/*
    Comp3290 Compiler Design
    Models.ScannerState.enum class - Handles the scanners state
    Created by - Keeylan Hume C3320396

 */

public enum EScannerState {
        START,
        IDENT,
        INTEGER,
        COMMENT,
        STRING,
        DELIM,
        ERROR,
        EOF,
}