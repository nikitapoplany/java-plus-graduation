package ru.practicum.ewm.stats.avro;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class AvroBytes {

    private AvroBytes() {
    }

    public static <T extends SpecificRecord> byte[] serialize(T value) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            SpecificDatumWriter<T> writer = new SpecificDatumWriter<>(value.getSchema());
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(value, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot serialize Avro record " + value.getClass().getSimpleName(), exception);
        }
    }

    public static <T extends SpecificRecord> T deserialize(byte[] bytes, Schema schema) {
        try {
            SpecificDatumReader<T> reader = new SpecificDatumReader<>(schema);
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(bytes, null);
            return reader.read(null, decoder);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot deserialize Avro record", exception);
        }
    }
}
