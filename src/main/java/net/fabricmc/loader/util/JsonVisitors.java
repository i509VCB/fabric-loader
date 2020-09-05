package net.fabricmc.loader.util;

import java.util.Map;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

public final class JsonVisitors {
	public static void accept(JsonObject object, ObjectVisitor visitor) {
		for (Map.Entry<String, Object> entry : object.entrySet()) {
			final String key = entry.getKey();
			final Object value = entry.getValue();

			if (value instanceof JsonObject) {
				final ObjectVisitor objectVisitor = visitor.visitObject(key);

				if (objectVisitor != null) {
					JsonVisitors.accept(((JsonObject) value), objectVisitor);
				}
			} else if (value instanceof JsonArray) {
				final ArrayVisitor arrayVisitor = visitor.visitArray(key);

				if (arrayVisitor != null) {
					JsonVisitors.accept(((JsonArray) value), arrayVisitor);
				}
			} else if (value instanceof Boolean) {
				visitor.visitBoolean(key, ((Boolean) value));
			} else if (value instanceof Number) {
				visitor.visitNumber(key, ((Number) value));
			} else if (value instanceof String) {
				visitor.visitString(key, ((String) value));
			} else if (value == null) {
				visitor.visitNull(key);
			}
		}
	}

	public static void accept(JsonArray array, ArrayVisitor visitor) {
		for (int i = 0; i < array.size(); i++) {
			final Object value = array.get(i);

			if (value instanceof JsonObject) {
				final ObjectVisitor objectVisitor = visitor.visitObject(i);

				if (objectVisitor != null) {
					JsonVisitors.accept(((JsonObject) value), objectVisitor);
				}
			} else if (value instanceof JsonArray) {
				final ArrayVisitor arrayVisitor = visitor.visitArray(i);

				if (arrayVisitor != null) {
					JsonVisitors.accept(((JsonArray) value), arrayVisitor);
				}
			} else if (value instanceof Boolean) {
				visitor.visitBoolean(i, ((Boolean) value));
			} else if (value instanceof Number) {
				visitor.visitNumber(i, ((Number) value));
			} else if (value instanceof String) {
				visitor.visitString(i, ((String) value));
			} else if (value == null) {
				visitor.visitNull(i);
			}
		}
	}

	public interface ObjectVisitor {
		/* @Nullable */
		default ObjectVisitor visitObject(String key) {
			return null;
		}

		/* @Nullable */
		default ArrayVisitor visitArray(String key) {
			return null;
		}

		default void visitBoolean(String key, boolean value) {
		}

		default void visitNumber(String key, Number number) {
		}

		default void visitString(String key, String value) {
		}

		default void visitNull(String key) {
		}

		default void pop() {
		}
	}

	public interface ArrayVisitor {
		/* @Nullable */
		default ObjectVisitor visitObject(int idx) {
			return null;
		}

		/* @Nullable */
		default ArrayVisitor visitArray(int idx) {
			return null;
		}

		default void visitBoolean(int idx, boolean value) {
		}

		default void visitNumber(int idx, Number number) {
		}

		default void visitString(int idx, String value) {
		}

		default void visitNull(int idx) {
		}

		default void pop() {
		}
	}

	private JsonVisitors() {
	}
}
