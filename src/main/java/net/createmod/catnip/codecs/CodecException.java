package net.createmod.catnip.codecs;

public class CodecException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;
    public CodecException(String message) { super(message); }
    public CodecException(String message, Throwable cause) { super(message, cause); }
}
