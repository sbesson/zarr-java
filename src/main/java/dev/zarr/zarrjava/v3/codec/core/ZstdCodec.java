package dev.zarr.zarrjava.v3.codec.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import dev.zarr.zarrjava.ZarrException;
import dev.zarr.zarrjava.utils.Utils;
import dev.zarr.zarrjava.v3.ArrayMetadata;
import dev.zarr.zarrjava.v3.codec.BytesBytesCodec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import javax.annotation.Nonnull;

public class ZstdCodec extends BytesBytesCodec {

  public final String name = "zstd";
  @Nonnull
  public final Configuration configuration;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public ZstdCodec(
      @Nonnull @JsonProperty(value = "configuration", required = true) Configuration configuration) {
    this.configuration = configuration;
  }

  private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
    byte[] buffer = new byte[4096];
    int len;
    while ((len = inputStream.read(buffer)) > 0) {
      outputStream.write(buffer, 0, len);
    }
  }

  @Override
  public ByteBuffer decode(ByteBuffer compressedBytes) throws ZarrException {
    // Extract the byte array from the ByteBuffer
    byte[] compressedArray = new byte[compressedBytes.remaining()];
    compressedBytes.get(compressedArray);

    // Determine the original size (optional: you might need to store the original size separately)
    long originalSize = Zstd.decompressedSize(compressedArray);
    if (originalSize == 0) {
      throw new ZarrException("Failed to get decompressed size");
    }

    // Create a buffer for the decompressed data
    byte[] decompressed = new byte[(int) originalSize];

    // Perform decompression
    long bytesDecompressed = Zstd.decompress(decompressed, compressedArray);
    if (bytesDecompressed != originalSize) {
      throw new ZarrException("Decompression failed, incorrect decompressed size");
    }

    // Wrap the decompressed byte array into a ByteBuffer
    return ByteBuffer.wrap(decompressed);
  }

  @Override
  public ByteBuffer encode(ByteBuffer chunkBytes) throws ZarrException {
    byte[] arr = chunkBytes.array();
    byte[] compressed;
    try (ZstdCompressCtx ctx = new ZstdCompressCtx()) {
      ctx.setLevel(configuration.level);
      ctx.setChecksum(configuration.checksum);
      compressed = ctx.compress(arr);
    }
    return ByteBuffer.wrap(compressed);
  }

  @Override
  public long computeEncodedSize(long inputByteLength,
      ArrayMetadata.CoreArrayMetadata arrayMetadata) throws ZarrException {
    throw new ZarrException("Not implemented for Zstd codec.");
  }

  public static final class Configuration {

    public final int level;
    public final boolean checksum;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Configuration(@JsonProperty(value = "level", defaultValue = "5") int level,
        @JsonProperty(value = "checksum", defaultValue = "true") boolean checksum)
        throws ZarrException {
      if (level < -131072 || level > 22) {
        throw new ZarrException("'level' needs to be between -131072 and 22.");
      }
      this.level = level;
      this.checksum = checksum;
    }
  }
}


