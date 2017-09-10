import org.antlr.v4.runtime.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TurkishTokenizer {

	private static final BaseErrorListener IGNORING_ERROR_LISTENER = new BaseErrorListener() {
		@Override
		public void syntaxError(
				Recognizer<?, ?> recognizer,
				Object offendingSymbol,
				int line,
				int charPositionInLine,
				String msg,
				RecognitionException e) {
			System.out.printf("Unknown token. Original error: %s ", msg);
		}
	};

	public static void tokenize(File file) throws IOException {
		ANTLRInputStream inputStream = new ANTLRFileStream(file.getAbsolutePath());
		getAllTokens(lexerInstance(inputStream));
	}

	private static TurkishLexer lexerInstance(ANTLRInputStream inputStream) {
		TurkishLexer lexer = new TurkishLexer(inputStream);
		lexer.removeErrorListeners();
		lexer.addErrorListener(IGNORING_ERROR_LISTENER);
		return lexer;
	}

	static long tokens = 0;
	private static void getAllTokens(Lexer lexer) {
		long[] tokenCounts = new long[20];

		for (Token token = lexer.nextToken();
  		    token.getType() != Token.EOF;
			token = lexer.nextToken()) {
			tokens++;
			int type = token.getType();
			tokenCounts[type]++;
		}

		System.out.println("Total tokens:" + tokens);
		for (int i=0; i<tokenCounts.length; i++) {
			System.out.println("Token type: " + i + " Count:" + tokenCounts[i]);
		}

	}

	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();
		File f = new File("/home/mdakin/IdeaProjects/antlr4/speed/resources/corpus1_50M");
  	    tokenize(f);
		long elapsedMillis = System.currentTimeMillis() - startTime;
		System.out.println("Total time: " + elapsedMillis + "ms.");
		System.out.printf("Tokens per second: %.2f", tokens * 1000.0 / elapsedMillis);

//		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
//		InputStreamReader ir = new InputStreamReader(bis, StandardCharsets.UTF_8);
//		long total = 0;
//		long ascii = 0;
//		while (true) {
//			int c = ir.read();
//			if (c < 0) break;
//			total++;
//			if (c < 128) ascii++;
//		}
//		System.out.println("Total: " + total + " Ascii: " + ascii);
//		System.out.printf("Percent: %.2f", (100.0 * ascii / total));

	}
}
