package net.fabricmc.loader.util;

import java.io.PrintStream;

public interface LoggingInterface {
	default void info(String message) {
		this.info(message, null);
	}

	void info(String message, Throwable t);

	default void warn(String message) {
		this.warn(message, null);
	}

	void warn(String message, Throwable t);

	default void error(String message) {
		this.error(message, null);
	}

	void error(String message, Throwable t);

	default void debug(String message) {
		this.debug(message, null);
	}

	void debug(String message, Throwable t);

	class Delegated implements LoggingInterface {
		private LoggingInterface delegate;

		public Delegated(LoggingInterface delegate) {
			this.delegate = delegate;
		}

		public void setLogger(LoggingInterface logger) {
			this.delegate = logger;
		}

		@Override
		public void info(String message, Throwable t) {
			this.delegate.info(message, t);
		}

		@Override
		public void warn(String message, Throwable t) {
			this.delegate.warn(message, t);
		}

		@Override
		public void error(String message, Throwable t) {
			this.delegate.error(message, t);
		}

		@Override
		public void debug(String message, Throwable t) {
			this.delegate.debug(message, t);
		}
	}

	class Stdout implements LoggingInterface {
		private final PrintStream out = System.out;
		private final PrintStream err = System.err;
		private final MessageTemplate template;
		private final String prefix;
		private final boolean debugEnabled;

		public Stdout(MessageTemplate template, String prefix, boolean debugEnabled) {
			this.template = template;
			this.prefix = prefix;
			this.debugEnabled = debugEnabled;
		}

		@Override
		public void info(String message, Throwable t) {
			this.out.println(this.template.createMessage(LogType.INFO, this.prefix, message));

			if (t != null) {
				t.printStackTrace(this.out);
			}
		}

		@Override
		public void warn(String message, Throwable t) {
			this.out.println(this.template.createMessage(LogType.WARN, this.prefix, message));

			if (t != null) {
				t.printStackTrace(this.out);
			}
		}

		@Override
		public void error(String message, Throwable t) {
			this.err.println(this.template.createMessage(LogType.ERROR, this.prefix, message));

			if (t != null) {
				t.printStackTrace(this.err);
			}
		}

		@Override
		public void debug(String message, Throwable t) {
			if (this.debugEnabled) {
				this.out.println(this.template.createMessage(LogType.DEBUG, this.prefix, message));

				if (t != null) {
					t.printStackTrace(this.out);
				}
			}
		}
	}

	enum LogType {
		INFO("info"),
		WARN("warn"),
		ERROR("error"),
		DEBUG("debug");

		private final String name;

		LogType(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}

	interface MessageTemplate {
		String createMessage(LogType logType, String prefix, String message);
	}
}
