parser grammar ATEFdictionnaire;

// ----------------------------------------------------------------------------------
// Règle article
// Appelée par la règle 'dicATEF' de AY.g4.
// ----------------------------------------------------------------------------------
article :
	mor=Chaine // morphe
	EG EG
	fm=Litteral		// format morphologique
	PG
	(
		fs=Litteral 	// format syntaxique
 		(					// partie ---',' UL--- optionnelle
			VIRG
			ul1=Chaine
		)?

		PD
		POINT
			// Fin du cas 'morphe' == FTM (FTS [, 'ul'] ')' '.'
		|	// 18/10/2017 (JCD CB) 	
			// Cas 'morphe' == FTM ('*' [, 'ul'] ). FTS '/' complément de variables '.'
			// Exemple: ...==FTP1(* ,'BIS-ZU'). FTSG22 / GNR-E-MASC-U-NEU, NBR-E-...
		MULT 
		(
			VIRG 
			ul2=Chaine
		)?	// 18/10/2017. Cas de FTS=*, fin de la partie optionnelle (article d'affixe).

		PD	
		POINT

		// Partie "complément": FTS[G] '/' complément de variables
    // Par ex: "AEM3S / GNR-E-M-U-N"
		fsext=Litteral
		
		// Mise à jour du champ format S ou G. avec le nom de format de l'extension
		( SLASH | PLUS )	// 20/10/17 (CB) pour syntaxe FTS + CAT(N, A), NBR(PL)...
		
		varVal2 
		(VIRG varVal2)*	// GNR-E-M-U-N, NBR-E-PLU...
		
		POINT
	)
	;


// ----------------------------------------------------------------------------------
// On duplique ci-dessous les règles 'varVal' et 'valeur' définies dans
// la grammaire des formats (Format.g) car sinon ce sont les règles de 
// Format.g qui sont appelées et on a une erreur d'exécution à cause du 
// contexte différent (FmtATEF).
// ----------------------------------------------------------------------------------
// Règle varVal2
// ----------------------------------------------------------------------------------
varVal2:
  v=Litteral			// nom de la variable
	(
	  E
	  valeur2
	  ( UNION valeur2 )*
	|
		PG
  	valeur2
	  ( VIRG valeur2 )*
  	PD
 	)
	;
	
// ----------------------------------------------------------------------------------
// Règle valeur2: valeur élémentaire d'une variable
// ----------------------------------------------------------------------------------
valeur2:
	v=Litteral
	;