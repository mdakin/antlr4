/*
 * A simple lexer grammar for Turkish texts.
 *
 */
lexer grammar TurkishLexer;

options {
  language = Java;
}

fragment Digit: [0-9];

// Letters
fragment TurkishLetters
    : [a-zçğıöşüâîû];

fragment TurkishLettersCapital
    : [A-ZÇĞİÖŞÜÂÎÛ];

fragment TurkishLettersAll
    : [a-zA-ZçğıöşüâîûÇĞİÖŞÜÂÎÛ];

fragment AllTurkishAlphanumerical
    : [0-9a-zA-ZçğıöşüâîûÇĞİÖŞÜÂÎÛ-];

fragment Apostrophe: ('\''|'’');

fragment DoubleQuote: ('"'|'”'|'“'|'»'|'«');

// 'lerin
fragment AposAndSuffix: Apostrophe TurkishLettersAll+;

SpaceTab
    : [ \t]+;
NewLine
    : [\n\r];

Time
    : [0-2][0-9] (':'|'.') [0-5][0-9] ((':'|'.') [0-5][0-9])? AposAndSuffix? ;

Date
    :([0-3]?[0-9] '.' [0-1]?[0-9] '.' ([1][7-9][0-9][0-9]|[2][0][0-9][0-9]|[0-9][0-9]) AposAndSuffix?)|
    ([0-3]?[0-9] '/' [0-1]?[0-9] '/' ([1][7-9][0-9][0-9]|[2][0][0-9][0-9]|[0-9][0-9]) AposAndSuffix?);

PercentNumeral
    : '%' Number;

Number
    : [+\-]? Integer [.,] Integer Exp? AposAndSuffix? // -1.35, 1.35E-9, 3,1'e
    | [+\-]? Integer Exp AposAndSuffix?     // 1e10 -3e4 1e10'dur
    | [+\-]? Integer AposAndSuffix?         // -3, 45
    | (Integer '.')+ Integer AposAndSuffix? // 1.000.000
    | (Integer ',')+ Integer AposAndSuffix? // 2,345,531
    | Integer '.'? AposAndSuffix?           // Ordinal 2. 34.
    ;

// Not really an integer as it can have zeroes at the start but this is ok.
fragment Integer
    : Digit+ ;

fragment Exp
    : [Ee] [+\-]? Integer ;

fragment URLFragment
    : [0-9a-zA-ZçğıöşüâîûÇĞİÖŞÜÂÎÛ\\-_]+;
fragment URLFragmentWithDot
    :'.'[0-9a-zA-ZçğıöşüâîûÇĞİÖŞÜÂÎÛ\\-_/?&+;=]+;

URL :
    ('http://'|'https://')? 'www.' URLFragment URLFragmentWithDot+;

Email
    :AllTurkishAlphanumerical+ '.'? AllTurkishAlphanumerical+ '@'
    (AllTurkishAlphanumerical+ '.' AllTurkishAlphanumerical+)+ ;

HashTag: '#' AllTurkishAlphanumerical+;

Mention: '@' AllTurkishAlphanumerical+;

// Only a subset.
// TODO: Add more, also consider Emoji tokens.
Emoticon
    : ':)'|':-)'|':-]'|':D'|':-D'|'8-)'|';)'|';‑)'|':('|':-('|':\'('
    |':‑/'|':/'|':^)'|'¯\\_(ツ)_/¯'|'O_o'|'o_O'|'O_O'|'\\o/';

// Roman numbers:
RomanNumeral
    : ('I'|'II'|'III'|'IV'|'V'|'VI'|'VII'|'VIII'|'IX') '.'? AposAndSuffix? ;

// I.B.M.
AbbreviationWithDots
    : (TurkishLettersCapital '.')+ TurkishLettersCapital? AposAndSuffix?;

// Merhaba kedi
Word
    : TurkishLettersAll+;

// Ahmet'in
WordWithApostrophe
    : AllTurkishAlphanumerical+ AposAndSuffix?;

Punctuation
    :  Apostrophe | DoubleQuote | '‘' | '…' | '...' | '(!)' | '(?)'| [.,!?%$&*+@:;]
    | '\\' | '-'  | '(' | ')' | '[' | ']' | '{' | '}';

UnknownWord
    : ~([ \n\r\t.,!?%$&*+@:;] | '\'' | '’' | '‘' | '"' | '”' | '“' | '»' | '«'
    |'\\' | '-' |'(' | ')' | '[' | ']' | '{' | '}')+;

// Catch all remaining as Unknown.
Unknown : .+? ;