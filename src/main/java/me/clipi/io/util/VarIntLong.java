/*
 * MinecraftIO, a simple library with multiple Minecraft IO-tools
 * Copyright (C) 2024  Clipi (GitHub: Clipi-12)
 *
 * This file is part of MinecraftIO.
 *
 * MinecraftIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MinecraftIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MinecraftIO.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.clipi.io.util;

import org.jetbrains.annotations.NotNull;

/**
 * @see <a href="https://wiki.vg/VarInt_And_VarLong">VarInt and VarLong</a>
 */
public class VarIntLong {
	public static class ParseVarIntLongException extends Exception {
		private static final long serialVersionUID = 5275320250886376696L;

		private ParseVarIntLongException(@NotNull String decodedClass, boolean enough) {
			super("Couldn't parse the specified bytes as a " + decodedClass + " because there were" +
				  (enough ? " too many bytes for a single " + decodedClass : "n't enough bytes provided"));
		}
	}

	@FunctionalInterface
	public interface VarIntConsumer {
		boolean acceptAndContinue(int value, int index);
	}

	@FunctionalInterface
	public interface VarLongConsumer {
		boolean acceptAndContinue(long value, int index);
	}

	public static void parseVarInts(byte @NotNull [] bytes, @NotNull VarIntConsumer nextInt) throws ParseVarIntLongException {
		int byteIdx = 0, idx = 0;

		int len = bytes.length;
		// Bulk check all maximum VarInt size bytes to avoid doing it every byte
		while (byteIdx + 4 < len) {
			// Unfold loop to avoid checking for the "too many bytes" parsing exception

			byte b0 = bytes[byteIdx++];
			if ((b0 & 128) == 0) {
				if (nextInt.acceptAndContinue((b0 & 127), idx++)) continue;
				return;
			}

			// <editor-fold defaultstate="collapsed" desc="bytes[1]">
			byte b1 = bytes[byteIdx++];
			if ((b1 & 128) == 0) {
				if (nextInt.acceptAndContinue(((b1 & 127) << 7) |
											  (b0 & 127), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[2]">
			byte b2 = bytes[byteIdx++];
			if ((b2 & 128) == 0) {
				if (nextInt.acceptAndContinue(((b2 & 127) << 14) |
											  ((b1 & 127) << 7) |
											  (b0 & 127), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[3]">
			byte b3 = bytes[byteIdx++];
			if ((b3 & 128) == 0) {
				if (nextInt.acceptAndContinue(((b3 & 127) << 21) |
											  ((b2 & 127) << 14) |
											  ((b1 & 127) << 7) |
											  (b0 & 127), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[4]">
			byte b4 = bytes[byteIdx++];
			if ((b4 & 128) == 0) {
				if (nextInt.acceptAndContinue(((b4 & 127) << 28) |
											  ((b3 & 127) << 21) |
											  ((b2 & 127) << 14) |
											  ((b1 & 127) << 7) |
											  (b0 & 127), idx++)) continue;
				return;
			}
			// </editor-fold>

			throw new ParseVarIntLongException("VarInt", true);
		}

		while (byteIdx < len) {
			// Unfold loop to avoid checking for the "too many bytes" parsing exception

			byte b0 = bytes[byteIdx++];
			if ((b0 & 128) == 0) {
				if (nextInt.acceptAndContinue((b0 & 127), idx++)) continue;
				return;
			}

			// <editor-fold defaultstate="collapsed" desc="bytes[1]">
			if (byteIdx >= len) throw new ParseVarIntLongException("VarInt", false);
			byte b1 = bytes[byteIdx++];
			if ((b1 & 128) == 0) {
				if (nextInt.acceptAndContinue(((b1 & 127) << 7) |
											  (b0 & 127), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[2]">
			if (byteIdx >= len) throw new ParseVarIntLongException("VarInt", false);
			byte b2 = bytes[byteIdx++];
			if ((b2 & 128) == 0) {
				if (nextInt.acceptAndContinue(((b2 & 127) << 14) |
											  ((b1 & 127) << 7) |
											  (b0 & 127), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[3]">
			if (byteIdx >= len) throw new ParseVarIntLongException("VarInt", false);
			byte b3 = bytes[byteIdx++];
			if ((b3 & 128) == 0) {
				if (nextInt.acceptAndContinue(((b3 & 127) << 21) |
											  ((b2 & 127) << 14) |
											  ((b1 & 127) << 7) |
											  (b0 & 127), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[4]">
			if (byteIdx >= len) throw new ParseVarIntLongException("VarInt", false);
			byte b4 = bytes[byteIdx++];
			if ((b4 & 128) == 0) {
				if (nextInt.acceptAndContinue(((b4 & 127) << 28) |
											  ((b3 & 127) << 21) |
											  ((b2 & 127) << 14) |
											  ((b1 & 127) << 7) |
											  (b0 & 127), idx++)) continue;
				return;
			}
			// </editor-fold>

			throw new ParseVarIntLongException("VarInt", true);
		}
	}

	public static void parseVarLongs(byte @NotNull [] bytes, @NotNull VarLongConsumer nextLong) throws ParseVarIntLongException {
		int byteIdx = 0, idx = 0;

		int len = bytes.length;
		// Bulk check all maximum VarLong size bytes to avoid doing it every byte
		while (byteIdx + 9 < len) {
			// Unfold loop to avoid checking for the "too many bytes" parsing exception

			byte b0 = bytes[byteIdx++];
			if ((b0 & 128) == 0) {
				if (nextLong.acceptAndContinue((b0 & 127), idx++)) continue;
				return;
			}

			// <editor-fold defaultstate="collapsed" desc="bytes[1]">
			byte b1 = bytes[byteIdx++];
			if ((b1 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b1 & 127) << 7) |
											   (b0 & 127), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[2]">
			byte b2 = bytes[byteIdx++];
			if ((b2 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b2 & 127) << 14) |
											   ((b1 & 127) << 7) |
											   (b0 & 127), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[3]">
			byte b3 = bytes[byteIdx++];
			if ((b3 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b3 & 127) << 21) |
											   ((b2 & 127) << 14) |
											   ((b1 & 127) << 7) |
											   (b0 & 127), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[4]">
			byte b4 = bytes[byteIdx++];
			if ((b4 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b4 & 127L) << 28) |
											   ((b3 & 127L) << 21) |
											   ((b2 & 127L) << 14) |
											   ((b1 & 127L) << 7) |
											   (b0 & 127L), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[5]">
			byte b5 = bytes[byteIdx++];
			if ((b5 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b5 & 127L) << 35) |
											   ((b4 & 127L) << 28) |
											   ((b3 & 127L) << 21) |
											   ((b2 & 127L) << 14) |
											   ((b1 & 127L) << 7) |
											   (b0 & 127L), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[6]">
			byte b6 = bytes[byteIdx++];
			if ((b6 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b6 & 127L) << 42) |
											   ((b5 & 127L) << 35) |
											   ((b4 & 127L) << 28) |
											   ((b3 & 127L) << 21) |
											   ((b2 & 127L) << 14) |
											   ((b1 & 127L) << 7) |
											   (b0 & 127L), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[7]">
			byte b7 = bytes[byteIdx++];
			if ((b7 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b7 & 127L) << 49) |
											   ((b6 & 127L) << 42) |
											   ((b5 & 127L) << 35) |
											   ((b4 & 127L) << 28) |
											   ((b3 & 127L) << 21) |
											   ((b2 & 127L) << 14) |
											   ((b1 & 127L) << 7) |
											   (b0 & 127L), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[8]">
			byte b8 = bytes[byteIdx++];
			if ((b8 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b8 & 127L) << 56) |
											   ((b7 & 127L) << 49) |
											   ((b6 & 127L) << 42) |
											   ((b5 & 127L) << 35) |
											   ((b4 & 127L) << 28) |
											   ((b3 & 127L) << 21) |
											   ((b2 & 127L) << 14) |
											   ((b1 & 127L) << 7) |
											   (b0 & 127L), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[9]">
			byte b9 = bytes[byteIdx++];
			if ((b9 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b9 & 127L) << 63) |
											   ((b8 & 127L) << 56) |
											   ((b7 & 127L) << 49) |
											   ((b6 & 127L) << 42) |
											   ((b5 & 127L) << 35) |
											   ((b4 & 127L) << 28) |
											   ((b3 & 127L) << 21) |
											   ((b2 & 127L) << 14) |
											   ((b1 & 127L) << 7) |
											   (b0 & 127L), idx++)) continue;
				return;
			}
			// </editor-fold>

			throw new ParseVarIntLongException("VarLong", true);
		}
		while (byteIdx < len) {
			// Unfold loop to avoid checking for the "too many bytes" parsing exception

			byte b0 = bytes[byteIdx++];
			if ((b0 & 128) == 0) {
				if (nextLong.acceptAndContinue((b0 & 127), idx++)) continue;
				return;
			}

			// <editor-fold defaultstate="collapsed" desc="bytes[1]">
			if (byteIdx >= len) throw new ParseVarIntLongException("VarLong", false);
			byte b1 = bytes[byteIdx++];
			if ((b1 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b1 & 127) << 7) |
											   (b0 & 127), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[2]">
			if (byteIdx >= len) throw new ParseVarIntLongException("VarLong", false);
			byte b2 = bytes[byteIdx++];
			if ((b2 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b2 & 127) << 14) |
											   ((b1 & 127) << 7) |
											   (b0 & 127), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[3]">
			if (byteIdx >= len) throw new ParseVarIntLongException("VarLong", false);
			byte b3 = bytes[byteIdx++];
			if ((b3 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b3 & 127) << 21) |
											   ((b2 & 127) << 14) |
											   ((b1 & 127) << 7) |
											   (b0 & 127), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[4]">
			if (byteIdx >= len) throw new ParseVarIntLongException("VarLong", false);
			byte b4 = bytes[byteIdx++];
			if ((b4 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b4 & 127L) << 28) |
											   ((b3 & 127L) << 21) |
											   ((b2 & 127L) << 14) |
											   ((b1 & 127L) << 7) |
											   (b0 & 127L), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[5]">
			if (byteIdx >= len) throw new ParseVarIntLongException("VarLong", false);
			byte b5 = bytes[byteIdx++];
			if ((b5 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b5 & 127L) << 35) |
											   ((b4 & 127L) << 28) |
											   ((b3 & 127L) << 21) |
											   ((b2 & 127L) << 14) |
											   ((b1 & 127L) << 7) |
											   (b0 & 127L), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[6]">
			if (byteIdx >= len) throw new ParseVarIntLongException("VarLong", false);
			byte b6 = bytes[byteIdx++];
			if ((b6 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b6 & 127L) << 42) |
											   ((b5 & 127L) << 35) |
											   ((b4 & 127L) << 28) |
											   ((b3 & 127L) << 21) |
											   ((b2 & 127L) << 14) |
											   ((b1 & 127L) << 7) |
											   (b0 & 127L), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[7]">
			if (byteIdx >= len) throw new ParseVarIntLongException("VarLong", false);
			byte b7 = bytes[byteIdx++];
			if ((b7 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b7 & 127L) << 49) |
											   ((b6 & 127L) << 42) |
											   ((b5 & 127L) << 35) |
											   ((b4 & 127L) << 28) |
											   ((b3 & 127L) << 21) |
											   ((b2 & 127L) << 14) |
											   ((b1 & 127L) << 7) |
											   (b0 & 127L), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[8]">
			if (byteIdx >= len) throw new ParseVarIntLongException("VarLong", false);
			byte b8 = bytes[byteIdx++];
			if ((b8 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b8 & 127L) << 56) |
											   ((b7 & 127L) << 49) |
											   ((b6 & 127L) << 42) |
											   ((b5 & 127L) << 35) |
											   ((b4 & 127L) << 28) |
											   ((b3 & 127L) << 21) |
											   ((b2 & 127L) << 14) |
											   ((b1 & 127L) << 7) |
											   (b0 & 127L), idx++)) continue;
				return;
			}
			// </editor-fold>

			// <editor-fold defaultstate="collapsed" desc="bytes[9]">
			if (byteIdx >= len) throw new ParseVarIntLongException("VarLong", false);
			byte b9 = bytes[byteIdx++];
			if ((b9 & 128) == 0) {
				if (nextLong.acceptAndContinue(((b9 & 127L) << 63) |
											   ((b8 & 127L) << 56) |
											   ((b7 & 127L) << 49) |
											   ((b6 & 127L) << 42) |
											   ((b5 & 127L) << 35) |
											   ((b4 & 127L) << 28) |
											   ((b3 & 127L) << 21) |
											   ((b2 & 127L) << 14) |
											   ((b1 & 127L) << 7) |
											   (b0 & 127L), idx)) continue;
				return;
			}
			// </editor-fold>

			throw new ParseVarIntLongException("VarLong", true);
		}
	}
}
