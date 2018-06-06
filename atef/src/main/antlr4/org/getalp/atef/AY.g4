parser grammar AY;

options 
{
	tokenVocab=Lex;
}

// Grammaire composée des différents langages d'ARIANE G5
import ATEFgrammaire, // Format, //Variable,
  ATEFdictionnaire
	//, SYGMORgrammaire, SYGMORdictionnaire 
	//, Dproc, Expr, Cvar, Dic
		;

@header
{
  // Seams not usefull as the antlr generator adds it
  // package org.getalp.atef;
}

// ***************************
// Composants du langage ATEF:
// ***************************

// Dictionnaires
// *************
// ------------------------------------------------------------------------------	
dicATEF : article+ ;


// Grammaires
// **********

// ------------------------------------------------------------------------------	
// Une grammaire ATEF doit contenir obligatoirement une règle RDICT et 
// une règle MOTINC, à tester avec des prédicats ?
// ------------------------------------------------------------------------------	
gramATEF:  regleATEF+
	  sousGrammaires ?	;


// formats
// *******

// ------------------------------------------------------------------------------	
// fmtATEF:  formats ;

// variables
// *********

// ------------------------------------------------------------------------------	
// declVar:	dv;
	