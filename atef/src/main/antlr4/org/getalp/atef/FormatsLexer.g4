lexer grammar FormatsLexer;

// Grammaire composée des différents langages d'ARIANE G5
import CommonLexer;

// Comments are different in Formats
COMMENTAIRE_AG5	:   '**' ( ~ '.')* '.' -> skip;

EGEG			:	'==' ( ~ '.')* '.**' 	;

