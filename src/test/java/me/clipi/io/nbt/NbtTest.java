package me.clipi.io.nbt;

import me.clipi.io.*;
import me.clipi.io.nbt.exceptions.NbtParseException;
import me.clipi.io.util.FixedStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

public class NbtTest {
	private static final ClassLoader cl = NbtTest.class.getClassLoader();

	@NotNull
	private static InputStream resource(@NotNull String resource) {
		return Objects.requireNonNull(cl.getResourceAsStream(resource));
	}

	private static String getString(@NotNull String resource) throws IOException {
		try (InputStream is = resource(resource)) {
			return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim().replace("\r\n", "\n");
		}
	}

	@NotNull
	private static GZIPInputStream gunzip(@NotNull InputStream is) {
		try {
			return new GZIPInputStream(is);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@NotNull
	@SuppressWarnings("unchecked")
	private static NbtParser<IOException> getParser(@NotNull String @NotNull ... resources) {
		return new NbtParser<>(new CheckedBigEndianDataInput<>(CheckedReader.concat(
			Arrays.stream(resources)
				  .map(NbtTest::resource)
				  .map(NbtTest::gunzip)
				  .map(CheckedReader::fromIs)
				  .toArray(CheckedReader[]::new)
		)));
	}

	@Test
	public void testAllTypes() throws IOException, OomException, NbtParseException,
									  EofException, NotEofException,
									  FixedStack.FullStackException {
		try (NbtParser<IOException> parser = getParser("nbt/all-types.nbt.gz")) {
			Assertions.assertEquals(getString("nbt/output-all-types.txt"), parser.parseRoot().toString());
		}
	}

	@Test
	public void testBigTest() throws IOException, OomException, NbtParseException,
									 EofException, NotEofException, FixedStack.FullStackException {
		try (NbtParser<IOException> parser = getParser("nbt/bigtest.nbt.gz")) {
			Assertions.assertEquals(getString("nbt/output-bigtest.txt"), parser.parseRoot().toString());
		}
	}

	@Test
	public void testMultipleInputs() throws IOException, OomException, NbtParseException, EofException,
											NotEofException,
											FixedStack.FullStackException {
		try (NbtParser<IOException> parser = getParser("nbt/all-types.nbt.gz", "nbt/bigtest.nbt.gz")) {
			Assertions.assertEquals(getString("nbt/output-all-types.txt"), parser.parseRoot().toString());
			Assertions.assertEquals(getString("nbt/output-bigtest.txt"), parser.parseRoot().toString());
		}
	}
}