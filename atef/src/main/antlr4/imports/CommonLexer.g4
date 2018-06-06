lexer grammar CommonLexer;

// Attention à ne pas modifier les groupes d'éléments ci-dessous: ils forment des
// intervalles utilisés par les itérateurs. 
tokens
{
	TRACOMPL_PROG,

	// Composants:
	DV,
	DPROC,
	CVAR,				
	
   // Noeuds créés lors des définitions de variables:
	Deco,
	Var,
		
	// Noeuds créés lors des définitions de procédures:
	Pcp,
	Pcis,
	Paf,
	Prca,
	
	// et pour les appels de procédures:
	Apcp,
	Apcis,
	Apaf,
	Aprca,
	
	Param,		// paramètre dans les définitions de procédure.
	ParamCible,	// paramètre cible dans les procédures réseau.
//	Sommet,
	
	// Types de variable:
	Type,
//	TypeIndefini,
	TypeChaine,
	TypeExclusif,
	TypeEnsembliste,
	TypeArithmetique,
   TypeGeneral,   // non utilisé pour le moment.

	// Etats d'une variable
	Etat,
	Nouvelle,
	Dollar,
	DollarDollar,
	
	// Valeur d'une variable
	Val,
	ValNul,
	Cardinal,

	// valeurs des différents "codes origine":
	SourceJ1,
	CibleJ2Initial,
	CibleJ2Courant,
	
	REFORM, // noeud identifiant une phase REFORM.
	
	// Affectation
	Affectation,
	
	// Pour une expression booléenne:
	Exbool,
	Oper, // rajouté le 28/08/09 pour le cas variable ou valeur qu'on ne peut 
	// trancher au niveau syntaxique.
	Ou,
	Et,
	Non,
	
	// Opérateurs arithmétiques et ensemblistes: 
	Plus,
	Moins,
	Mult,
	Div,
	Union,
	Intersection,
	Complementaire,
	
	// Pour les réseaux d'affectations conditionnelles (forme 1) IV.1: */
	Resaff,
	Si,
	Alors,
	SinonSi,
	FinSi,
	Sinon,
	
	Aprorca,
	Aprocaf,
	
	// relateurs:
	Eg,
	NonEg,
	Dans,
	NonDans,
	Inclut,
	NInclutPas,
	SupEg,
	Sup,
	Inf,
	InfEg,
	
	// Pour l'analyse des arbres de données linguistiques:
	Foret,
	Arbre,
	Structure,
	Decoratio, // volontairement abrégé car sinon conflit entre la classe 'Decoration'
	// de la bibliothèque et le '#define Decoration 37' généré par ANTLR.
	
	// traitement donc un peu particulier pour ces 2 variables prédéfinies en AG5:
	Forme,
	Ul,
	
	PlusUnaire,
	MoinsUnaire,
	Entier,
		
	// Pour les dictionnaires:
	Dictionnaire,
	Noeud,
	Article,
	Arbor,
	Designateur,
	Affectations,
	
	Formats,
	Format,
	
	// 02/04/2010: ATEF:
	DictionnaireATEF,
	ArticleATEF,
	FormatMATEF,
	FormatSATEF,
	
	// 23/05/2011: grammaire ATEF:
	GrammaireATEF,
	RegleATEF,
	SousRegleATEF,
	PartieGauche,
	PartieDroite,
	Motinc,
	
	Rdict,
	ValDict,
	ControleDict,
	
	ActionsR,
	ConditionR,
	ModifEntreeR,
	ConditionSuivantR,
	SousReglesR,
	
	Varnm,
	Varns,
	Tourn,
	
	Fonction,
	
	// pour SCHAINE et TCHAINE:
	OrigineLongueur,
	Origine, // et pour les conditions, affectations...
	OrigNum, // obsolètes (utilisés dans les ast seulement
	Long,
	
	Schaine,
	Tchaine,
	
	// Pour un début de moniteur:
	// M_ pour moniteur
	M_Ariane_Y,
	M_APPLICATION,
	M_EXECUTION,
	M_PHASE,
	M_ATEF,
//	TRACOMPL, // ne pas utiliser sinon erreur à la compilation.
	M_EXPANS,
	M_ROBRA,
	M_UTILISATEUR,
	M_PHASES,
	M_EXECUTIONS,
	M_CORPUS,
	M_DV,
	M_DV_SOURCE,
	M_DV_XML,
	
	//	PS9,
	
	// Nœud liste dans la représentation d'une liste avec tabArb.
	Liste
}

Blancs:         ( ' ' | '\t' | '\n' | '\r'
                        | '\u00A0' | '\u00C2' 
                        )+ -> skip; //c:{ $channel=HIDDEN; } ;

// Mots-clés et autres unités lexicales:
// *************************************
DECVAR	:	'-DECVAR-'	;
DECO		:	'-DECO-'		;
PROC		:	'-PROC-'		;
PRCA		:	'-PRCA-'		;
MCCVAR	:	'-CVAR-'		;
FIN		:	'-FIN-'		;

// Types des variables:
EXC		:	'-EXC-'		;
NEX		: 	'-NEX-'		;
ARITH		:	'-ARIT-'	;
CHAINE	:	'-CHA-'	;
GEN		: 	'-GEN-'		;	// 26/06/2014: Bloc de variables générales

// Expressions booléennes:
OU		:	'-OU-'	;
ET		:	'-ET-'	;
NO	   :	'-N-'		;  // NON tronque en NO pour des raisons analogues a' 'Decoration' 
                     //ci-dessus

// Opérateurs ensemblistes:
UNION	:	'-U-'		;
INTER	:	'-I-'		;
COMPL	:	'-C-'		;

// Addition notée par "+", et soustraction par "--"
MOINSMOINS	: 	'--'		;
PLUSPLUS		: 	'++'		;

// 06/02/2010: 
//PLUS			: 	'+'		;
PLUS	:	Plus	;
fragment Plus:	'+'	; // utilisé dans la def de Entier. 

// Relateurs
DANS	:	'-DANS-'	;
E		:	'-E-'		;
GE		:	'-GE-'	;
GT		:	'-GT-'	;
INC	:	'-INC-'	;
LE		:	'-LE-'	;
LTH	:	'-LT-'	;
NDANS	:	'-NDANS-';
NE		:	'-NE-'	;
NINC	:	'-NINC-'	;

// Réseaux de conditions et d'affectations:
SI		:	'-SI-'	;
ALORS	:	'-ALORS-';	
SNSI	:	'-SNSI-'	;
SINON	:	'-SINON-';
SNFSI	:	'-SNFSI-';
FSI	:	'-FSI-'	;

// Pour ATEF (<FONC>):
FINAL		: '-FINAL-'	;
ARRET		: '-ARRET-' ;
STOP		: '-STOP-' 	;
TRANS		: '-TRANS-' ;
TRANSA	: '-TRANSA-';
INIT		: '-INIT-' 	;
SOL		: '-SOL-' 	;
ARF		: '-ARF-'	;
ARD		: '-ARD-' 	;
IDN		: '-IDN-' 	;
IDX		: '-IDX-' 	;
ISN		: '-ISN-' 	;
ISX		: '-ISX-' 	;
IFN		: '-IFN-' 	;
IFX		: '-IFX-' 	;
IHN		: '-IHN-' 	;
IHX		: '-IHX-' 	;
IAN		: '-IAN-' 	;
IAX		: '-IAX-' 	;
DST		: '-DST-' 	;
SGRAM		: '-SGRAM-' ;	// 10/6/15
SEC		: '-SEC-'	; 	// 05/08/16

/*		Pour mémoire:
// Autre alternative à ci-dessus qui a l'inconvénient de ne pas associer un entier 
// unique à chaque token (ils ont tous la valeur de FONCSPEC). Voir dans
// ATEFgrammaire.g en particulier la fonction id->getType(id) et l'utilisation de 
// l'étiquette 'id' sur tout le groupe de lexèmes.
	fonc!:
		id=(FINAL|ARRET|STOP|TRANS|TRANSA|INIT|SOL|ARF|ARD|IDN
		|IDX|ISN|ISX|IFN|IFX|IHN|IHX|IAN|IAX|DST)
		{ 	string sid=(char*) id->getText(id)->chars;
			string s=prefNomNdBin+sid+prefNomNdBin;
			creerNoeudBinaire2(id->getType(id), (char*) s.data());
		};

// 
fragment FONC: 	'-FINAL-'|'-ARRET-'|'-STOP-'|'-TRANS-'|'-TRANSA-'|'-INIT-'|'-SOL-'|
						'-ARF-'|'-ARD-'|'-IDN-'|'-IDX-'|'-ISN-'|'-ISX-'|'-IFN-'|'-IFX-'|
						'-IHN-'|'-IHX-'|'-IAN-'|'-IAX-'|'-DST-';
FONCSPEC: FONC;
*/

// à supprimer puisque le langage n'est pas à mots réservés et on
// teste dans les règles ?
//TOURN		: 'TOURN'	;
SCHAINE	: 'SCHAINE'	;
TCHAINE	: 'TCHAINE'	;
ELIM		: 'ELIM'		;
ELIT		: 'ELIT'		;
RDICT		: 'RDICT'	;
MOTINC	: 'MOTINC'	;
MODINC	: 'MODINC'	;

// Voir la règle controleDict dans ATEF_grammaire.g .
//NN	:	'NN'	;
//DU	:	'DU'	;
//NU	:	'NU'	;
//DN	:	'DN'	;

// Ajout le 18/10/2011:
VAREM :  'VAREM'	;
VARES :  'VARES'	;


// ces symboles sont réservés sinon pb ds grammaire ATEF.
VARNM	:	'VARNM'	;
VARNS	:	'VARNS'	;
VARM	:	'VARM'	;
VARS	:	'VARS'	;
VAR	: 	'VAR'		;

// Conflit du mot-clé ci-dessous avec la variable -> suppression le 
// 26/01/2010.
// 01/06/2011: UL et DICT sont des variables particulières d'ATEF cf doc ATEF p 56.
// 21/07/2011: il faut bien ne pas considérer des mots-clés ici; sinon erreur à
// l'exécution incompréhensible: dans le cas d'une déclaration de variables
// dvar* n'est pas exécuté; c'est avec dvar+ qu'on a pu voir le pb causé par UL.


//UL	:		'UL'		;
//DICT	:	'DICT'	;

//UL0	:	'UL0'		;
//DICT0	:	'DICT0'	;

// On ne peut considérer les lexèmes ci-dessous (le langage n'est pas
// à mots-clés; mais on les considère en tant que 'fragment' seulement.
// mais comment exploiter ça ?
/*
// or1:
fragment C: 'C';
fragment S: 'S';
fragment P1: 'P1';
fragment P2: 'P2';
fragment P3: 'P3';
fragment P4: 'P4';

// en plus pour or:
fragment A: 'A';
fragment T: 'T';
fragment R: 'R';

fragment PS1: 'PS1';
fragment PS2: 'PS2';
fragment PS3: 'PS3';
fragment PS4: 'PS4';
fragment PS5: 'PS5';
fragment PS6: 'PS6';
fragment PS7: 'PS7';
fragment PS8: 'PS8';
fragment PS9: 'PS9';
*/
// fin de ce qui spe'cifique a' ATEF:


// remplacé dans ATEF par '--' défini plus haut, sinon pb.
// Moins		: '-'			;

//
// Importance de la position relative des deux règles ci-dessous:
//
// Dans le cas ou' 'Litteral' est avant 'PCP':
// 'warning(208): Lex.g:48:1: The following token définitions are unreachable: PCP'
// évidemment car PCP est vu comme un litteral.
//
// Dans l'autre cas, on n'aura jamais PCP comme identificateur; donc pour résoudre ce 
// conflit, mot-clé ou identificateur selon le contexte, il faut faire autrement (avec 
// un prédicat. 
// 
// Pour les mots-clés 'PCP', 'PCIS', 'PAF'dans les définitions de procédures, on les 
// voit comme des littéraux ayant les bonnes valeurs de chaine à l'aide de prédicats.

fragment Lettre	: 	'a'..'z' | 'A'..'Z'	;
fragment Chiffre	: 	'0'..'9'	;
fragment	QUOTE		:	'\''	;

// on rajoute les codes unicode des caractères 'cent' et 'degre''
fragment AutreSymbole	:	'*' | '.' | '_' | '?' | '!' | '-' | ' ' | '#' | '&'
                          | '\u00A2' | '\u00B0'
								  |'+' | '"' | '/' | '<' | '>' | ',' | ';' | ':' | '£' ;

// Litteral:
Litteral	:	('%' | '_')* ( Lettre | Chiffre )+	; // rajout du souligné pour la balise TRACOMPL_PROG --> obsolète.

COMMENTAIRE_AG5	:   '**' ( ~ '.')* '.' -> skip; //c:{ $channel=HIDDEN; }	;

// Commentaire dans les fichiers XML:
COMMENTAIRE_XML	:   '<!--' ( . )*? '-->' -> skip; //c:{ $channel=HIDDEN; }	;

// Entête XML:
ENTETE_XML	:	'<?' ( . )*? '?>' -> skip; //c:{ $channel=HIDDEN; } ;

DOCTYPE	:	'<!DOCTYPE' ( . )*? '>' -> skip; //c:{ $channel=HIDDEN; } ;

// Autres symboles:
POINT		:	'.'	;
VIRG		:	','	;
PTVIRG	:	';'	;
DPEG		:	':='	;
DXPTS		:	':'	;
EG			:	'='	;

// On a dû changer les deux caractères ci-dessous car sinon erreur ANTLR à la
// lecture du fichier, donc bien faire le changement dans les fichiers à compiler.
AGRAVE	:	'à'	;
CCED		:	'ç'	;

//PGM		:	'(-'	;
//MPD		:	'-)'	;
PG			:	'('	;
PD			:	')'	;

DOLDOL	: '$$'		;
DOL		: '$'		;

// introduit pour traiter les valeurs numériques de variables arithmétiques ds 1 ao.
fragment MOINS	:	'-'	;

// Pour essai:
CG       :  '['   ;
CD       :  ']'   ;

// Opérateurs arithmétiques:
MULT		:	'*'	;
//DIV		:	'/'	;
// Comment faire la différence avec le séparateur dans les triplets de dictionnaires:
SLASH		:	'/'	;

//   Essai avec les chaînes (délimitées par des quotes simples; une quote a' 
// l'intérieur d'une chaîne est double'e, ce qui justifie l'itération, on obtient
// un seul élément lexical du fait qu'on n'a pas de blancs entre les 2 quotes.
Chaine:
//		( QUOTE ( Lettre | Chiffre |  AutreSymbole )* QUOTE )+
// pourquoi pas plus simplement:
		( QUOTE (.)*? QUOTE )+
		;

DBal2	:	'</';
FBal2	:  '/>';
DBal	:	'<';
FBal	:	'>';

fragment			// si on utilise ChaineXML
DQT	:	'"'; 
// Chaine XML, délimitée par des '"' et contenant éventuellement des blancs
/*
ChaineXML:
			DQT ( Lettre | Chiffre |  AutreSymbole )* DQT
		(	DQT ( Lettre | Chiffre |  AutreSymbole )* DQT)*
		;
*/
ChaineXML:
//	DQT .* DQT, changement en ci-dessous car sinon on ne peut analyser 'valeur=""""'
// en effet on double la double quote.
	( DQT (.)*? DQT ) //c:*
	;

// Règle supprimée le 18/03/2010, sinon on n'analyse pas correctement les expressions
// arithmétiques. Pourquoi avait-elle été introduite ?
/*
Entier	:
	(Plus | MOINS)
	Chiffre+
	;
*/

/*
ChDbQuotEchap : 
	{ dtr("ChDbQuotEchap", trgrSYGMOR); }
	
//	DQT ( ~(\\\")* (\\\")? )* DQT
	DQT ( ~(DQT) | '\\'DQT )* DQT

	{ dtr("ChDbQuotEchap", trgrSYGMOR); }
	; // ChDbQuotEchap
*/

// Ajouté le 08/09/2015: lexème supplémentaire pour éviter boucle infinie sur les lexèmes.
AutreSuiteDeCaracteres:
	(	'\u0080'..'\u02AF' 	// supplément latin-1 + latin étendu A et B + extension IPA
//	|	'\u002D'					// tiret supprimé pour que ça marche,
// voir commentaire en début de fichier
	|	'\u0020'					// Blanc
	| '\u005E'					// circonflexe
	)+ -> skip; //c:{ $channel=HIDDEN; };
	//| '\u0020'..'\u002F')+;

