package com.yanivian.riddlr.common.util;

import com.google.protobuf.Internal;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;
import com.google.protobuf.util.JsonFormat;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** Collection of utility methods to help work with protocol buffers. */
public final class TextProtoUtils {
  private static final Charset CHARSET = StandardCharsets.UTF_8;

  // Not instantiable.
  private TextProtoUtils() {}

  /** Unencrypted string encoding of the given proto payload, refered to as a textproto. */
  public static <T extends MessageOrBuilder> String encodeToString(MessageOrBuilder data) {
    return TextFormat.printer().printToString(data);
  }

  /** UTF-8 encoding of the string encoding of the given proto payload. */
  public static <T extends MessageOrBuilder> byte[] encode(MessageOrBuilder data) {
    return encodeToString(data).getBytes(CHARSET);
  }

  /**
   * Decodes the given byte array and returns the original proto payload.
   * @throws ParseException when incompatible
   */
  public static <T extends Message> T decode(byte[] code, Class<T> protoClass)
      throws ParseException {
    return TextFormat.parse(new String(code, CHARSET), protoClass);
  }

  @SuppressWarnings("unchecked")
  public static <T extends Message> T parseJson(String json, Class<T> protoClass)
      throws InvalidProtocolBufferException {
    T.Builder builder = Internal.getDefaultInstance(protoClass).toBuilder();
    JsonFormat.parser().merge(json, builder);
    return (T) builder.build();
  }

  public static <T extends Message> String encodeToJsonString(T proto)
      throws InvalidProtocolBufferException {
    return JsonFormat.printer().print(proto);
  }
}
