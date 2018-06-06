/*************************************************************************************
 *                                                                                   *
 *   Grammaire des formats après leur prétraitement.                                 *
 *   27/10/2009: JCD.                                                                *
 *                                                                                   *
 *  Ce prétraitement a pour effet de mettre en commentaires Ariane les parties       *
 *  répétées. Voici une entrée avec 2 formats:                                       *
 *                                                                                   *
 *    3      911 14                                                               85 *
 * .--+------+-+--+----------------------------------------------------------------+.*
 * |  FT1    **01.==** bla bla .                                                    |*
 * |**FT1      02.  ** bla .                                                        |*
 * |**FT1      03.  ** et bla .                                                     |*
 * |**FT1      04.     CAT -E- S,                                                   |*
 * |**FT1      05.      SF -E- REG .                                                |*
 * |  FMDVDX **01.==** blabla .   KMS -E- VB, MT -E-                                |* 
 * |**FMDVDX   02.     INF -U- PPA .                                                |*
 * .+--------+-+--+----------------------------------------------------------------+.*
 *                                                                                   *
 *************************************************************************************/
//
// Modifications:
//		22/10/2013: de l'option pour traiter les formats généraux dans la règle varval.
//		17/12/2014: modification de la grammaire des formats Format.g suite au comportement
// incompréhensible d'ANTLR dans l'analyse d'un format: on n'arrive pas à un
// fonctionnement correct de la règle comportant l'alternative entre format généraux
// et formats morphologiques ou syntaxiques.
//		04/09/2016: 			passage à ANTLR4. 
//		23/05/2017: 			ajout de ValeurFtgCtx et ListeValeursFtgCtx
//		30/05/17 (CB JCD):	À FAIRE: nettoyer, en particulier varVal non appelée
//									(mise à la fin avant la partie "obsolète")
// 	28/10/17: 			modif bidon !
//		18/10/17 (CB):			Commentaires (on adaptait ceci pour la compile des dicos)
//		20/10/2017 (CB):		+ syntaxe alternative Var1(val1, val2...), Var2(x, y...).
//		14/12/2017 (JCD): mise de l'initialisation des structures de données dans la
// première règle au lieu de la procédure icommande.
//

parser grammar FormatDeclaration;

options
{
	tokenVocab=FormatsLexer;
}

// ----------------------------------------------------------------------------------
// Règle formats
// Première règle appelée
// ----------------------------------------------------------------------------------
formats:
	(
		(formatG)+
	|
		(formatMS)+
	)	; // formats

// ----------------------------------------------------------------------------------
// Règle formatMS
// Formats morphologiques et syntaxiques
// ----------------------------------------------------------------------------------
formatMS:
	id=Litteral 	
	EGEG
	(
		varValMS
		(VIRG varValMS)*
	)?
	POINT	;

// ----------------------------------------------------------------------------------
// Règle formatG
// Formats généraux
// ----------------------------------------------------------------------------------
formatG:
	id=Litteral
	EG EG
	(
		valeurFtg
		(VIRG valeurFtg)*
	)?
	POINT	;

// ----------------------------------------------------------------------------------
// Règle 
// Variable et liste de valeurs; appelé par formatMS
// ----------------------------------------------------------------------------------
varValMS:
	v=Litteral
	(
  	E
	  valeur=Litteral
	  ( UNION valeur=Litteral )*
	|
	  PG
  	valeur=Litteral
	  ( VIRG valeur=Litteral )*
  	PD
	)	;


// ----------------------------------------------------------------------------------
// Règle valeur, appelée par varValMS
// Valeur et liste de valeurs
// ----------------------------------------------------------------------------------
//valeur:
//	v=Litteral	;


// ----------------------------------------------------------------------------------
// Règle valeurFtg, appelée par formatG
// Liste de formats FTS ou FTG
// ----------------------------------------------------------------------------------
valeurFtg:
	v=Litteral	;
