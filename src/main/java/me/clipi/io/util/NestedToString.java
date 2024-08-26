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

import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;

public interface NestedToString {
	@NotNull
	default String nestedToString() {
		Nester nester = new Nester();
		nester.append(this);
		return nester.finish();
	}

	void toString(@NotNull Nester nester);

	class Nester {
		private boolean explicitType = true;
		private int depth;
		private final StringBuilder str = new StringBuilder();

		/**
		 * @return this
		 */
		public Nester append(Object key, Object val) {
			nlTabs();
			if (key instanceof String) {
				str.append(key);
			} else {
				append(key);
			}
			str.append(": ");
			append(val);
			str.append(',');
			return this;
		}

		private void append(Object obj) {
			if (obj != null) {
				Class<?> objClass = obj.getClass();
				if (obj instanceof String) {
					str.append('"').append(obj).append('"');
					return;
				} else {
					Class<?> prim = MethodType.methodType(objClass).unwrap().returnType();
					if (!objClass.equals(prim)) {
						if (explicitType) str.append(prim.getSimpleName()).append(' ');
						str.append(obj);
						return;
					}
				}

				if (explicitType) {
					String name = objClass.getSimpleName();
					if (!name.isEmpty()) str.append(name).append(' ');
				}
				if (obj instanceof NestedToString) {
					appendComposite('{', '}', () -> ((NestedToString) obj).toString(this));
					return;
				} else if (obj instanceof Iterable | objClass.isArray()) {
					Iterable<?> iter;
					if (objClass.isArray()) {
						if (Modifier.isFinal(objClass.getComponentType().getModifiers()))
							explicitType = false;
						iter = BoxedArrayIterable.infer(obj);
					} else {
						iter = (Iterable<?>) obj;
					}
					appendComposite('[', ']', () -> iter.forEach(o -> {
						nlTabs();
						append(o);
						str.append(',');
					}));
					explicitType = true;
					return;
				}
			}

			str.append(obj);
		}

		private void appendComposite(char open, char close, @NotNull Runnable writeInner) {
			str.append(open);
			int lengthBefore = str.length();
			++depth;
			writeInner.run();
			--depth;
			if (str.length() == lengthBefore) {
				str.append(' ');
			} else {
				str.deleteCharAt(str.length() - 1);
				nlTabs();
			}
			str.append(close);
		}

		private void nlTabs() {
			assert str.length() != 0;
			str.append('\n');
			int i = depth;
			str.ensureCapacity(str.length() + i);
			for (; i > 0; --i)
				str.append('\t');
		}

		@NotNull
		private String finish() {
			assert depth == 0;
			return str.toString();
		}

		@Override
		public String toString() {
			return str.toString();
		}
	}
}
