import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.LexerATNSimulator;

public class SourceCodeLexerBenchmark {

	static String basedir = "/home/mdakin/tmp/codebases/";

	private static final BaseErrorListener IGNORING_ERROR_LISTENER = new BaseErrorListener() {
		@Override
		public void syntaxError(
				Recognizer<?, ?> recognizer,
				Object offendingSymbol,
				int line,
				int charPositionInLine,
				String msg,
				RecognitionException e) {
			// Just ignore the error.
		}
	};

	class Stats {
		String fileName;
		String grammar;

		long fileSize;
		long startTime;
		long endTime;
		long elapsedMillis;
		long[] runTimes;
		long charCount;
		long asciiCharCount;
		long totalTokens;
		double asciiRatio;
		int[] tokenTypes;
		int[] tokenLengths;
		int emptyTokens;

		public Stats(String filename, String grammar) {
			this.fileName = filename;
			this.grammar = grammar;
			this.tokenTypes = new int[500];
			this.tokenLengths = new int[500];
			init();
		}

		void init() {
			startTime = System.currentTimeMillis();
		}

		void end() {
			endTime = System.currentTimeMillis();
			elapsedMillis = endTime - startTime;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("File: " ).append(fileName).append('\n');
			sb.append("Lexer Grammar: " ).append(grammar).append('\n');
			if (charCount > 0) {
				sb.append("Total chars read: ").append(charCount).append('\n');
				sb.append("Total chars (ASCII): ").append(asciiCharCount).append('\n');
				sb.append("Total chars (non ASCII): ")
						.append(charCount - asciiCharCount).append('\n');
				sb.append("Char mix: ")
						.append(String.format("Ascii pct: %.3f", (100.0 * asciiCharCount / charCount)))
						.append('\n');
			}
			sb.append("Token count: ").append(totalTokens).append('\n');;
			sb.append("Tokens per second: ")
					.append(String.format("%.2f", totalTokens * 1000.0 / elapsedMillis)).append('\n');
			sb.append("Time: ").append(elapsedMillis).append("ms.").append('\n');;
			return sb.toString();
		}

		public void update(Token token) {
			totalTokens++;
			// Count token types
//			tokenTypes[Math.min(token.getType(), tokenTypes.length-1)]++;
//			// Count token sizes
//			String text = token.getText();
//			tokenLengths[Math.min(text.length(), tokenLengths.length-1)]++;
//			// Count empty tokens
//			if (text.matches("\\s+")) {
//				emptyTokens++;
//			}
		}

		public void update(int c) {
			charCount++;
			if (c < 128) {
				asciiCharCount++;
			}
		}
	}

	private ANTLRInputStream createStream(String filename) throws IOException {
		File f = new File(filename);
		return  new ANTLRFileStream(f.getAbsolutePath());
	}

	public TurkishLexer createTrLexer(String fileName) throws IOException {
		TurkishLexer lexer = new TurkishLexer(createStream(fileName));
		lexer.removeErrorListeners();
		lexer.addErrorListener(IGNORING_ERROR_LISTENER);
		return lexer;
	}

	public Java8Lexer createJava8Lexer(String fileName) throws IOException {
		Java8Lexer lexer = new Java8Lexer(createStream(fileName));
		lexer.removeErrorListeners();
		lexer.addErrorListener(IGNORING_ERROR_LISTENER);
    return lexer;
	}

	public CLexer createCLexer(String fileName) throws IOException {
		CLexer lexer = new CLexer(createStream(fileName));
		lexer.removeErrorListeners();
		lexer.addErrorListener(IGNORING_ERROR_LISTENER);
		return lexer;
	}

	public CPP14Lexer createCpp14Lexer(String fileName) throws IOException {
		CPP14Lexer lexer = new CPP14Lexer(createStream(fileName));
		lexer.removeErrorListeners();
		lexer.addErrorListener(IGNORING_ERROR_LISTENER);
		return lexer;
	}

	Stats lex(String filename, Lexer lexer) throws IOException {
		Stats stats = new Stats(filename, lexer.getGrammarFileName());
		for (Token token = lexer.nextToken();
				token.getType() != Token.EOF;
				token = lexer.nextToken()) {
			stats.update(token);
		}
		stats.end();
		return stats;
	}

	void updateFileStats(Stats stats, String fileName) throws Exception {
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileName));
		InputStreamReader ir = new InputStreamReader(bis, StandardCharsets.UTF_8);
		while (true) {
			int c = ir.read();
			if (c < 0) break;
			stats.update(c);
		}
	}

	Stats benchmark(String fileName, Lexer lexer) throws Exception {
		Stats stats = lex(fileName, lexer);
//		updateFileStats(stats, fileName);
		System.out.println(stats);
//		System.out.println(lexer.getInterpreter());
		return stats;
	}

	public static void main(String[] args) throws Exception {
		SourceCodeLexerBenchmark bench = new SourceCodeLexerBenchmark();

		String testFile;

		testFile = basedir + "linux_kernel_4.13_100MB";
		CLexer cLexer = bench.createCLexer(testFile);
		bench.benchmark(testFile, cLexer);

		testFile = basedir + "jdk8";
		Java8Lexer java8Lexer = bench.createJava8Lexer(testFile);
		bench.benchmark(testFile, java8Lexer);
//
//   	    testFile = basedir + "java_guava_23.0";
//		Java8Lexer java8Lexer2 = bench.createJava8Lexer(testFile);
//		bench.benchmark(testFile, java8Lexer2);
//
//		testFile = basedir + "skia";
//		CPP14Lexer cppLexer = bench.createCpp14Lexer(testFile);
//		bench.benchmark(testFile, cppLexer);

//		String strbasedir = "/home/mdakin/tmp/reduced/";
//		testFile = strbasedir + "www.cnnturk.com.corpus";
//		TurkishLexer trLexer = bench.createTrLexer(testFile);
//		bench.benchmark(testFile, trLexer);

	}

}
