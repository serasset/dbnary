parser grammar VariableDeclaration;

options
{
	tokenVocab=Lex;
}

dv :
	// partie optionnelle car inexistante en AG5
	(
	DECVAR
	id=Litteral
	POINT
	)?
	
//	(	
		decoration
//	)+
  FIN ? EOF
	;

decoration:
	(
  	DECO				// -DECO-
	  id=Litteral		// Nom de la décoration
	  POINT
	)?
	// suite éventuellement vide de blocs de variables de même type.
	ebloc=bloc* 
	;

/** Bloc des différents types de variables introduits par les mots-clés -EXC-, ...*/     
bloc:
	varType
	// déclarations des variables d'un même bloc:
	(
		vars+=dvar
	)*
;

varType : ( EXC | NEX | ARITH | CHAINE | GEN ) ;

// dvar, appelé par bloc:
// **********************	
dvar:
	eetat=etatVar
	id=Litteral
	(
		// Pour les variables de type chaîne ou arithmétique (en AY), on a
		// ou pas une suite de valeurs initiales nommées
		(
			// EG EG
			DPEG
			PG
				valeurNommee (VIRG valeurNommee)*
			PD
		)?
	|
		// Pour les autres une suite de valeurs
		DPEG
		PG
			val
			(
				VIRG val
			)*
		PD
	)
	POINT;

// État d'une variable
// *******************
etatVar	:
	(
		// vide
	|
		DOL
	|
		DOLDOL	
	);

// Valeur d'une variable
// *********************
val:	id=Litteral ;

// Analyse de la liste de valeurs nommées éventuelle des variables de type chaîne ou
// arithmétique
valeurNommee:
		val 
		EG 
		ch=Chaine
		// Les valeurs nommées sont des chaînes entre simples quotes (doublées si internes)
		// il faut supprimer les quotes externes et une sur deux des internes.
		;