package com.hari.androidtvremote.androidLib.remote;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.protobuf.CodedInputStream;
import java.io.IOException;
import java.util.HexFormat;
import org.junit.Test;

/**
 * Golden Remote v2 protobuf wire frames (length-delimited), independently
 * constructed from the schema and send_text() in tronikos/androidtvremote2.
 * These are serialization tests, not proof that a real TV accepts text.
 */
public class RemoteImeWireTest {
    private final RemoteMessageManager manager = new RemoteMessageManager();

    @Test
    public void fullTextIsExactlyOneImeBatchWithZeroCounters() {
        assertArrayEquals(
                HexFormat.of().parseHex("1daa011a1a1808011214080d100d1a0e546573746520416e795456203237"),
                manager.createImeBatchEditCompatV2("Teste AnyTV 27", 0, 0));
    }

    @Test
    public void usesRemoteCountersAndUtf8ByteLength() {
        assertArrayEquals(
                HexFormat.of().parseHex("19aa0116080310091a100801120c080310031a0661c3a7c3a36f"),
                manager.createImeBatchEditCompatV2("ação", 3, 9));
    }

    @Test
    public void longInputUsesVarintLengthFraming() throws IOException {
        byte[] frame = manager.createImeBatchEditCompatV2("X".repeat(150), 1, 2);
        CodedInputStream coded = CodedInputStream.newInstance(frame);
        int declaredLength = coded.readRawVarint32();
        int prefixLength = coded.getTotalBytesRead();
        assertEquals(177, frame.length);
        assertEquals(frame.length - prefixLength, declaredLength);
        assertEquals((21 << 3) | 2, coded.readTag()); // only remote_ime_batch_edit
        assertEquals(declaredLength - (coded.getTotalBytesRead() - prefixLength),
                coded.readByteArray().length); // one batch, no extra key injection
        assertEquals(0, coded.readTag());
    }
}
