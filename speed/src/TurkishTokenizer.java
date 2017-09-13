import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.dfa.DFAState;

import java.io.*;
import java.nio.charset.StandardCharsets;

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

	private static TurkishLexer lexerInstance(ANTLRInputStream inputStream) {
		TurkishLexer lexer = new TurkishLexer(inputStream);
		lexer.removeErrorListeners();
		lexer.addErrorListener(IGNORING_ERROR_LISTENER);
		return lexer;
	}

	private static TurkishLexerAscii lexerInstanceAscii(ANTLRInputStream inputStream) {
		TurkishLexerAscii lexer = new TurkishLexerAscii(inputStream);
		lexer.removeErrorListeners();
		lexer.addErrorListener(IGNORING_ERROR_LISTENER);
		return lexer;
	}

	static long tokens = 0;

	private static void getAllTokens(Lexer lexer) {
		tokens = 0;
		long[] tokenCounts = new long[20];

		for (Token token = lexer.nextToken();
  		    token.getType() != Token.EOF;
			token = lexer.nextToken()) {
			tokens++;
//			int type = token.getType();
//			tokenCounts[type]++;
		}

		System.out.println("Total tokens:" + tokens);
//		for (int i=0; i<tokenCounts.length; i++) {
//			System.out.println("Token type: " + i + " Count:" + tokenCounts[i]);
//		}
	}

	static int[] asc = new int[1000];
	static {
		asc['ç'] = 'c';
		asc['ğ'] = 'g';
		asc['ı'] = 'i';
		asc['ö'] = 'o';
		asc['ş'] = 's';
		asc['ü'] = 'u';
		asc['Ç'] = 'C';
		asc['Ğ'] = 'G';
		asc['İ'] = 'I';
		asc['Ö'] = 'O';
		asc['Ş'] = 'S';
		asc['Ü'] = 'U';
	}

	static int asciify(int input) {
		if (input < 128) return input;
		// Asciify Turkish chars
		if (input < 1000 && asc[input] != 0) {
			return asc[input];
		}
		// Else just return x
		return 'x';
	}

	public static void main(String[] args) throws IOException {


		File f = new File("/home/mdakin/IdeaProjects/antlr4/speed/resources/corpus1_ascii_50M");


	//	File f = new File("/home/mdakin/IdeaProjects/antlr4/speed/resources/corpus1_50M");
		for (int i=0; i<10; i++) {
			ANTLRInputStream inputStream = new ANTLRFileStream(f.getAbsolutePath());
			long startTime = System.currentTimeMillis();
			//getAllTokens(lexerInstance(inputStream));
			TurkishLexerAscii lexer = lexerInstanceAscii(inputStream);
			getAllTokens(lexer);
			long elapsedMillis = System.currentTimeMillis() - startTime;
//			LexerATNSimulator interpreter = lexer.getInterpreter();
//			System.out.println(interpreter);
			System.out.println("Total time: " + elapsedMillis + "ms.");
			System.out.printf("Tokens per second: %.2f\n", tokens * 1000.0 / elapsedMillis);
		}

//		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
//		InputStreamReader ir = new InputStreamReader(bis, StandardCharsets.UTF_8);
//
//      FileOutputStream fos = new FileOutputStream("/home/mdakin/IdeaProjects/antlr4/speed/resources/corpus1_ascii_50M");
//		BufferedOutputStream bos = new BufferedOutputStream(fos);
//		while (true) {
//			int c = ir.read();
//			if (c < 0) break;
//			bos.write(asciify(c));
//		}
//		fos.close();

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
