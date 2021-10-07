package cz.startnet.utils.pgdiff.parsers.antlr;

import org.antlr.v4.runtime.Token;

import cz.startnet.utils.pgdiff.ContextLocation;

public class AntlrError extends ContextLocation {

    private static final long serialVersionUID = -6122212579392980835L;
    private final String msg;
    private final String text;
    private final int stop;
    private final ErrorTypes errorType;

    public AntlrError(Token tokenError, String location, int line, int charPositionInLine, String msg) {
        this(tokenError, location, line, charPositionInLine, msg, ErrorTypes.OTHER);
    }

    public AntlrError(Token tokenError, String location, int line, int charPositionInLine, String msg, ErrorTypes errorType) {
        this(location, line, charPositionInLine, msg,
                (tokenError == null ? -1 : tokenError.getStartIndex()),
                (tokenError == null ? -1 : tokenError.getStopIndex()),
                (tokenError == null ? null : tokenError.getText()),
                errorType);
    }

    private AntlrError(String location, int line, int charPositionInLine, String msg,
            int start, int stop, String text, ErrorTypes errorType) {
        super(location, start, line, charPositionInLine);
        this.msg = msg;
        this.stop = stop;
        this.text = text;
        this.errorType = errorType;
    }

    public AntlrError copyWithOffset(int offset, int lineOffset, int inLineOffset) {
        return new AntlrError(getFilePath(), getLineNumber() + lineOffset,
                (getLineNumber() == 1 ? getCharPositionInLine() + inLineOffset : getCharPositionInLine()),
                msg,
                (getStart() == -1 ? -1 : getStart() + offset),
                (stop == -1 ? -1: stop + offset),
                text, errorType);
    }

    public String getMsg() {
        return msg;
    }

    public String getText() {
        return text;
    }

    public int getStart() {
        return getOffset();
    }

    public int getStop() {
        return stop;
    }

    public ErrorTypes getErrorType() {
        return errorType;
    }

    @Override
    public String toString() {
        // ANTLR position in line is 0-based, GUI's is 1-based
        return getFilePath() + " line " + getLineNumber() + ':' + (getCharPositionInLine() + 1) + ' ' + getMsg();
    }
}