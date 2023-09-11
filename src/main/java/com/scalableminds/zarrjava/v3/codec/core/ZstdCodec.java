package com.scalableminds.zarrjava.v3.codec.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.scalableminds.zarrjava.ZarrException;
import com.scalableminds.zarrjava.v3.ArrayMetadata;
import com.scalableminds.zarrjava.v3.codec.BytesBytesCodec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import javax.annotation.Nonnull;

public class ZstdCodec implements BytesBytesCodec {

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
  public ByteBuffer decode(ByteBuffer chunkBytes, ArrayMetadata.CoreArrayMetadata arrayMetadata)
      throws ZarrException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); ZstdInputStream inputStream = new ZstdInputStream(
        new ByteArrayInputStream(chunkBytes.array()))) {
      copy(inputStream, outputStream);
      inputStream.close();
      return ByteBuffer.wrap(outputStream.toByteArray());
    } catch (IOException ex) {
      throw new ZarrException("Error in decoding zstd.", ex);
    }
  }

  @Override
  public ByteBuffer encode(ByteBuffer chunkBytes, ArrayMetadata.CoreArrayMetadata arrayMetadata)
      throws ZarrException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); ZstdOutputStream zstdStream = new ZstdOutputStream(
        outputStream, configuration.level).setChecksum(
        configuration.checksum)) {
      zstdStream.write(chunkBytes.array());
      zstdStream.close();
      return ByteBuffer.wrap(outputStream.toByteArray());
    } catch (IOException ex) {
      throw new ZarrException("Error in decoding zstd.", ex);
    }
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

