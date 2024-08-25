package me.clipi.io.util;

import org.jetbrains.annotations.NotNull;

public interface NestedToString {
	@NotNull
	default String nestedToString() {
		Nester nester = new Nester();
		nester.append(this);
		return nester.finish();
	}

	void toString(@NotNull Nester nester);

	class Nester {
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
				if (obj instanceof String) {
					str.append('"').append(obj).append('"');
					return;
				} else if (obj.getClass().isPrimitive()) {
					str.append(obj);
					return;
				}

				String name = obj.getClass().getSimpleName();
				if (!name.isEmpty()) str.append(name).append(' ');
				if (obj instanceof NestedToString) {
					appendComposite('{', '}', () -> ((NestedToString) obj).toString(this));
					return;
				} else if (obj instanceof Iterable) {
					appendComposite('[', ']', () -> ((Iterable<?>) obj).forEach(o -> {
						nlTabs();
						append(o);
						str.append(',');
					}));
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
	}
}
